package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.domain.ReimbursementStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReimbursementClaimResponse(
		UUID id,
		UUID expenseTransactionId,
		ReimbursementSourceExpenseResponse sourceExpense,
		UUID personId,
		String personDisplayName,
		BigDecimal originalAmount,
		BigDecimal amountReimbursed,
		BigDecimal remainingAmount,
		String currency,
		ReimbursementStatus status,
		String note,
		List<ReimbursementPaymentResponse> payments) {

	public ReimbursementClaimResponse {
		payments = List.copyOf(payments);
	}

	static ReimbursementClaimResponse from(ReimbursementClaimOutput output) {
		return new ReimbursementClaimResponse(
				output.claimId().value(),
				output.expenseTransactionId().value(),
				ReimbursementSourceExpenseResponse.from(output.sourceExpense()),
				output.personId().value(),
				output.personDisplayName(),
				output.originalAmount(),
				output.amountReimbursed(),
				output.remainingAmount(),
				output.currency(),
				output.status(),
				output.note(),
				output.payments().stream()
						.map(ReimbursementPaymentResponse::from)
						.toList());
	}
}
