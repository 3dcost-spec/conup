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
| 원칙 | 서식의 모든 칸은 **데이터 모델의 필드 1개와 1:1**로 연결된다. 사람이 두 번 치는 칸이 없다. 서명·날짜 칸은 자동 생성 시 비운다 |
| 유형 표기 | 등록 화면 4번째 유형 **「양성화 현장조사」**(공사·해체·석면해체 다음), 현장 목록 「양성화」 탭 |
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
| M1 현장 등록 | 02·03 | 「현장 등록」 → 감리 유형 선택 화면의 순서를 **공사감리 → 해체감리 → 석면해체감리 → 양성화 현장조사** 로 확정(대표 지시 2026-10-18). 화면 표기명 「양성화 현장조사」(부제: 특정건축물 정리법), 내부 코드 `SPECIAL_ACT_SURVEY`. 선택 시 공간 구조 = 동(주건축물 제1동 …) × 층(지하 n~지상 n, 옥탑), 공간 체크 항목을 조사용으로 교체, 감리일지 서식·직책 선택은 숨김 |
| M2 공부 등록 | 04 AI 등록 | 「대장 찍기」「토지이용계획 찍기」「등기 찍기」 3버튼. OCR 결과를 **대장 현황 카드**로 표시, 확인 후 저장(BuildingRecord·Site) |
| M3 조사 메인 | 05 달력 | **단계 카드**: 공부 → 동·층 → 위반 입력 → 조사 체크 → 사진 → 초안. 각 카드에 완료율 |
| M4 위치·분야 선택 | 06 | 동 → 층 → 분야(대지/외관규모/층별내부/구조/피난방화/설비/일조인접) |
| M5 사진 | 07 | 사진보드에 동·층·위반유형·실측치. **AI 위반유형 제안**(V코드+하위 태그) → [맞아요]/[고칠게요]. 마크업 줄자로 치수 |
| M6 위반 입력(층별) | 신규 | 층 선택 → 행 추가: permit_basis(무허가/허가부분/허가외), 위반유형 코드, 바닥면적(㎡), 위반시점(연도), 위반내용, 세대수. 사진 연결 |
| M7 이격 입력 | 신규 | 건축선 침범·건축선 이격·인접대지 이격: 거리 m, 위반면적 ㎡, 사진 |
| M8 조사 체크 | 10 체크리스트 | 분야별 항목. 조사서 1쪽 **지장 유무 5종 + 대지 5항목 + 주차**를 직접 입력하는 화면 |
| M9 실측 | 신규(간단) | 최고·처마·반자 높이, 전면도로폭, 접도 길이(3m 이상 도로), 세대 전용면적 |
| M10 AI 초안 | 11 생성형 일지 | 「조사서 초안 만들기」 → 종합의견·지장 내용 초안, 「AI 초안·확인 전」 배지 |
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

### 2-3. 폼 뷰 공통 규칙
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
| 1-15 | 도로에 접하는 대지의 길이 | Site.frontage_m | M(실측) | ● | 서식 단위 표기는 원본대로 "㎡" 인쇄(원본 오기 가능성, 값은 m 입력·m 저장. 공포본 확인) |
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

### 4-6. 별지 제2호 현장조사서 3쪽 — 현장사진
블록당 위치(동·층)·위반면적·위반내용·사진 1장, 쪽당 2블록. Violation별 대표 사진 1장씩 자동 배치(사진 수만큼 쪽 추가). 사진대지 D-2의 2장형 레이아웃 재사용.

---

## 5. 계산·판정 규칙

### 5-1. 면적
- 층별 현재 바닥면적 = legal_area + Σ violation.area(해당 층, type∈{V-A, V-D, V-C는 면적 중복 주의: 용도변경은 기존 면적 내이므로 **합산 제외**, 별도 열})
- 연면적(현재) = Σ 층별 현재 바닥면적(지하 포함, 옥탑은 건축법 산정 규칙 플래그로 포함/제외 선택)
- 건축면적(현재) = max 층 수평투영(기본 1층) + 수평 증축분(사용자 입력 또는 1층 위반면적 가산 옵션)
- 건폐율 = 건축면적 / Site.area_m2 × 100, 용적률 = 지상 연면적 / Site.area_m2 × 100
- 용도별 면적 = Σ (Floor/Violation.use → 면적), 용도 코드는 건축법 시행령 별표1

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

---

## 7. 서식 생성(hwpx 템플릿 엔진)

### 7-1. 템플릿 전략
- 법정 원본 hwpx(입법예고본)에서 **별지 1·2만 분리**한 템플릿 2개를 `templates/legal/special_act_decree_2026_draft/`에 둔다(form1_report.hwpx, form2_survey.hwpx).
- 아키엠 표준 템플릿은 법정 원본 복제본의 각 칸 텍스트에 **치환 태그**를 심은 것(`templates/standard/`). 레이아웃·글꼴·표 구조는 원본 유지.
- hwpx = zip(XML). `Contents/section0.xml`의 `<hp:t>` 텍스트 노드를 치환한다. 표 반복 행은 `<hp:tr>` 복제, 쪽 복제는 해당 표·단락 블록 복제.
- 체크박스는 원문 글자 `[  ]`→`[√]`, `□`→`■` 치환.
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
8. 신고 패키지 ZIP에 신고서·조사서·사진대지·위반현황표·공부 사본·증빙 목록·목차가 들어간다.

---

## 13. 구현 순서(MVP, 2026.12.17 전)

| 주 | 범위 |
|---|---|
| 1 | 데이터 모델·API(§3·§10), 유형 추가, LawParams 테이블, 템플릿 분리(별지 1·2 hwpx) |
| 2 | PC 폼 뷰 P2·P3(수동 입력 전부 가능), 계산 §5, 검증 §6 |
| 3 | hwpx 치환 엔진(반복 행·쪽 복제·체크박스), pdf 변환, 서명·버전 |
| 4 | 모바일 M1~M9(기존 화면 변형), 사진·위반 연결, 오프라인 |
| 5 | AI-1~AI-4 프롬프트 연결, 초안 배지·확정 로그 |
| 6 | 템플릿 라이브러리 P7, 신고 패키지 ZIP, 신고 진행 P8 |
| 7 | 샘플 현장 3건 E2E, 건축사 2인 검수, 수용 기준 §12 통과 |
| 8 | 버퍼·공포본 반영(2026_final), 배포 |

---

## 14. 미결·확인 필요

- 신고서 1-15 "도로에 접하는 대지의 길이" 단위가 원문에 ㎡로 인쇄됨 → 공포본에서 m로 정정되는지 확인. 앱은 m로 저장하고 인쇄 단위는 템플릿 버전에 따름.
- 건축면적(현재) 산정에서 옥탑·발코니 확장분의 수평투영 처리 규칙(건축법 시행령 제119조 준용 여부) → 건축사 자문.
- 주차 추가 설치 의무(법 제7조1·2항)의 판정 로직은 법 조문 확보 후 LawParams에 규칙화.
- 「건축물의 설계도서 작성기준」 개정 고시(2026.9 행정예고)와 영 제6조2항 설계도서 범위의 관계.
- 지자체가 별지 외 자체 서식(접수 체크리스트 등)을 요구할 경우 JurisdictionParams 템플릿으로 흡수.
