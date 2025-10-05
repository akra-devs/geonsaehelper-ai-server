# HUG_POLICY — HUG 정책/상품 정리(정규화 버전)
Status: canonical (HUG Programs)

last_verified: 2025-09-08

## 데이터 스키마(요약)
- id: 고유 ID (영문 대문자+언더스코어)
- name: 상품명(한글)
- category: rent(임차) | purchase(구입) | refinance(대환) | special(특례/취약)
- eligibility: 핵심 자격 요건 요약(리스트)
- income_cap: 텍스트(예: "부부합산 5천만원 이하")
- asset_cap: 텍스트(예: "순자산 3.37억원 이하")
- age_range: 텍스트(예: "만 19–34세")
- rate: 텍스트(범위/구간)
- limit: 텍스트(상한/구간)
- term: 텍스트(기간/연장 조건)
- region_limits: 지역별 상한(있을 때)
- ltv_dti: LTV/DTI 규정(해당 시)
- notes: 기타 특이사항

---

## Programs

### RENT_YOUTH_MONTHLY — 청년전용 보증부월세대출
- category: rent
- eligibility:
  - 무주택 단독 세대주(예비세대주 포함)
  - 부부합산 연소득 5천만원 이하, 순자산 3.37억원 이하
  - 만 19–34세 청년
- income_cap: 부부합산 5천만원 이하
- asset_cap: 3.37억원 이하
- age_range: 만 19–34세
- rate: (보증금) 연 1.3%, (월세금) 연 0%(20만원 한도)/연 1.0%(20만원 초과)
- limit: (보증금) 최대 4,500만원, (월세금) 최대 1,200만원(24개월 기준: 월 50만원)
- term: 25개월(최장 10년 5개월)
- notes: 월세 이자우대 구간 존재

### RENT_STANDARD — 버팀목전세자금
- category: rent
- eligibility:
  - 무주택 세대주
  - 부부합산 연소득 5천만원 이하, 순자산 3.37억원 이하
- income_cap: 부부합산 5천만원 이하
- asset_cap: 3.37억원 이하
- rate: 연 2.5%–3.5%
- region_limits: 수도권 1.2억원, 수도권 외 0.8억원
- limit: 지역 상한 이내
- term: 2년(최장 10년)

### RENT_RENEWAL_SUPPORT — 갱신만료 임차인 지원 버팀목전세자금
- category: rent
- eligibility:
  - 버팀목 계열(신혼/청년/중소기업청년 전월세 포함) 대출 이용 중
  - '20.8.1~'21.7.31 갱신요구권 행사 후 동일 주택으로 보증금 증액 갱신계약 체결 세대주
- rate: 신청자격에 따른 기존 금리 적용
- region_limits: 수도권 4.5억원, 수도권 외 2.5억원
- term: 2년(최초 취급 계좌의 최종만기일 이내 운용)


### RENT_YOUTH_BEOTIMMOK — 청년전용 버팀목전세자금
- category: rent
- eligibility:
  - 무주택 세대주(예비세대주 포함)
  - 부부합산 연소득 5천만원 이하, 순자산 3.37억원 이하
  - 만 19–34세
- income_cap: 부부합산 5천만원 이하
- asset_cap: 3.37억원 이하
- age_range: 만 19–34세
- rate: 연 2.2%–3.3%
- limit: 최대 1.5억원(임차보증금의 80% 이내)
- term: 2년(최장 10년)

### RENT_NEWBORN_SPECIAL — 신생아 특례 버팀목전세자금
- category: special
- eligibility:
  - 대출접수일 기준 2년 내 출산(’23.1.1. 이후 출생) 무주택 세대주
  - 부부합산 연소득 1.3억원 이하(맞벌이 2억원 이하), 순자산 3.37억원 이하
- income_cap: 부부합산 1.3억원 이하(맞벌이 2억원 이하)
- asset_cap: 3.37억원 이하
- rate: 연 1.3%–4.3%
- limit: 최대 2.4억원(임차보증금의 80% 이내)
- term: 2년(최장 12년)

### RENT_VULNERABLE_MOVE — 주거취약계층 이주지원 버팀목전세자금
- category: special
- eligibility:
  - 「주거취약계층 주거지원 업무처리지침」 제3조 제1항 제1호·제3호 대상
  - 신청일 현재 3개월 이상 거주 중 무주택 세대주
