# 아키엠 — 특정건축물 신고서·현장조사서 작성 기능 기획·설계서 v1.0

작성일: 2026-10-18 · 상태: 구현 착수용(대표 확정 D-1~D-4 반영)
상위 문서: `docs/planning/archim-special-act-field-survey-plan.md`(v1.0), `docs/planning/archim-special-act-form-set.md`(v1.0)
기준 서식: 「특정건축물 정리에 관한 특별조치법 시행령」 제정안 별지 제1호 **특정건축물 신고서**(3쪽), 별지 제2호 **특정건축물 현장조사서**(3쪽) — 원문·항목표 `docs/reference/law/2026_decree_draft/`

---

## 0. 한 장 요약

| 항목 | 내용 |
|---|---|
| 목표 | 아키엠 안에서 **신고서(별지 1)와 현장조사서(별지 2)를 한 데이터로 동시에 작성·검증·출력(hwpx/pdf)·서명**한다 |
| 작성 흐름 | 현장 등록(유형: 특정건축물 조사) → 공부 입력 → 동·층 구조 → 층별 위반 입력 → 분야별 조사 체크 → 사진 → AI 초안 → PC 검토·보정 → 검증 통과 → 건축사 서명 → 출력·패키지 |
| 원칙 | 서식의 모든 칸은 **데이터 모델의 필드 1개와 1:1**로 연결된다. 사람이 두 번 치는 칸이 없다. 서명·날짜 칸은 자동 생성 시 비운다. **판단 칸은 앱이 묻고 선택지 카드를 주며 건축사가 현장에서 고른다. 기록은 전부 음성으로 가능하다**(§2-3·§2-4) |
| 유형 표기 | 등록 화면에서 감리 3종과 **구분된 별도 항목 「양성화 현장조사」**, 현장 목록 「양성화」 탭 |
| 범위(MVP) | 별지 1·2 전 필드 입력·계산·검증·hwpx/pdf 생성·서명·ZIP. 설계도서(도면)는 면적표·실측 export까지 |
| 비범위 | 도면 작도, 지자체 전자접수 연동, 과태료 산식 확정(파라미터만) |

---

## 1. 사용자 시나리오

### 1-1. 건축사 사무소(주 사용자)
1. PC에서 「특정건축물 조사」 유형으로 현장 생성, 건축주 초대 링크 발송(D-3 비공개).
2. 건축주가 링크로 대장·등기·토지이용계획 사진과 완공 증빙 업로드.
3. 조사 보조원이 모바일로 현장 방문: 동·층별 위반부 사진(AI 위반유형 제안 → 확정), 실측(폭·깊이·높이 3종), 분야별 체크, 음성 소견.
4. 앱이 층별 위반현황표·건축물 현황(허가 당시 vs 현재)을 계산하고 **신고서 2쪽·조사서 2쪽을 동시에 채운다**.
5. 건축사가 PC에서 조사서 1쪽 지장 유무·종합의견(AI 초안)을 확정하고 검증 통과 후 서명.
6. hwpx·pdf 출력, 신고 패키지 ZIP, 접수 후 접수번호 입력.

### 1-2. 건축주(초대 링크)
- 신고인 정보·소유 구분·완공 증빙 입력, 진행 조회, 신고인 서명은 **출력물에 수기** 또는 PC 전자서명(선택).

---

## 2. 화면 설계

### 2-1. 모바일(현장) — 기존 화면 변형

| 화면 | 기존 아키엠 | 변경 |
|---|---|---|
| M0 현장 목록 | 02 | 상단 분류 탭 **전체 / 건축 / 해체·석면해체 / 양성화** — 「양성화」 탭 추가(리스트·카드·지도 보기 동일) |
| M1 현장 등록 | 02·03 | 「현장 등록」 유형 선택 화면을 **두 그룹**으로 구성: ① 감리 그룹 = 공사감리 / 해체감리 / 석면해체감리(기존), ② 구분선 아래 **별도 항목 「양성화 현장조사」**(부제: 특정건축물 정리법 신고서·현장조사서). 감리의 하위 유형이 아닌 독립 카테고리이며 내부 코드 `SPECIAL_ACT_SURVEY`, 프로젝트 유형 아이콘·색상도 별도. 선택 시 공간 구조 = 동(주건축물 제1동 …) × 층(지하 n~지상 n, 옥탑), 공간 체크 항목을 조사용으로 교체, 감리일지 서식·직책 선택은 표시하지 않음(대표 지시 2026-10-18) |
| M2 공부 등록 | 04 AI 등록 | 「대장 찍기」「토지이용계획 찍기」「등기 찍기」 3버튼. OCR 결과를 **대장 현황 카드**로 표시, 확인 후 저장(BuildingRecord·Site) |
| M3 조사 메인 | 05 달력 | **단계 카드**: 공부 → 동·층 → 위반 입력 → 조사 체크 → 사진 → 초안. 각 카드에 완료율 |
| M4 위치·분야 선택 | 06 | 동 → 층 → 분야(대지/외관규모/층별내부/구조/피난방화/설비/일조인접) |
| M5 사진 | 07 | 「사진 찍자」 음성 촬영 → 메모 받아쓰기 → **판단 카드(C-1 위반유형·C-2 허가근거·C-3 산정·C-10 배치)**. 사진보드에 동·층·위반유형·실측치. 마크업 줄자로 치수 |
| M6 위반 입력(층별) | 신규 | 층 선택 → 행 추가: permit_basis(무허가/허가부분/허가외), 위반유형 코드, 바닥면적(㎡), 위반시점(연도), 위반내용, 세대수. 사진 연결 |
| M7 이격 입력 | 신규 | 건축선 침범·건축선 이격·인접대지 이격: 거리 m, 위반면적 ㎡, 사진 |
| M8 조사 체크 | 10 체크리스트 | 분야별 항목을 음성으로 체크 → 분야 종료 시 **판단 카드(C-5 지장 유무·C-6 대지 서술·C-7 주차)** 로 조사서 1쪽 칸을 현장에서 확정 |
| M9 실측 | 신규(간단) | 최고·처마·반자 높이, 전면도로폭, 접도 길이(3m 이상 도로), 세대 전용면적 |
| M10 AI 초안 | 11 생성형 일지 | 「조사서 초안 만들어」 → **C-8 위반내용 문장·C-9 종합의견 선택지 카드**(각 2~4안), 「AI 제안」 배지, 고른 뒤에도 수정 가능 |
| M11 미리보기 | 12 | 신고서·조사서 PDF 미리보기(서명 전 "미서명" 워터마크) |

### 2-2. PC(문서) — 기존 20·22 확장

