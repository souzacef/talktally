package com.talktally.application.reimbursement.output;

import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.TransactionId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ReimbursementClaimOutput(
		ReimbursementClaimId claimId,
		TransactionId expenseTransactionId,
		ReimbursementSourceExpenseOutput sourceExpense,
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
		Objects.requireNonNull(sourceExpense, "source expense must not be null");
		payments = List.copyOf(payments);
	}
}
