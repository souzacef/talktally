package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.output.ReimbursementPageOutput;

import java.util.List;

public record ReimbursementPageResponse(
		List<ReimbursementClaimResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public ReimbursementPageResponse {
		items = List.copyOf(items);
	}

	static ReimbursementPageResponse from(ReimbursementPageOutput output) {
		return new ReimbursementPageResponse(
				output.content().stream().map(ReimbursementClaimResponse::from).toList(),
				output.page(),
				output.size(),
				output.totalElements(),
				output.totalPages());
	}
}