| 화면 | 설명 |
|---|---|
| P1 문서 드롭다운 | 「특정건축물 신고서」「특정건축물 현장조사서」「위반현황표」「신고 체크리스트」「템플릿 라이브러리」 |
| P2 신고서 편집 | 별지 1 레이아웃 그대로의 **폼 뷰**(3쪽 탭). 자동 채움 칸은 회색, 수동 칸은 흰색, 오류 칸은 빨간 테두리. 동별 개요는 동 탭으로 반복 |
| P3 현장조사서 편집 | 별지 2 레이아웃 폼 뷰(3쪽 탭). 지장 유무는 토글+내용, 위반사항 7항목은 자동 체크(수동 override 가능, 로그) |
| P4 검증 패널 | 우측 고정. 오류/경고 목록, 클릭 시 해당 칸 이동. 통과 전 서명 버튼 비활성 |
| P5 서명·확정 | 현장조사자(건축사) 전자서명(24장 등록 서명). 서명 시 문서 상태 SIGNED, 이후 수정은 새 버전 |
| P6 출력 | hwpx / pdf / docx(보조) 다운로드, 신고 패키지 ZIP(23장 경로) |
| P7 템플릿 라이브러리 | 법정 원본(2026_draft) 빈 서식 다운로드, 아키엠 표준·사무소 커스텀 관리(form-set §J) |
| P8 신고 진행 | 접수번호·접수일·보완·심의·결과·조건부 승인 이행기한 |

### 2-3. 현장 판단 카드 — "묻고, 선택지를 주고, 건축사가 고른다"(대표 지시 2026-10-18)

서식의 판단 칸(지장 유무, 위반유형, 위반내용 문장, 산정 프로파일, 종합의견 등)은 앱이 **결론을 쓰지 않는다**. 앱은 질문을 던지고 **초안 선택지 2~4개를 카드**로 제시하며, 건축사(또는 보조원)가 현장에서 하나를 고르거나 "고칠게요"로 수정한다. 모든 카드는 「AI 제안」 배지와 근거(사진·실측·대장 값)를 함께 표시하고, 선택 결과와 선택지 전체를 로그에 남긴다.

| 카드 | 질문(음성·화면) | 선택지 예시 | 선택 → 데이터 |
|---|---|---|---|
| C-1 위반유형 | "이 부분은 무엇으로 볼까요?" | ① V-A 증축(옥탑증축) ② V-C 용도변경(창고→주거) ③ V-B 대수선 ④ 직접 입력 | Violation.type_code/tag |
| C-2 허가 근거 | "허가·신고된 부분인가요?" | ① 무허가 ② 허가·신고 부분(위치변경) ③ 허가·신고 외 | Violation.permit_basis |
| C-3 산정 프로파일 | "면적을 어떻게 셀까요?" | ① 옥탑: 바닥면적 전부 산입·층수 불변(1/8 이하) ② 옥탑: +1층(1/8 초과) ③ 발코니 거실화: 전부 산입 ④ 노대 유지: 1.5m 공제 | Violation.calc_profile → Calculation |
| C-4 위반시점 | "언제 완공됐나요?" | ① 항공사진 2019 ② 재산세 2017 ③ 진술 2020 ④ 직접 입력 | Violation.year(근거 자료 링크) |
| C-5 지장 유무(5종) | "구조안전에 지장이 있나요?" | ① 지장 없음 — "증축부 경량철골, 균열·처짐 없음" ② 지장 있음 — "접합부 부식 확인" ③ 확인 필요 — "기초 미확인, 구조기술사 검토" | SurveyChecks.bldg.*{has_issue, note} |
| C-6 대지 서술(5종) | "도로와의 관계는?" | ① "너비 4m 도로에 6.2m 접함" ② "너비 3m 도로, 접도 요건 미달" ③ 직접 말하기 | SurveyChecks.site.* |
| C-7 주차 | "주차장 추가 설치 의무가 있나요?" | ① 비대상(세대 증가 없음, 법 7조1항) ② 검토 대상 — 필요 2대 ③ 비대상(전세사기피해주택, 2항) | SurveyChecks.parking |
| C-8 위반내용 문장 | "서식에 쓸 문장을 고르세요" | ① "옥상 경량철골 증축, 주거 사용" ② "옥상 판넬 구조물 증축 약 18㎡, 임대 세대" ③ 직접 말하기 | Violation.note |
| C-9 종합의견 | "종합의견 초안 3개 중 고르세요" | ① 적합 의견형 ② 보완 전제형(구조확인서 첨부 조건) ③ 쟁점 명시형(건폐율 초과 소명) | Opinion.final_text(수정 가능) |
| C-10 사진 배치 | "이 사진을 어디에 붙일까요?" | ① 조사서 3쪽 대표 사진 ② 사진대지 ③ 완공 입증 자료 | Photo.is_representative/scope |

- 카드 노출 시점: 사진 촬영 직후(C-1·C-2·C-10), 실측 입력 직후(C-3), 분야 체크 종료 시(C-5·C-6·C-7), 초안 생성 시(C-8·C-9). 현장에서 바로 결정하도록 **모바일 M5~M9에서 카드가 뜬다.** PC P2·P3에서도 같은 카드를 다시 열어 바꿀 수 있다.
- 선택지는 AI-1~AI-4가 생성하되, 법령 파라미터·계산 결과와 모순되는 선택지는 비활성(회색)으로 보여 이유를 적는다. "직접 입력/직접 말하기"는 모든 카드에 항상 존재한다.
- 카드 없이 자동 확정되는 칸은 **순수 계산 칸**(합계·건폐율·용적률·자동 체크)뿐이며, 그것도 override 가능.

### 2-4. 음성 입력 전면 적용 — "기록은 전부 말로"(대표 지시 2026-10-18)

모든 기록 행위에 음성 메모가 붙는다. 사진 촬영·위반 행 추가·체크·실측·서술·카드 선택을 전부 음성으로 수행할 수 있고, 손은 서명과 마크업 끌기에만 쓴다. 엔진은 막찍는 현장조사앱의 **온디바이스 전사(보이스태그 모듈)** 를 공통 호출하고, 통신 불량(반지하·옥탑)에서도 동작한다. 발화 → 파싱 → **확인 카드(§2-3)** → 저장 순서를 지키며, 확인 없는 쓰기는 없다.

