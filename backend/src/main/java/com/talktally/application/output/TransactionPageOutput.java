package com.talktally.application.output;

import java.util.List;
import java.util.Objects;

public record TransactionPageOutput(
		List<TransactionOutput> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public TransactionPageOutput {
		Objects.requireNonNull(content, "content must not be null");
		content = List.copyOf(content);
		if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
			throw new IllegalArgumentException("page metadata must not be negative");
		}
	}
}
