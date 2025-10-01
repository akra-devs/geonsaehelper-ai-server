package com.akra.geonsaehelperaiserver.config

import com.akra.geonsaehelperaiserver.domain.vector.LoanProductType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class LoanProductTypeConverter : Converter<String, LoanProductType> {
    override fun convert(source: String): LoanProductType {
        return LoanProductType.fromValue(source)
    }
}