- rate: 연 0%(5천만원 한도), 5천만원 초과분 연 1.2–1.8%
- limit: (공공임대) 보증금 50만원, (민간임대) 보증금 8천만원
- term: (공공임대) 2년(최장 20년), (민간임대) 2년(최장 10년)

### RENT_DAMAGES_STANDARD — 전세피해 임차인 버팀목전세자금
- category: special
- eligibility:
  - 무주택 세대주, 부부합산 연소득 1.3억원 이하, 순자산 4.88억원 이하
  - 전세피해주택 보증금 5억원 이하, 보증금의 30% 이상 피해
- rate: 연 1.2%–2.7%
- limit: 2.4억원 이내
- term: 2년(최장 10년)

### REFINANCE_DAMAGES — 전세피해 임차인 대상 버팀목전세대출대환
- category: refinance
- eligibility:
  - 전세피해 요건 충족 + 임차권등기 설정자
  - 부부합산 연소득 1.3억원 이하, 순자산 4.88억원 이하
- rate: 연 1.2%–2.7%
- limit: 4억원 이내
- term: 6개월(해당 보증기관 보증연장 기준 따름)

### RENT_DAMAGES_PRIORITY — 전세사기피해자 최우선변제금 버팀목 전세자금 대출
- category: special
- eligibility:
  - 임차보증금 3억원 이하 계약 + 보증금 5% 이상 지급
  - 「전세사기피해자 지원 및 주거안정 특별법」상 전세사기피해자
  - 세대주이며 세대원 전원 무주택
  - 소득/자산 제한 없음
- rate: 연 1.2%–3.0%
- limit: 2.4억원(일반대출 포함 합산)
- term: 2년(최장 10년)

### RENT_MONTHLY_STABILITY — 주거안정월세대출
- category: rent
- eligibility:
  - (우대형) 취업준비생, 희망키움통장 가입자, 근로/자녀장려금 수급자, 사회초년생, 주거급여수급자
  - (일반형) 부부합산 연소득 5천만원 이하(우대형 미해당)
  - (공통) 부부합산 순자산 3.37억원 이하
- income_cap: (일반형) 부부합산 5천만원 이하
- asset_cap: 3.37억원 이하
- rate: (우대형) 연 1.3%, (일반형) 연 1.8%
- limit: 최대 1,440만원(월 60만원)
- term: 2년(최장 10년)

### RENT_NEWLYWED — 신혼부부전용 전세자금
- category: rent
- eligibility:
  - 무주택 세대주, 신혼부부(혼인 7년 이내 또는 3개월 이내 결혼예정)
  - 부부합산 연소득 7.5천만원 이하, 순자산 3.37억원 이하
- income_cap: 부부합산 7.5천만원 이하
- asset_cap: 3.37억원 이하
- rate: 연 1.9%–3.3%
- region_limits: 수도권 2.5억원, 수도권 외 1.6억원(임차보증금 80% 이내)
- term: 2년(최장 10년)

### PURCHASE_YOUTH_DREAM_DIDIMDOL — 청년 주택드림 디딤돌 대출
- category: purchase
- eligibility:
  - 입주자모집공고일 이전 보유한 청년 주택드림 통장 연계(만 39세 이하 청약 당첨)
  - (요건) 가입 1년 이상 + 1천만원 이상 납입
  - 미혼 연소득 7천만원, 신혼 부부합산 1억원 이하, 순자산 4.88억원 이하, 무주택 세대주
- rate: 연 2.4%–4.15%
- limit: 미혼 3억원, 신혼 4억원(LTV 70%, DTI 60%; 생애최초 LTV 80% 예외, 수도권·규제지역 LTV 70%)
- ltv_dti: LTV 70%(생애최초 80%/수도권·규제지역 70%), DTI 60%

### PURCHASE_NEWLYWED — 신혼부부전용 구입자금
- category: purchase
- eligibility:
  - 무주택 세대주, 신혼부부(혼인 7년 이내 또는 3개월 내 결혼예정)
  - 부부합산 연소득 8.5천만원 이하, 순자산 4.88억원 이하
  - 생애최초 주택구입자
- rate: 연 2.55%–3.85%
- limit: 최대 3.2억원(LTV 80%, DTI 60%; 수도권·규제지역 LTV 70%)
- ltv_dti: LTV 80%(수도권·규제지역 70%), DTI 60%
- term: 10/15/20/30년(거치 1년 또는 비거치)

