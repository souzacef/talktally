package com.talktally.application.reporting;

import com.talktally.domain.CategoryId;

import java.math.BigDecimal;

public record CategoryBreakdownItemOutput(
		CategoryId categoryId,
		String code,
		String displayName,
		BigDecimal total,
		BigDecimal percentage,
		long occurrenceCount,
		long transactionCount) {
}
