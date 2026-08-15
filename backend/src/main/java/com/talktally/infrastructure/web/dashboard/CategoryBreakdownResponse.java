package com.talktally.infrastructure.web.dashboard;

import com.talktally.application.reporting.CategoryBreakdownOutput;
import com.talktally.domain.TransactionKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CategoryBreakdownResponse(
		LocalDate from,
		LocalDate to,
		TransactionKind kind,
		String currency,
		BigDecimal total,
		List<CategoryItemResponse> categories) {

	public CategoryBreakdownResponse {
		categories = List.copyOf(categories);
	}

	static CategoryBreakdownResponse from(CategoryBreakdownOutput output) {
		return new CategoryBreakdownResponse(
				output.from(),
				output.to(),
				output.kind(),
				output.currency(),
				output.total(),
				output.categories().stream()
						.map(item -> new CategoryItemResponse(
								item.categoryId().value(),
								item.code(),
								item.displayName(),
								item.total(),
								item.percentage(),
								item.occurrenceCount(),
								item.transactionCount()))
						.toList());
	}

	public record CategoryItemResponse(
			UUID categoryId,
			String code,
			String displayName,
			BigDecimal total,
			BigDecimal percentage,
			long occurrenceCount,
			long transactionCount) {
	}
}