### PURCHASE_DIDIMDOL_STANDARD — 내집마련 디딤돌대출
- category: purchase
- eligibility:
  - 무주택 세대주, 순자산 4.88억원 이하
- income_cap: 부부합산 6천만원 이하(생애최초/2자녀 이상/신혼은 7천/7천/8.5천만원)
- rate: 연 2.85%–4.15%
- limit: 일반 2억원(생애최초 2.4억원), 신혼/2자녀 이상 3.2억원
- ltv_dti: LTV 70%(생애최초 80%/수도권·규제지역 70%), DTI 60%
- term: 10/15/20/30년(거치 1년 또는 비거치)

### PURCHASE_DIDIMDOL_DAMAGES — 전세사기피해자 전용 디딤돌 구입자금 대출
- category: purchase
- eligibility:
  - 전세사기피해자(특별법)
  - 무주택 세대주, 부부합산 연소득 7천만원 이하, 순자산 4.88억원 이하
- rate: 연 1.85%–2.70%
- limit: 최대 4억원(LTV 80%, DTI 100%)
- ltv_dti: LTV 80%, DTI 100%
- term: 10/15/20/30년(거치 1/2/3년 또는 무거치)

### PURCHASE_NEWBORN_DIDIMDOL — 신생아 특례 디딤돌대출
- category: purchase
- eligibility:
  - 대출접수일 기준 2년 내 출산(’23.1.1. 이후) 무주택 세대주 및 1주택 세대주(대환)
  - 부부합산 연소득 1.3억원 이하(맞벌이 2억원 이하), 순자산 4.88억원 이하
- rate: 연 1.8%–4.5%
- limit: 최대 4억원(LTV 70%, DTI 60%; 생애최초 80% 예외, 수도권·규제지역 70%)
- ltv_dti: LTV 70%(생애최초 80%/수도권·규제지역 70%), DTI 60%
- term: 10/15/20/30년(거치 1년 또는 비거치)

### PURCHASE_SHARED_PROFIT — 수익공유형 모기지
- category: purchase
- eligibility:
  - 무주택 세대주, 순자산 4.88억원 이하
  - 부부합산 총소득 6천만원 이하(생애최초 7천만원 이하)
- rate: 연 1.8%(고정)
- limit: 최고 2억원(주택가격의 최대 70%)
- term: 20년

### PURCHASE_SHARED_GAINLOSS — 손익공유형 모기지
- category: purchase
- eligibility:
  - 무주택 세대주, 순자산 4.88억원 이하
  - 부부합산 총소득 6천만원 이하(생애최초 7천만원 이하)
- rate: 최초 5년 연 1.3% 이후 연 2.3%(고정)
- limit: 최고 2억원(주택가격의 최대 40%)
- term: 20년

### PURCHASE_NEWLYWED_TOWN — 신혼희망타운전용 주택담보장기대출
- category: purchase
- eligibility:
  - LH 공급 주거전용면적 60㎡ 이하 신혼희망타운 입주자
- rate: 연 1.6%(고정)
- limit: 최고 4억원(주택가액 최대 70%)
- term: 20년 또는 30년

### SPECIAL_DISTRESSED_RENT_AUCTION — 부도임대주택경락자금
- category: special
- eligibility: 부도임대주택 경매낙찰 임차인
- rate: 10년간 연 2.6%(국토부 고시 변동)
- limit: 매각가격과 주택가격의 80% 중 적은 금액
- term: 1(3)년 이자만 + 19(17)년 분할상환

### SPECIAL_DISTRESSED_RENT_MOVEOUT — 부도임대주택퇴거자 전세자금
- category: special
- eligibility: 부도임대주택 퇴거(예정), 임차보증금 2억원 이하 계약, 임차보증금 5% 이상 지급, 만 19세 이상 무주택 세대주
- rate: 연 3.1%
- limit: 5천만원 이내
- term: 2년(2회 연장, 최장 6년)


---

## 비고/정규화 규칙
- 모든 금액 단위는 원문 그대로 유지(예: 1.2억원, 4,500만원).
- LTV/DTI는 원문에 있을 때만 표기, 없으면 생략.
- ‘상세보기’, ‘대출신청’ 등 링크/버튼 문구는 제거함.
- 동일 제목의 중복 라인은 병합.
- 본 문서는 정책 요약이며 실제 심사/취급 기준과 다를 수 있음.