| 기록 행위 | 음성 예시 | 파싱 결과 | 확인 카드 |
|---|---|---|---|
| 문맥 설정 | "1동 옥탑 외관" | dong·floor·분야 | 상황 띠 갱신(카드 없음) |
| 사진 촬영 + 메모 | "사진 찍자" → 띵 → "옥상 경량철골 증축, 폭 4.2 깊이 3.6, 주거로 쓰고 있음" | Photo + Measurement(4.2×3.6) + Violation 초안(V-A, 옥탑증축, 15.12㎡) + note | C-1·C-2·C-3 연속 카드 |
| 위반 행 추가 | "2층 발코니 확장, 거실로 막음, 2018년" | Violation(V-A, 발코니거실화, year 2018) | C-1·C-3·C-4 |
| 이격 | "건축선에서 0.8m, 위반 3.2제곱" | Setback.bldg_line{0.8, 3.2, flag} | 확인 카드 |
| 실측 | "최고 11.2 처마 10.4 반자 2.3" | Dong.height 3종 | 확인 카드(순서 역전 시 W-HEIGHT) |
| 체크 | "구조 체크, 균열 있음, 나머지 양호" | 구조 분야 항목 YES/NO | C-5 지장 유무 카드 |
| 대지 서술 | "도로 4미터, 접도 6.2미터, 상하수도 연결됨" | site.road_relation·frontage·water·sewage | C-6 |
| 세대 | "3층 두 세대, 전용 42와 38" | Unit[] | 확인 카드(85㎡ 초과 시 W-UNIT) |
| 이행강제금 | "이행강제금 두 번, 삼백만 원, 다 냈음" | Penalty{2, 3,000,000, 3,000,000} | 확인 카드 |
| 소견 | "소견: 증축부 기초 미확인, 구조기술사 검토 필요" | Opinion 메모 → AI-3 입력 | C-9 |
| 카드 선택 | "두 번째" / "맞아요" / "고칠게요, 3.4로" | 선택·수정 | — |
| 조회·이동 | "조사서 초안 만들어", "미리보기" | 화면 이동·생성 | — |

- 숫자 단위 파서: "제곱/평방미터/㎡", "미터/m", "센치"(→ m 환산), 연도 2자리·4자리, 금액 "만 원·백만 원".
- 음성 메모 원문(audio + transcript)은 해당 Violation·Photo·Check에 **증거로 보존**되고, 조사서에는 쓰이지 않는다(요약 문장만 카드로 선택).
- 말로 안 되는 것: 건축사 서명, 외부 발송, 마크업 끌기, 사진 앨범 선택. 적합·부적합 판정도 말로 "확정"하지 않는다 — 카드 선택이 곧 건축사 판단이며 서명 시점에 효력.
- 모바일 M5~M9 화면 전부에 하단 오브(음성 버튼)와 "지금 할 수 있는 말" 힌트를 둔다(음성 혁신안 §4 레지스트리에 survey.* 행 추가).

### 2-5. 폼 뷰 공통 규칙
- 칸 상태 4종: `auto`(계산·OCR, 편집 시 override 플래그), `manual`, `signature`(항상 비움), `agency`(음영 칸: 신고번호·접수일자·처리일자 — 입력 불가, 접수 후 P8에서만)
- 숫자: 면적 소수 2자리, 거리 소수 2자리, 비율 소수 2자리, 단위 자동 표기(㎡, m, %)
- 체크박스: `[√]`/`[  ]` 렌더, 조사서 `□`는 `■`/`□`

---

## 3. 데이터 모델(상세)

```
Project(type=SPECIAL_ACT_SURVEY)
 ├ Applicant        name, id_no(생년월일|사업자|법인번호), address, phone, is_corp
 ├ Architect        name, license_no(자격번호), office_name, office_reg_no(사무소 신고번호), office_address, phone, signature_id
 ├ Site             address, pnu, area_m2, jimok(지목), zone(지역), district(지구), area_type(구역),
 │                  road_width_m(전면도로폭), frontage_m(도로에 접하는 대지 길이), frontage_3m_road_m(3m 이상 도로 접도 길이),
 │                  ownership{자가|타인_사용승락|국공유지}, exclusion_flags[]
 ├ BuildingRecord   main_use, sub_use, start_date(착공일), completion_date(완공일), permit_exists(bool),
 │                  at_permit{bldg_area, gross_area, far, bcr, main_use, units, parking}   # 허가(신고) 당시
 ├ Dong[]           name, floors_above, floors_below, has_rooftop,
 │                  structure{roof, wall, floor}, height{max_m, eave_m, ceiling_m}
 │   ├ Floor[]      label(지하1|1|…|옥탑), legal_area_m2(①적법 바닥면적), permitted_area_m2(허가·신고 부분)
 │   │   └ Violation[] type_code(V-A..V-Z), tag, permit_basis{무허가|허가부분|허가외}, area_m2, year(위반시점), units, note, photo_ids[]
 │   └ Setback      bldg_line{dist_m, area_m2, flag}, adj_boundary{dist_m, area_m2, flag}, bldg_line_intrusion{flag, area_m2}
 ├ Unit[]           dong, floor, unit_no, excl_area_m2, scope{existing|filed}       # 다세대, 공용면적 common_area_m2는 Dong에
 ├ Penalty[]        imposed_count, imposed_amount, paid_amount (층별 또는 동별), source(고지서|대장)
 ├ SurveyChecks     site{safety, road_relation, water_supply, sewage}(서술),
 │                  bldg{structural, fire, hygiene, evacuation, planning}: {has_issue(bool|null), note}
 │                  parking{exempt_basis{법7조1항|법7조2항|null}, review{needed_spaces, exempt}, method{설치|비용납부|해당없음}}
 ├ NationalProject  is_applicable, name, permit_no, permit_date
 ├ Opinion          ai_draft, final_text, status
 ├ Photo[]          (기존) + dong, floor, violation_id, caption, area_m2
 ├ Calculation      per dong/total: bldg_area, gross_area, far, bcr, units, parking, use_areas{use: m2}, totals per floor
 ├ Document[]       kind{FORM1|FORM2|VIOLATION_TABLE|CHECKLIST}, template_id, template_version, data_snapshot(json),
 │                  status{DRAFT|REVIEW|SIGNED|FILED}, signed_by, signed_at, file_hwpx, file_pdf, version
 └ Filing           receipt_no, received_at, processed_at, stage, supplements[], review_date, result, conditional{flag, deadline}
```

필드 타입·필수 여부·서식 칸 대응은 §4 사양표가 정본.

---

## 4. 필드 사양 — 서식 칸 ↔ 데이터 ↔ 입력 방식

표기: 입력 `A`=자동(계산·OCR), `M`=수동, `S`=서명(비움), `G`=기관(음영). 필수 `●`.

### 4-1. 별지 제1호 신고서 1쪽

