package com.talktally.application.reimbursement.output;

import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.TransactionId;

import java.math.BigDecimal;
import java.util.List;

public record ReimbursementClaimOutput(
		ReimbursementClaimId claimId,
		TransactionId expenseTransactionId,
		PersonId personId,
		String personDisplayName,
		BigDecimal originalAmount,
		BigDecimal amountReimbursed,
		BigDecimal remainingAmount,
		String currency,
		ReimbursementStatus status,
		String note,
		List<ReimbursementPaymentOutput> payments) {

	public ReimbursementClaimOutput {
		payments = List.copyOf(payments);
	}
}
