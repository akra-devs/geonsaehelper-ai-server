package com.akra.geonsaehelperaiserver.domain.ai.service

import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorContext
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductType
import org.springframework.stereotype.Component

@Component
class LoanProductFallbackProvider {

    fun createContexts(productTypes: Set<LoanProductType>): List<LoanAdvisorContext> {
        if (productTypes.isEmpty()) {
            return emptyList()
        }
        var rank = 1
        val contexts = mutableListOf<LoanAdvisorContext>()
        productTypes
            .sortedBy { it.ordinal }
            .forEach { type ->
            val summary = fallbackByType[type] ?: return@forEach
            contexts += LoanAdvisorContext(
                rank = rank++,
                id = "fallback:${type.name.lowercase()}",
                productType = type.name,
                productTypeDescription = type.description,
                score = null,
                content = summary
            )
        }
        return contexts
    }

    companion object {
        private val fallbackByType: Map<LoanProductType, String> = mapOf(
            LoanProductType.RENT_STANDARD to """
                상품명: 버팀목전세자금
                핵심 자격요건:
                - 무주택 세대주
                - 부부합산 연소득 5천만원 이하
                - 부부합산 순자산 3.37억원 이하
                주요 대출조건:
                - 금리: 연 2.5%~3.5%
                - 대출한도: 수도권 1.2억원, 그 외 지역 0.8억원(임차보증금의 80% 이내)
                - 기간: 2년 약정, 최장 10년까지 연장 가능
            """.trimIndent(),
            LoanProductType.RENT_YOUTH_BEOTIMMOK to """
                상품명: 청년전용 버팀목전세자금
                핵심 자격요건:
                - 만 19~34세 무주택(예비)세대주
                - 부부합산 연소득 5천만원 이하
                - 부부합산 순자산 3.37억원 이하
                주요 대출조건:
                - 금리: 연 2.2%~3.3%
                - 대출한도: 최대 1.5억원(보증금의 80% 이내)
                - 기간: 2년 약정, 최장 10년
            """.trimIndent(),
            LoanProductType.RENT_NEWLYWED to """
                상품명: 신혼부부전용 전세자금
                핵심 자격요건:
                - 혼인 7년 이내(또는 3개월 내 결혼예정) 무주택 세대주
                - 부부합산 연소득 7.5천만원 이하
                - 부부합산 순자산 3.37억원 이하
                주요 대출조건:
                - 금리: 연 1.9%~3.3%
                - 대출한도: 수도권 2.5억원, 비수도권 1.6억원(임차보증금의 80% 이내)
                - 기간: 2년 약정, 최장 10년
            """.trimIndent(),
            LoanProductType.RENT_NEWBORN_SPECIAL to """
                상품명: 신생아 특례 버팀목전세자금
                핵심 자격요건:
                - 대출접수일 기준 2년 내 출산한 무주택 세대주(2023.1.1. 이후 출생)
                - 부부합산 연소득 1.3억원 이하(맞벌이 2억원 이하)
                - 부부합산 순자산 3.37억원 이하
                주요 대출조건:
                - 금리: 연 1.3%~4.3%
                - 대출한도: 최대 2.4억원(보증금의 80% 이내)
                - 기간: 2년 약정, 최장 12년
            """.trimIndent(),
            LoanProductType.RENT_RENEWAL_SUPPORT to """
                상품명: 갱신만료 임차인 지원 버팀목전세자금
                핵심 자격요건:
                - 기존 버팀목 계열 대출 이용 중인 무주택 세대주
                - 2020.8.1.~2021.7.31. 갱신요구권 행사 후 동일 주택으로 증액 갱신계약 체결
                주요 대출조건:
                - 금리: 기존 계좌 적용금리 유지
                - 대출한도: 수도권 4.5억원, 비수도권 2.5억원
                - 기간: 기존 계좌의 최종 만기일 이내에서 2년 약정
            """.trimIndent(),
            LoanProductType.RENT_DAMAGES_STANDARD to """
                상품명: 전세피해 임차인 버팀목전세자금
                핵심 자격요건:
                - 무주택 세대주
                - 부부합산 연소득 1.3억원 이하, 순자산 4.88억원 이하
                - 전세피해주택 보증금 5억원 이하이며 보증금의 30% 이상 피해 발생
                주요 대출조건:
                - 금리: 연 1.2%~2.7%
                - 대출한도: 최대 2.4억원
                - 기간: 2년 약정, 최장 10년
            """.trimIndent(),
            LoanProductType.RENT_DAMAGES_PRIORITY to """
                상품명: 전세사기피해자 최우선변제금 버팀목 전세자금 대출
                핵심 자격요건:
                - 보증금 3억원 이하 계약 + 보증금 5% 이상 지급
                - 특별법상 전세사기피해자에 해당
                - 세대주이며 세대원 전원 무주택
                - 소득·자산 제한 없음
                주요 대출조건:
                - 금리: 연 1.2%~3.0%
                - 대출한도: 최대 2.4억원(기존 대출과 합산)
                - 기간: 2년 약정, 최장 10년
            """.trimIndent(),
            LoanProductType.REFINANCE_DAMAGES to """
                상품명: 전세피해 임차인 대상 버팀목전세대출대환
                핵심 자격요건:
                - 전세피해 요건 충족 및 임차권등기 설정
                - 부부합산 연소득 1.3억원 이하, 순자산 4.88억원 이하
                주요 대출조건:
                - 금리: 연 1.2%~2.7%
                - 대출한도: 최대 4억원
                - 기간: 6개월(보증기관 연장 기준 따름)
            """.trimIndent(),
            LoanProductType.RENT_VULNERABLE_MOVE to """
                상품명: 주거취약계층 이주지원 버팀목전세자금
                핵심 자격요건:
                - 주거취약계층 주거지원 지침 대상자
                - 신청일 기준 3개월 이상 거주 중인 무주택 세대주
                주요 대출조건:
                - 금리: 5천만원 이내 구간 연 0%, 초과분 연 1.2%~1.8%
                - 대출한도: 공공임대 보증금 50만원, 민간임대 보증금 8천만원
                - 기간: 공공임대 2년(최장 20년), 민간임대 2년(최장 10년)
            """.trimIndent(),
            LoanProductType.RENT_YOUTH_MONTHLY to """
                상품명: 청년전용 보증부월세대출
                핵심 자격요건:
                - 만 19~34세 무주택 단독 세대주(예비세대주 포함)
                - 부부합산 연소득 5천만원 이하, 순자산 3.37억원 이하
                주요 대출조건:
                - 금리: 보증금 연 1.3%, 월세 20만원 초과분 연 1.0%(20만원 이내 구간 0%)
                - 대출한도: 보증금 최대 4,500만원, 월세 최대 1,200만원(24개월 기준)
                - 기간: 25개월 기본, 최장 10년 5개월
            """.trimIndent()
        )
    }
}