| # | 서식 칸 | 데이터 | 입력 | 필수 | 검증 |
|---|---|---|---|---|---|
| 1-01 | 신고번호·접수일자·처리일자 | Filing.* | G | | 접수 후만 |
| 1-02 | 위반구분 체크 8종 | Violation.type_code/tag 집계 → 신축·증축·개축·재축·이전·대수선·용도변경·기타 | A(override M) | ● | 최소 1개 |
| 1-03 | 허가·신고 여부 | BuildingRecord.permit_exists | M | ● | permit_exists=true면 at_permit 필수 |
| 1-04 | 신고인 성명(법인명) | Applicant.name | M | ● | |
| 1-05 | 생년월일(사업자·법인번호) | Applicant.id_no | M | ● | 형식 |
| 1-06 | 주소·전화 | Applicant.address/phone | M | ● | |
| 1-07 | 현장조사자 성명·서명 | Architect.name / signature | M / S | ● | 서명은 SIGNED 시만 삽입 |
| 1-08 | 자격번호 | Architect.license_no | M(프로필) | ● | |
| 1-09 | 사무소명·신고번호·주소·전화 | Architect.office_* | M(프로필) | ● | |
| 1-10 | 주용도·부속용도 | BuildingRecord.main_use/sub_use | A(OCR)→M | ● | 건축법 시행령 별표1 코드표 선택 |
| 1-11 | 착공일·완공일 | BuildingRecord.start_date/completion_date | M | 완공일 ● | 완공일 ≤ 2023-12-31 아니면 경고(적격성) |
| 1-12 | 대지 위치·면적·지목 | Site.address/area_m2/jimok | A(OCR) | ● | |
| 1-13 | 전면도로폭 m | Site.road_width_m | M(실측) | ● | >0 |
| 1-14 | 지역 / 지구·구역 | Site.zone/district/area_type | A(토지이용계획 OCR) | ● | 제외구역 키워드 → 경고 |
| 1-15 | 도로에 접하는 대지의 길이 | Site.frontage_m | M(실측) | ● | **단위 m 확정**(2026-10-18). 입법예고 원문의 "㎡"는 오기로 보고 아키엠 표준 템플릿은 m로 인쇄, 법정 원본 템플릿은 원문 그대로 보존 |
| 1-16 | 소유구분 3택 | Site.ownership | M | ● | 타인소유면 첨부 C-6(사용승낙) 체크리스트 필수 |
| 1-17 | 건축물 현황 — 허가(신고) 당시 7칸 | BuildingRecord.at_permit.* | A(OCR 대장)→M | permit_exists=true면 ● | |
| 1-18 | 건축물 현황 — 현재(신청일) 7칸 | Calculation.total.* | A | ● | §5 계산 |
| 1-19 | 년월일 | Document.signed_at | S | | |
| 1-20 | 신고인 서명 / 현장조사자 서명 | S | S | | 비움 |

### 4-2. 별지 제1호 신고서 2쪽 — 동별 개요(동마다 1쪽)

| # | 서식 칸 | 데이터 | 입력 | 필수 | 검증 |
|---|---|---|---|---|---|
| 2-01 | 동명칭 | Dong.name | M | ● | |
| 2-02 | 층수 지상/지하 | Dong.floors_above/below | A(Floor 수) | ● | Floor 라벨과 일치 |
| 2-03 | 구조 지붕·벽체·바닥 | Dong.structure.* | M(선택지) | ● | |
| 2-04 | 최고·처마·반자 높이 | Dong.height.* | M(실측) | ● | max ≥ eave ≥ ceiling 경고 |
| 2-05 | 층별 행: 층 | Floor.label | A | ● | |
| 2-06 | ①기존건축물 바닥면적 | Floor.legal_area_m2 | A(OCR)→M | ● | |
| 2-07 | ③허가·신고 위반면적 | Σ Violation(permit_basis=허가부분).area | A | | |
| 2-08 | ④허가·신고 외 위반면적 | Σ Violation(permit_basis∈{무허가,허가외}).area | A | | |
| 2-09 | ⑤위반유형 | Violation.type_code 라벨(증축,개축,대수선,용도변경 등) 결합 | A | | |
| 2-10 | ⑥위반시점 | min(Violation.year) 또는 층별 대표 | A(override) | ● | ≤ 2023 |
| 2-11 | ⑦세대수(가구·호) | Σ Violation.units 또는 Unit count | A | | |
| 2-12 | ②이행강제금 부과횟수·부과금액·납부금액 | Penalty.* | M | | 납부 ≤ 부과 |
| 2-13 | 다세대 호/세대별 면적 — 기존/신고 공용 전체면적 | Dong.common_area_m2{existing,filed} | M | 다세대만 ● | |
| 2-14 | 호(세대) 구분·층·전용면적 | Unit[] | M | 다세대만 ● | 각 ≤ 85㎡ 아니면 적격성 경고 |
| 2-15 | 건축선 이격 위반 m·㎡ | Setback.bldg_line | A(M7) | | |
| 2-16 | 인접대지경계선 이격 위반 m·㎡ | Setback.adj_boundary | A(M7) | | |

행 수: 서식 8행. 층이 8개를 넘으면 **2쪽을 복제**(동별 개요 계속).

### 4-3. 별지 제1호 신고서 3쪽
첨부서류·수수료·작성방법·처리절차는 **고정 텍스트**. 첨부 체크(설계도서·현장조사서·대지 권리 증명)는 신고 체크리스트(D-7)와 연동해 누락 시 출력 경고.

### 4-4. 별지 제2호 현장조사서 1쪽

| # | 서식 칸 | 데이터 | 입력 | 필수 | 검증 |
|---|---|---|---|---|---|
| S1-01 | ①국가시책 사업명(허가번호)·허가일 | NationalProject | M | 해당 시 ● | 해당 시 "지자체 작성 조사서로 갈음 가능" 안내 |
| S1-02 | 신고인 성명·등록번호 | Applicant | A(공유) | ● | |
| S1-03 | 건축물 개요 위치·용도·구조 | Site.address, BuildingRecord.main_use, Dong[0].structure 요약 | A | ● | |
| S1-04 | 대지의 안전 | SurveyChecks.site.safety | M(서술, AI 초안) | ● | |
| S1-05 | 도로와의 관계 | SurveyChecks.site.road_relation (+ road_width_m 자동 삽입) | M/A | ● | |
| S1-06 | 3미터 이상 도로에 접하는 길이 | Site.frontage_3m_road_m | M(실측) | ● | |
| S1-07 | 상수도·하수도 시설 현황 | SurveyChecks.site.water_supply/sewage | M | ● | |
| S1-08 | ②구조안전 | SurveyChecks.bldg.structural{has_issue,note} | M(체크 결과 → AI 초안) | ● | 구조 체크 점수와 모순 시 경고 |
| S1-09 | 소방상 지장 | bldg.fire | M | ● | 영 제7조 특례 해당 시 note 자동 문구 |
| S1-10 | 위생상 지장 | bldg.hygiene | M | ● | |
| S1-11 | 피난상 지장 | bldg.evacuation | M | ● | |
| S1-12 | 도시계획상 지장 | bldg.planning | M | ● | 제외구역 플래그 있으면 경고 |
| S1-13 | 주차장 — 추가 설치 의무 비대상 | parking.exempt_basis | M | ● | 법 제7조1항/2항 택1 또는 null |
| S1-14 | 주차장 — 검토 대상(추가 필요 구획 수, 면제) | parking.review | M | 비대상 아니면 ● | |
| S1-15 | 확보방법 설치/비용납부/해당없음 | parking.method | M | ● | |
| S1-16 | 위반사항 7항목 해당/해당없음 | Violation·Setback 집계 | A(override M, 로그) | ● | §5-4 매핑 |
| S1-17 | 종합의견 | Opinion.final_text | AI 초안 → M 확정 | ● | 확정 전 출력 시 "초안" 워터마크 |
| S1-18 | 년월일·현장조사자 서명 | S | S | | |

