package com.talktally.application.reporting;

import com.talktally.domain.TransactionKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CategoryBreakdownOutput(
		LocalDate from,
		LocalDate to,
		TransactionKind kind,
		String currency,
		BigDecimal total,
		List<CategoryBreakdownItemOutput> categories) {

	public CategoryBreakdownOutput {
		categories = List.copyOf(categories);
	}
}
