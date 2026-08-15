package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.output.CreateReimbursableExpenseOutput;
import com.talktally.infrastructure.web.transaction.TransactionResponse;

public record CreateReimbursementResponse(
		TransactionResponse expense,
		ReimbursementClaimResponse claim) {

	static CreateReimbursementResponse from(CreateReimbursableExpenseOutput output) {
		return new CreateReimbursementResponse(
				TransactionResponse.from(output.expense()),
				ReimbursementClaimResponse.from(output.claim()));
	}
}
