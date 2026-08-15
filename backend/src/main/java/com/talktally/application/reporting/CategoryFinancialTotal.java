package com.talktally.application.reporting;

import com.talktally.domain.CategoryId;

import java.math.BigDecimal;
import java.util.Objects;

public record CategoryFinancialTotal(
		CategoryId categoryId,
		String categoryCode,
		String categoryDisplayName,
		BigDecimal total,
		long occurrenceCount,
		long transactionCount) {

	public CategoryFinancialTotal {
		Objects.requireNonNull(categoryId, "category id must not be null");
		Objects.requireNonNull(categoryCode, "category code must not be null");
		Objects.requireNonNull(categoryDisplayName, "category display name must not be null");
		Objects.requireNonNull(total, "category total must not be null");
	}
}
