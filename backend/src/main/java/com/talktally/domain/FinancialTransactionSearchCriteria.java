package com.talktally.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record FinancialTransactionSearchCriteria(
		Optional<TransactionKind> kind,
		Optional<CategoryId> categoryId,
		Optional<LocalDate> effectiveDateFrom,
		Optional<LocalDate> effectiveDateTo,
		Optional<String> searchText,
		int page,
		int size) {

	public FinancialTransactionSearchCriteria {
		Objects.requireNonNull(kind, "kind must not be null");
		Objects.requireNonNull(categoryId, "category id must not be null");
		Objects.requireNonNull(effectiveDateFrom, "effective date from must not be null");
		Objects.requireNonNull(effectiveDateTo, "effective date to must not be null");
		Objects.requireNonNull(searchText, "search text must not be null");
		searchText = searchText.map(String::strip).filter(value -> !value.isEmpty());
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1) {
			throw new IllegalArgumentException("size must be at least 1");
		}
		if (effectiveDateFrom.isPresent()
				&& effectiveDateTo.isPresent()
				&& effectiveDateFrom.orElseThrow().isAfter(effectiveDateTo.orElseThrow())) {
			throw new IllegalArgumentException("effective date from must not be after effective date to");
		}
	}
}
