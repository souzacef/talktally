package com.talktally.application.reimbursement.output;

import java.util.List;

public record ReimbursementPageOutput(
		List<ReimbursementClaimOutput> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public ReimbursementPageOutput {
		content = List.copyOf(content);
	}
}
