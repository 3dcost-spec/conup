# -*- coding: utf-8 -*-
"""마이노트 비서 0단계 — 네이버/구글 CalDAV 왕복 실측 스크립트.

목적: 네이버 캘린더(caldav.calendar.naver.com)에 CalDAV로 접속해
  1) 캘린더 목록 조회  2) 테스트 일정 생성  3) 읽기 확인  4) 수정  5) sync-token/ETag 확인  6) 삭제
를 한 번에 돌리고, 결과를 JSON 보고서로 남긴다. 사용자 실제 일정은 건드리지 않는다.

사용법 (PowerShell):
  $env:CALDAV_URL="https://caldav.calendar.naver.com"
  $env:CALDAV_USER="네이버ID"
  $env:CALDAV_PASS="2단계인증_앱비밀번호"
  python caldav_probe.py --out probe_result.json
  python caldav_probe.py --list-only        # 조회만, 쓰기 없음
  python caldav_probe.py --keep             # 테스트 일정 삭제 안 함(앱/폰에서 보이는지 확인용)

구글: CALDAV_URL=https://apidata.googleapis.com/caldav/v2 (OAuth 필요, 2단계에서)
네이버웍스: 관리자 CalDAV 설정 후 안내된 URL

의존: pip install caldav icalendar   (caldav 3.x, Python 3.10+)
"""
from __future__ import annotations
import argparse, json, os, sys, time, uuid, traceback
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

KST = ZoneInfo("Asia/Seoul")
TEST_PREFIX = "[마이노트 실측]"


def log(step, ok, **info):
    rec = {"step": step, "ok": ok, "t": datetime.now(KST).isoformat(timespec="seconds"), **info}
    print(("OK  " if ok else "FAIL"), step, {k: v for k, v in info.items() if k != "trace"}, flush=True)
    return rec


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", default=os.environ.get("CALDAV_URL", "https://caldav.calendar.naver.com"))
    ap.add_argument("--user", default=os.environ.get("CALDAV_USER"))
    ap.add_argument("--password", default=os.environ.get("CALDAV_PASS"))
    ap.add_argument("--calendar", default=None, help="대상 캘린더 이름(없으면 첫 번째 쓰기 가능 캘린더)")
    ap.add_argument("--list-only", action="store_true")
    ap.add_argument("--keep", action="store_true", help="테스트 일정을 삭제하지 않음")
    ap.add_argument("--out", default="caldav_probe_result.json")
    a = ap.parse_args()
    if not a.user or not a.password:
        print("CALDAV_USER / CALDAV_PASS 환경변수(또는 --user/--password)가 필요합니다.", file=sys.stderr)
        return 2

    import caldav  # noqa
    from icalendar import Calendar as ICal

    report = {"url": a.url, "user": a.user, "started": datetime.now(KST).isoformat(timespec="seconds"), "steps": []}
    S = report["steps"]
    client = caldav.DAVClient(url=a.url, username=a.user, password=a.password)

    # 1) principal / 캘린더 목록
    try:
        t0 = time.time()
        principal = client.principal()
        cals = principal.calendars()
        names = [(c.get_display_name(), str(c.url)) for c in cals]
        S.append(log("list_calendars", True, ms=int((time.time() - t0) * 1000), calendars=names))
    except Exception as e:
        S.append(log("list_calendars", False, error=repr(e), trace=traceback.format_exc()))
        return finish(report, a.out, 1)
    if not cals:
        S.append(log("pick_calendar", False, error="캘린더가 0개"))
        return finish(report, a.out, 1)

    cal = next((c for c in cals if a.calendar and c.get_display_name() == a.calendar), None) or cals[0]
    S.append(log("pick_calendar", True, name=cal.get_display_name(), url=str(cal.url)))

    # 2) 향후 14일 일정 읽기 (내용은 제목 수·건수만 기록, 본문은 저장하지 않음)
    try:
        t0 = time.time()
        now = datetime.now(KST)
        evs = cal.search(start=now, end=now + timedelta(days=14), event=True, expand=True)
        S.append(log("read_next_14d", True, ms=int((time.time() - t0) * 1000), count=len(evs)))
    except Exception as e:
        S.append(log("read_next_14d", False, error=repr(e), trace=traceback.format_exc()))

    # 3) sync-token 지원 여부 (RFC 6578)
    try:
        t0 = time.time()
        objs = cal.objects_by_sync_token(load_objects=False)
        token = getattr(objs, "sync_token", None)
        S.append(log("sync_token", bool(token), ms=int((time.time() - t0) * 1000), token_present=bool(token)))
    except Exception as e:
        S.append(log("sync_token", False, error=repr(e)))

    if a.list_only:
        return finish(report, a.out, 0)

    # 4) 테스트 일정 생성 → 읽기 → 수정 → 삭제
    uid = f"mynote-probe-{uuid.uuid4()}"
    start = (datetime.now(KST) + timedelta(days=1)).replace(hour=15, minute=0, second=0, microsecond=0)
    end = start + timedelta(hours=1)
    summary = f"{TEST_PREFIX} 캘린더 왕복 테스트"
    ev = None
    try:
        t0 = time.time()
        ev = cal.save_event(uid=uid, dtstart=start, dtend=end, summary=summary,
                            location="서울중앙지방법원", description="마이노트 비서 0단계 자동 생성. 삭제해도 됩니다.")
        S.append(log("create_event", True, ms=int((time.time() - t0) * 1000), uid=uid, url=str(ev.url), start=start.isoformat()))
    except Exception as e:
        S.append(log("create_event", False, error=repr(e), trace=traceback.format_exc()))
        return finish(report, a.out, 1)

    try:
        t0 = time.time()
        got = cal.event_by_uid(uid)
        ical = ICal.from_ical(got.data)
        vev = next(c for c in ical.walk() if c.name == "VEVENT")
        same = str(vev.get("summary")) == summary
        etag = None
        try:
            etag = got.get_property(caldav.elements.dav.GetEtag())
        except Exception:
            pass
        S.append(log("read_back", same, ms=int((time.time() - t0) * 1000), summary=str(vev.get("summary")), etag=str(etag) if etag else None))
    except Exception as e:
        S.append(log("read_back", False, error=repr(e), trace=traceback.format_exc()))

    try:
        t0 = time.time()
        got = cal.event_by_uid(uid)
        got.vobject_instance.vevent.summary.value = summary + " (수정됨)"
        got.save()
        again = cal.event_by_uid(uid)
        ok = "(수정됨)" in again.data
        S.append(log("update_event", ok, ms=int((time.time() - t0) * 1000)))
    except Exception as e:
        S.append(log("update_event", False, error=repr(e), trace=traceback.format_exc()))

    if a.keep:
        S.append(log("delete_event", True, skipped=True, note="--keep: 폰 DAVx⁵/네이버앱에서 보이는지 확인 후 수동 삭제"))
    else:
        try:
            t0 = time.time()
            cal.event_by_uid(uid).delete()
            gone = False
            try:
                cal.event_by_uid(uid)
            except Exception:
                gone = True
            S.append(log("delete_event", gone, ms=int((time.time() - t0) * 1000)))
        except Exception as e:
            S.append(log("delete_event", False, error=repr(e), trace=traceback.format_exc()))

    return finish(report, a.out, 0 if all(s["ok"] for s in S) else 1)


def finish(report, out, code):
    report["finished"] = datetime.now(KST).isoformat(timespec="seconds")
    report["pass"] = all(s["ok"] for s in report["steps"])
    with open(out, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(("\n합격" if report["pass"] else "\n불합격"), "→", out)
    return code


if __name__ == "__main__":
    sys.exit(main())
