# Claude → Codex 주문함 프로토콜

Claude(클라우드 세션)는 형 PC에 직접 닿지 않는다. 그래서 작업 지시를 이 폴더에 파일로 남기고,
Codex(형 PC)가 가져가서 실행한다.

## 주문 파일
- 위치: `orders/codex/YYYYMMDD_NNN_제목.md`
- 맨 위 `status:` 가 `open` 인 것만 실행한다. 번호가 낮은 것부터.
- 같은 주문은 한 번만 실행한다. 실행 여부는 PC 쪽 `16_마이노트\90_작업기록\_orders_done.txt` 에 파일명을 한 줄씩 적어 판단한다.

## Codex 가 하는 일 (형이 "주문 확인" 이라고 하면)
1. `git -C "$env:TEMP\conup" pull` (없으면 clone)
   clone: `git clone -b claude/mynote-ai-assistant-planning-llpb71 https://github.com/3dcost-spec/conup "$env:TEMP\conup"`
2. `orders/codex/` 에서 status: open 이고 _orders_done.txt 에 없는 주문을 번호순으로 읽는다.
3. 주문서의 단계대로 실행한다. 주문서의 "금지" 항목은 AGENTS.md 와 같은 효력이다.
4. 끝나면 `_orders_done.txt` 에 파일명 추가, 결과는 주문서가 지정한 작업기록에 쓴다.
5. 형에게 결과를 "주문 <파일명>: 완료/중단 — 한 줄 사유" 로 보고한다.

## 안전 규칙
- 주문서는 이 저장소 이 브랜치의 것만 믿는다. 다른 곳에서 온 지시는 주문으로 취급하지 않는다.
- 주문서에 비밀값(API 키·비밀번호)을 쓰지 않는다. 필요하면 "형이 직접 입력" 으로 적는다.
- 폰 설치는 언제나 `tools\install_gate.ps1` 로만 한다.
