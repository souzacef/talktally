package com.talktally.infrastructure.web.transaction;

import com.talktally.application.output.TransactionPageOutput;

import java.util.List;

public record TransactionPageResponse(
		List<TransactionResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public TransactionPageResponse {
		items = List.copyOf(items);
	}

	static TransactionPageResponse from(TransactionPageOutput output) {
		return new TransactionPageResponse(
				output.content().stream().map(TransactionResponse::from).toList(),
				output.page(),
				output.size(),
				output.totalElements(),
				output.totalPages());
	}
}
