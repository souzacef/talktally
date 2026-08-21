package com.talktally.application.reimbursement;

import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.application.reimbursement.output.ReimbursementPaymentOutput;
import com.talktally.application.reimbursement.output.ReimbursementSourceExpenseOutput;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.Person;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.TransactionKind;

final class ReimbursementOutputMapper {

	private ReimbursementOutputMapper() {
	}

	static ReimbursementClaimOutput toOutput(
			ReimbursementClaim claim,
			Person person,
			FinancialTransaction sourceExpense) {
		if (!sourceExpense.id().equals(claim.expenseTransactionId())
				|| !sourceExpense.ownerId().equals(claim.ownerId())
				|| sourceExpense.kind() != TransactionKind.EXPENSE) {
			throw new IllegalStateException("reimbursement source expense invariant failed");
		}
		return new ReimbursementClaimOutput(
				claim.id(),
				claim.expenseTransactionId(),
				new ReimbursementSourceExpenseOutput(
						sourceExpense.id(),
						sourceExpense.description(),
						sourceExpense.totalAmount().amount(),
						sourceExpense.totalAmount().currency().getCurrencyCode(),
						sourceExpense.categoryId(),
						sourceExpense.eventDate(),
						sourceExpense.firstOccurrenceDate(),
						sourceExpense.occurrences().size()),
				person.id(),
				person.displayName(),
				claim.originalAmount().amount(),
				claim.amountReimbursed().amount(),
				claim.remainingAmount().amount(),
				claim.originalAmount().currency().getCurrencyCode(),
				claim.status(),
				claim.note(),
				claim.payments().stream()
						.map(payment -> new ReimbursementPaymentOutput(
								payment.id(),
								payment.amount().amount(),
								payment.amount().currency().getCurrencyCode(),
								payment.receivedDate(),
								payment.receiptTransactionId(),
								payment.note()))
						.toList());
	}
}