### 4-5. 별지 제2호 현장조사서 2쪽 — 동별 세부현황(층 열 5)

| # | 행 | 데이터 | 입력 |
|---|---|---|---|
| S2-01 | 적법부분 바닥면적 | Floor.legal_area_m2 | A |
| S2-02 | 허가·신고 없이 위반 — 건축: 바닥면적·위반시점·위반내용 | Violation(permit_basis=무허가, type∈{V-A,V-D}) 층별 합·연도·note 결합 | A |
| S2-03 | — 대수선: 위반시점·위반내용 | Violation(무허가, V-B) | A |
| S2-04 | — 용도변경: 바닥면적·시점·내용 | Violation(무허가, V-C) | A |
| S2-05 | — 그 밖: 바닥면적·시점·내용 | Violation(무허가, V-E~V-Z) | A |
| S2-06 | — 합계 | Σ | A |
| S2-07 | 허가·신고했으나 사용승인 미취득 — 허가·신고한 부분 바닥면적 | Floor.permitted_area_m2 | A(OCR)→M |
| S2-08 | — ②허가·신고 부분 위반(바닥면적·시점·내용) | Violation(permit_basis=허가부분) | A |
| S2-09 | — ③허가·신고 외의 부분 | Violation(permit_basis=허가외) | A |
| S2-10 | — 그 밖 / 합계 | | A |
| S2-11 | 합계(전체) | | A |
| S2-12 | 용도 및 용도별 면적(열 5 + 합계) | Calculation.use_areas | A |
| S2-13 | 층수·구조·높이·이격 위반 | 신고서 2쪽 2-02~2-04, 2-15~2-16 **동일 데이터** | A |

열 수: 층 5열 고정. 6층 이상 또는 지하 포함 초과 시 **2쪽을 복제**해 층 열을 이어 붙인다.

### 4-6. 별지 제2호 현장조사서 3쪽 — 현장사진 (+ 사진대지 첨부)  ★ MVP 1순위
- **서식 3쪽(현장사진)**: 블록당 위치(동·층)·위반면적(㎡)·위반내용·사진 1장, 쪽당 2블록. Violation별 대표 사진을 자동 배치하고 사진 수만큼 3쪽을 복제한다. 위치·면적·내용은 Violation 행에서 자동 채움.
- **사진대지(별도 첨부)**: 서식 3쪽에 들어가지 않은 나머지 사진 전부를 아키엠 사진대지(08·14장, 2·4·6·8장형)로 조판해 **조사서 뒤에 이어 붙인다**(대표 확정 2026-10-18). 사진보드에 동·층·위반유형·실측치·촬영일시 표기, 사진번호는 서식 3쪽과 연속 번호.
- 출력 단위: 「현장조사서 hwpx」 1개 파일 = 1쪽 조사사항 + 2쪽 동별 세부현황(쪽 복제) + 3쪽 현장사진(쪽 복제) + 사진대지(쪽 복제). pdf도 동일 순서 1파일. 신고 패키지 ZIP에는 사진대지를 별도 pdf로도 중복 수록.
- 사진 규격: 장변 1600px 리사이즈, JPEG 품질 85, EXIF 촬영일시 유지, hwpx 용량 상한 50MB(초과 시 사진대지를 2권으로 분할).

### 4-7. 사진 요건 — 법규가 요구하는 것 vs 아키엠 표준(2026-10-18 조사)

**법규가 명시한 사진 요건은 별지 제2호 3쪽 「현장사진」 하나다.** 시행령안 본문·법률에는 사진 규격(장수·방향·해상도·촬영일 표기)이 없고, 2014년 국토교통부 「특정건축물 정리에 관한 특별조치법 시행지침」(행정규칙, CODIL 등록)은 원문 미확보 상태다. 지자체 2014 안내문에도 사진 요건은 없었다. 따라서 **법정 필수 = 3쪽 블록(위치·위반면적·위반내용·사진)** 이고, 그 외는 심의 통과를 위한 실무 표준이다.

| 구분 | 요건 | 근거 | 아키엠 처리 |
|---|---|---|---|
| 법정 필수 | 위반부별 사진 1장 이상 + 위치(동·층)·위반면적·위반내용 | 시행령안 별지 2 3쪽 | Violation별 대표 사진 필수(E-REQ), 자동 조판 |
| 법정 필수(간접) | 완공시점(2023.12.31 이전) 입증 — 항공사진·과세·공과금 등 | 법 제3조·실무 | 완공 입증 목록(C-1~C-3)에 항공사진 연도 캡처 첨부, 서식 아님 |
| 실무 표준(아키엠 기본 세트) | ① 건물 전경 4면(정면·좌·우·후면) ② 위반부 원경·근경 ③ 위반부 실측(줄자·레이저 수치 포함) ④ 내부 용도 확인(거실화·세대분할) ⑤ 피난·소방 설비 ⑥ 도로·접도 현황 ⑦ 옥탑·발코니는 지붕선·외벽선 보이는 각도 | 정기점검 매뉴얼(적합·부적합 현황사진), 공공시설 실태조사 지침(원경·정면·측면 3장), 심의 실무 | 조사 분야별 **필수 촬영 체크리스트**로 안내, 누락 시 W-PHOTO 경고(차단 아님) |
| 메타 표기 | 촬영일시·위치(동·층)·위반유형·실측치·촬영자 | 사진보드(07장) | EXIF 유지 + 사진보드 ON 기본, 사진번호 연속 |
| 규격 | 장변 1600px, JPEG 85, 1장 ≤ 2MB, 원본은 별도 보관 | 아키엠 내부 기준 | 서식 삽입용 리사이즈, 원본은 패키지 ZIP `photos_original/` |
| 항공사진 비교 | 기준일 전후 항공사진 캡처 + 현재 사진 대비(위반부 표시) | 심의 실무 | 사진대지 "비교형" 레이아웃(좌: 항공, 우: 현장) 옵션 |

