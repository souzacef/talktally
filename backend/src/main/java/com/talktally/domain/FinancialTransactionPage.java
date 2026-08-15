package com.talktally.domain;

import java.util.List;
import java.util.Objects;

public record FinancialTransactionPage(
		List<FinancialTransaction> content,
		int page,
		int size,
		long totalElements) {

	public FinancialTransactionPage {
		Objects.requireNonNull(content, "content must not be null");
		content = List.copyOf(content);
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1) {
			throw new IllegalArgumentException("size must be at least 1");
		}
		if (totalElements < content.size()) {
			throw new IllegalArgumentException("total elements must cover the returned content");
		}
	}

	public int totalPages() {
		return totalElements == 0 ? 0 : (int) ((totalElements - 1) / size) + 1;
	}
}
