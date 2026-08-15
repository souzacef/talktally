package com.talktally.application.reimbursement;

import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.application.reimbursement.output.ReimbursementPaymentOutput;
import com.talktally.domain.Person;
import com.talktally.domain.ReimbursementClaim;

final class ReimbursementOutputMapper {

	private ReimbursementOutputMapper() {
	}

	static ReimbursementClaimOutput toOutput(ReimbursementClaim claim, Person person) {
		return new ReimbursementClaimOutput(
				claim.id(),
				claim.expenseTransactionId(),
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