- 검증 코드 추가: **W-PHOTO**(경고) 분야별 필수 촬영 세트 누락, **E-PHOTO-VIO**(오류) Violation에 대표 사진 없음.
- 후속: 2014 시행지침 원문(CODIL nserialno=810) 확보 시 사진 관련 조항이 있으면 이 표를 갱신. 지자체(강북구 등) 공고에 사진 요건이 있으면 JurisdictionParams로 흡수.

---

## 5. 계산·판정 규칙

### 5-1. 면적
- 층별 현재 바닥면적 = legal_area + Σ violation.area(해당 층, type∈{V-A, V-D, V-C는 면적 중복 주의: 용도변경은 기존 면적 내이므로 **합산 제외**, 별도 열})
- 연면적(현재) = Σ 층별 현재 바닥면적(지하 포함, 옥탑은 건축법 산정 규칙 플래그로 포함/제외 선택)
- 건축면적(현재) = max 층 수평투영(기본 1층) + 수평 증축분(사용자 입력 또는 1층 위반면적 가산 옵션)
- 건폐율 = 건축면적 / Site.area_m2 × 100, 용적률 = 지상 연면적 / Site.area_m2 × 100
- 용도별 면적 = Σ (Floor/Violation.use → 면적), 용도 코드는 건축법 시행령 별표1

### 5-1a. 위반유형별 면적·층수 산정 규칙(조사 메모 `docs/reference/law/research_area_parking_rules.md` §1-4)
- **옥탑(거실)**: 바닥면적·연면적 전부 산입(계단탑 예외 아님). 건축면적은 하부 외벽선 안이면 증가 0. 수평투영 합계(기존 승강기탑·계단탑 포함) > 건축면적 1/8이면 층수 +1.
- **발코니 확장**: 외벽 밖 돌출분은 건축면적 가산. 거실화(창호·벽 폐쇄)면 바닥면적 전부 산입, 노대 유지면 (면적 − 긴 변×1.5m) 산입. 기존 노대 실내화는 공제분이 산입으로 전환.
- **필로티 전용**: 제외분 전부 산입, 주차대수 감소 → §5-6 주차 검토. **지하 거실화**: 바닥면적 산입, 용적률 연면적·층수 제외. **다락**: 층고 1.5m(경사 1.8m) 초과 시 산입.
- 각 Violation 행에 `calc_profile`(옥탑/발코니돌출/발코니실내화/필로티/지하/다락/외부증축/세대분할)을 두고 위 규칙을 자동 적용, 결과는 건축사 확인 후 확정(override 로그).

### 5-2. 적격성(사전판정 연동)
- 완공일 ≤ 2023-12-31, 주거 비율 ≥ 50%(LawParams), 유형별 면적 상한(LawParams: 단독165/조례330, 다가구660*, 다세대 세대당 85 — Unit.excl_area_m2), 제외구역 없음, 과거 특조법 승인+소유권 변동 없음 아님
- 소방 특례 가능성(영 제7조): road_width_m ≥ 4 and 건축물 경계 ≤ 30m, 또는 소화전 20m, 또는 건축불허 공지 접함 → "소방상 지장" note 제안

### 5-3. 위반구분(신고서 1-02) 자동 체크
| 체크 | 조건 |
|---|---|
| 신축 | permit_exists=false and 전층 무허가 |
| 증축 | V-A(무허가 or 허가외) 면적 > 0 |
| 개축·재축·이전 | tag 또는 수동 |
| 대수선 | V-B 존재 |
| 용도변경 | V-C 존재 |
| 기타 | V-E~V-Z만 존재 |

### 5-4. 조사서 위반사항 7항목(S1-16) 자동 체크
| 항목 | 조건 |
|---|---|
| 면적 및 층수 위반 | V-D 존재 or 현재 층수 > 허가 층수 or 연면적(현재) > at_permit.gross_area |
| 건축선 침범 | Setback.bldg_line_intrusion.flag |
| 건축선 이격거리 위반 | Setback.bldg_line.flag |
| 인접대지경계선 이격 위반 | Setback.adj_boundary.flag |
| 대수선 위반 | V-B |
| 용도변경 위반 | V-C |
| 그 밖 | V-H, V-Z |

### 5-6. 주차장 판정(법 제7조, 조사서 S1-13~15)
```
전세사기피해주택이면 → 비대상(제2항)
세대·가구·호수 증가 없음 and 근생등→주택 용도변경 없음 → 비대상(제1항 본문)
그 외 → 검토 대상(제1항 단서): needed = max(0, 기준 대수(주차장법·조례) − 현재 대수)
      조례 면제면 면제 □, 아니면 확보방법 = 설치 | 비용납부(조례 단가×needed) 선택
```
기준 대수 산정은 JurisdictionParams.parking_rule, MVP는 수동 입력 허용. 필로티 전용 위반이 있으면 현재 대수 자동 감소.

### 5-5. 이행강제금
- Penalty 합계를 신고서 2쪽 ②에 층별 또는 동별로 기재. 과태료 추정(D-5)은 별도 문서, 서식에는 쓰지 않는다.

---

## 6. 검증 규칙(P4 패널)

| 코드 | 수준 | 규칙 |
|---|---|---|
| E-REQ | 오류 | §4 필수(●) 누락 |
| E-SUM | 오류 | 층별 합계 ≠ Σ 행, 동 합계 ≠ Σ 층 |
| E-SYNC | 오류 | 신고서 2쪽 vs 조사서 2쪽 동일 항목 불일치(층수·구조·높이·이격·층별 위반면적) — 단일 소스라 발생하면 버그로 로그 |
| E-FORM | 오류 | 숫자 형식, 음수, 날짜 범위 |
| E-SIGN | 오류 | SIGNED 문서 수정 시도 → 새 버전 강제 |
| W-ELIG | 경고 | 적격성 축 미충족(완공일·면적·구역·주거비율) |
| W-UNIT | 경고 | 다세대 세대 전용면적 > 85㎡ |
| W-HEIGHT | 경고 | 최고 < 처마 < 반자 순서 역전 |
| W-ATTACH | 경고 | 첨부 3종·사용승낙·공유자 동의 누락 |
| W-AI | 경고 | AI 초안 미확정 상태로 출력 |
| W-OVERRIDE | 정보 | 자동 칸 수동 override 존재(출력물에 표시 안 함, 로그만) |
| W-PAGE | 정보 | 층 8행/5열 초과로 쪽 복제됨 |
| W-PHOTO | 경고 | 조사 분야별 필수 촬영 세트(전경 4면·위반부 원경/근경·실측·내부 용도·피난소방·접도) 누락 |
| E-PHOTO-VIO | 오류 | Violation 행에 대표 사진 없음(별지 2 3쪽 생성 불가) |

