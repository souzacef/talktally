package com.talktally.application.reimbursement.output;

import com.talktally.application.output.TransactionOutput;

public record CreateReimbursableExpenseOutput(
		TransactionOutput expense,
		ReimbursementClaimOutput claim) {
}
