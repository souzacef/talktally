package com.talktally.domain;

import java.util.List;
import java.util.Objects;

public record ReimbursementClaimPage(
		List<ReimbursementClaim> content,
		int page,
		int size,
		long totalElements) {

	public ReimbursementClaimPage {
		Objects.requireNonNull(content, "content must not be null");
		content = List.copyOf(content);
		if (page < 0 || size < 1 || totalElements < content.size()) {
			throw new IllegalArgumentException("invalid reimbursement page metadata");
		}
	}

	public int totalPages() {
		return totalElements == 0 ? 0 : (int) ((totalElements - 1) / size) + 1;
	}
}