---

## 7. 서식 생성(hwpx 템플릿 엔진)

### 7-1. 템플릿 전략
- 법정 원본 hwpx(입법예고본)에서 **별지 1·2만 분리**한 템플릿 2개를 `templates/legal/special_act_decree_2026_draft/`에 둔다(form1_report.hwpx, form2_survey.hwpx).
- 아키엠 표준 템플릿은 법정 원본 복제본의 각 칸 텍스트에 **치환 태그**를 심은 것(`templates/standard/`). 레이아웃·글꼴·표 구조는 원본 유지.
- hwpx = zip(XML). `Contents/section0.xml`의 `<hp:t>` 텍스트 노드를 치환한다. 표 반복 행은 `<hp:tr>` 복제, 쪽 복제는 해당 표·단락 블록 복제.
- 체크박스는 원문 글자 `[  ]`→`[√]`, `□`→`■` 치환.
- **이미지 삽입**: 사진은 hwpx `BinData/` 에 파일 추가 + `Contents/header.xml`의 `<hh:binData>` 항목 등록 + `section0.xml`의 사진 셀 안 `<hp:pic>` 요소(binaryItemIDRef)로 참조. 템플릿의 사진 셀에는 플레이스홀더 그림 1개를 두고 복제·교체한다. 사진대지 쪽은 아키엠 기존 사진대지 생성 경로(08·14)의 hwp 출력 모듈을 호출해 조사서 뒤에 병합(섹션 추가).
- pdf는 hwpx → LibreOffice(hwp 필터) 또는 한컴 변환 서버. MVP는 서버 변환 1경로 + 실패 시 HTML 레이아웃 pdf 폴백(법정 서식과 시각 차이 있음을 표시).

### 7-2. 치환 태그 사전(발췌, 전체는 코드 생성)
```
{{applicant.name}} {{applicant.id_no}} {{applicant.address}} {{applicant.phone}}
{{architect.name}} {{architect.license_no}} {{architect.office_name}} {{architect.office_reg_no}} {{architect.office_address}} {{architect.phone}}
{{bldg.main_use}} {{bldg.sub_use}} {{bldg.start_date}} {{bldg.completion_date}}
{{site.address}} {{site.area_m2}} {{site.jimok}} {{site.road_width_m}} {{site.zone}} {{site.frontage_m}} {{site.district}} {{site.area_type}}
{{chk.violation.신축}} … {{chk.violation.기타}} {{chk.permit.해당}} {{chk.permit.미해당}}
{{chk.ownership.자가}} {{chk.ownership.사용승락}} {{chk.ownership.국공유}}
{{cur.bldg_area}} {{cur.gross_area}} {{cur.far}} {{cur.bcr}} {{cur.main_use}} {{cur.units}} {{cur.parking}}
{{permit.bldg_area}} … {{permit.parking}}
{{dong.name}} {{dong.floors_above}} {{dong.floors_below}} {{dong.roof}} {{dong.wall}} {{dong.floor}} {{dong.h_max}} {{dong.h_eave}} {{dong.h_ceiling}}
{{#floors}} {{floor.label}} {{floor.legal_area}} {{floor.v_permit}} {{floor.v_nonpermit}} {{floor.v_type}} {{floor.v_year}} {{floor.units}} {{floor.pen_count}} {{floor.pen_amount}} {{floor.pen_paid}} {{/floors}}
{{#units}} {{unit.no}} {{unit.floor}} {{unit.area}} {{/units}}
{{setback.bl_m}} {{setback.bl_area}} {{setback.adj_m}} {{setback.adj_area}}
{{survey.site.safety}} {{survey.site.road}} {{survey.site.frontage3m}} {{survey.site.water}} {{survey.site.sewage}}
{{survey.bldg.structural}} {{survey.bldg.fire}} {{survey.bldg.hygiene}} {{survey.bldg.evac}} {{survey.bldg.planning}}
{{chk.parking.exempt_7_1}} {{chk.parking.exempt_7_2}} {{parking.needed}} {{chk.parking.exempt}} {{chk.parking.install}} {{chk.parking.pay}} {{chk.parking.na}}
{{chk.vio.area_floor.y}} {{chk.vio.area_floor.n}} … {{chk.vio.other.y}} {{chk.vio.other.n}}
{{opinion.text}}
{{sign.applicant}} {{sign.architect}} {{date.signed}}   # 항상 빈 값
{{#photos}} {{photo.dong}} {{photo.floor}} {{photo.area}} {{photo.caption}} {{photo.image}} {{/photos}}
```
- `{{#…}}…{{/…}}` 블록은 행·쪽 반복. `chk.*`는 boolean → `√`/공백.
- 태그 사전은 데이터 모델에서 자동 생성해 P7 서식 편집 화면의 삽입 목록으로 제공(form-set §J-3).

### 7-3. 생성 파이프라인
1. Document.data_snapshot 고정(JSON) → 2. 검증(§6) 오류 0 → 3. 템플릿 선택(template_id, version) → 4. 치환·반복·쪽 복제 → 5. hwpx 저장 → 6. pdf 변환 → 7. 해시·버전 기록 → 8. SIGNED면 서명 이미지 삽입 후 재생성(서명본과 미서명본 분리 보관)

---

## 8. AI 보조(D-2, LLM 프롬프트)

| 호출 | 입력 | 출력 | 서식 반영 |
|---|---|---|---|
| AI-1 위반유형 제안 | 사진+문맥 | code(V-A~V-Z), tag, confidence, evidence | Violation 초안(확정 전 서식 미반영) |
| AI-2 대장 OCR | 대장·토지이용·등기 이미지 | BuildingRecord/Site/at_permit JSON, low_confidence_fields | 검토 후 저장 |
| AI-3 조사서 서술 초안 | SurveyChecks 결과·사진 caption·실측·플래그 | site 5항목 서술, bldg 5항목 {has_issue 제안, note}, opinion_draft, supplements_needed | 「AI 초안·확인 전」, 건축사 확정 시만 서식 삽입 |
| AI-4 위반내용 문장 | Violation 행 | 서식 "위반 내용" 1줄 문장(예: "옥상 경량철골 증축, 주거 사용") | 행 note 초안 |

원칙: 적합·부적합 판정 금지, 입력에 없는 수치 생성 금지, 각 문장 근거 카드 id 부착, 토큰 상한.

---

## 9. 상태·권한·감사

### 9-1. 문서 상태 머신
`DRAFT` → (검증 통과) `REVIEW` → (건축사 서명) `SIGNED` → (접수번호 입력) `FILED` → `SUPPLEMENT`(보완) → `APPROVED`/`REJECTED`/`CONDITIONAL`(조건부, 이행기한)
- SIGNED 이후 데이터 변경 시 Document.version+1, 이전 파일 보존.

### 9-2. 권한
| 역할 | 입력 | 검증 | 서명 | 출력 | 템플릿 편집 |
|---|---|---|---|---|---|
| 건축사 | ○ | ○ | ○(현장조사자) | ○ | ○ |
| 조사 보조원 | ○ | ○ | × | 미서명 PDF만 | × |
| 건축주(초대) | 신고인 정보·증빙만 | × | 신고인 서명(선택) | 미리보기 | × |
| 관리자 | × | × | × | ○ | 법정 원본·지자체 |

### 9-3. 감사 로그
자동 칸 override, AI 제안 확정/수정, 서명, 출력, 버전 생성 — 사용자·시각·전후값.

---

## 10. API 초안(REST)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /projects | type=SPECIAL_ACT_SURVEY 생성 |
| PUT | /projects/{id}/applicant · /architect · /site · /building | 기본 정보 |
| POST/PUT/DELETE | /projects/{id}/dongs, /dongs/{d}/floors, /floors/{f}/violations | 구조·위반 |
| PUT | /dongs/{d}/setback, /dongs/{d}/units, /projects/{id}/penalties | |
| PUT | /projects/{id}/survey-checks | 조사서 1쇽 항목 |
| POST | /projects/{id}/ai/violation-suggest · /ai/ocr · /ai/survey-draft | AI-1~3 |
| GET | /projects/{id}/calculation | §5 계산 결과 |
| GET | /projects/{id}/validate | §6 결과 |
| POST | /projects/{id}/documents/{FORM1|FORM2}/render?format=hwpx|pdf | 생성 |
| POST | /documents/{doc}/sign | 서명 → SIGNED |
| GET | /projects/{id}/package.zip | 신고 패키지 |
| GET/POST | /templates, /templates/{id}/download, /templates/{id}/versions | 라이브러리 |
| PUT | /projects/{id}/filing | 접수·단계 |

---

## 11. 비기능

- 오프라인: 모바일 입력(M5~M9)은 로컬 저장 후 동기화. 충돌은 서버 타임스탬프 우선 + 로그.
- 성능: hwpx 생성 3초 이내(사진 20장 기준), pdf 변환 10초 이내.
- 보존: 서명본·미서명본·데이터 스냅샷 제도 종료 후 5년(설정값).
- 보안: 신고인 식별번호 암호화, 초대 링크 만료·1회 인증.
- 법령 파라미터: LawParams(면적 상한·주거비율·기준일·과태료 산식), 템플릿 버전(2026_draft → 2026_final).

---

## 12. 수용 기준(MVP)

1. 샘플 현장(다가구 3층+옥탑 무단증축, 1층 필로티 전용) 데이터로 신고서·현장조사서 hwpx 2종이 **서식 원본과 동일 레이아웃**으로 생성되고, 모든 칸이 데이터와 1:1로 채워진다.
2. 신고서 2쪽과 조사서 2쪽의 층수·구조·높이·이격·층별 위반면적이 완전히 일치한다(E-SYNC 0건).
3. 층 9개(지하1+지상7+옥탑) 입력 시 신고서 2쪽·조사서 2쪽이 자동으로 쪽 복제된다.
4. 다세대 샘플에서 세대 전용면적 86㎡ 입력 시 W-UNIT 경고가 뜨고 적격성 카드가 "초과"로 바뀐다.
5. 서명 전 출력물에 "미서명" 워터마크, 서명 후 현장조사자 서명 이미지 삽입, 서명 후 수정 시 버전 2가 생성된다.
6. AI-3 초안이 확정되지 않은 상태로 출력하면 W-AI 경고와 "초안" 워터마크가 표시된다.
7. 템플릿 라이브러리에서 별지 1·2 빈 서식(법정 원본 hwpx·pdf)을 내려받을 수 있다.
8. 신고 패키지 ZIP에 신고서·조사서(현장사진 쪽 + **사진대지 병합본**)·사진대지 단독 pdf·위반현황표·공부 사본·증빙 목록·목차가 들어간다.
9. 사진 25장 입력 시 조사서 3쪽이 13쪽으로 복제되고, 서식 3쪽에 배치되지 않은 사진이 사진대지 2장형으로 뒤에 이어 붙으며 사진번호가 연속된다.

---

## 13. 구현 순서(MVP, 2026.12.17 전)

| 주 | 범위 |
|---|---|
| 1 | 데이터 모델·API(§3·§10), 유형 추가, LawParams 테이블, 템플릿 분리(별지 1·2 hwpx) |
| 2 | PC 폼 뷰 P2·P3(수동 입력 전부 가능), 계산 §5, 검증 §6 |
| 3 | hwpx 치환 엔진(반복 행·쪽 복제·체크박스·**사진 BinData 삽입·사진대지 병합**), pdf 변환, 서명·버전 |
| 4 | 모바일 M1~M9(기존 화면 변형), 사진·위반 연결, 오프라인 |
| 5 | AI-1~AI-4 프롬프트 연결, 초안 배지·확정 로그 |
| 6 | 템플릿 라이브러리 P7, 신고 패키지 ZIP, 신고 진행 P8 |
| 7 | 샘플 현장 3건 E2E, 건축사 2인 검수, 수용 기준 §12 통과 |
| 8 | 버퍼·공포본 반영(2026_final), 배포 |

---

## 14. 미결·확인 필요

- ~~신고서 1-15 단위~~ → **m로 확정**(대표 결정 2026-10-18). 공포본에서 정정되면 법정 원본 템플릿만 교체. 11.9 의견 제출 시 오기 정정 요청 항목으로 포함.
- ~~건축면적·바닥면적 산정(옥탑·발코니)~~ → 조사 메모 §1-4 규칙으로 **설계 반영**(§5-1a). 공포본·현행 제119조 목 번호 대조와 건축사 최종 확인만 남음.
- ~~주차 추가 설치 의무(법 제7조)~~ → 조사 메모 §2 취지(1항 본문 비대상 / 단서 세대수 증가·용도변경 검토 / 2항 전세사기피해주택 / 조례 면제)로 **§5-6 로직 반영**. 공포본 문언 대조 필요.
- 「건축물의 설계도서 작성기준」 개정 고시(2026.9 행정예고)와 영 제6조2항 설계도서 범위의 관계.
- 지자체가 별지 외 자체 서식(접수 체크리스트 등)을 요구할 경우 JurisdictionParams 템플릿으로 흡수.
