package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.output.RecordReimbursementPaymentOutput;

import java.util.UUID;

public record RecordReimbursementPaymentResponse(
		UUID paymentId,
		UUID receiptTransactionId,
		ReimbursementClaimResponse claim) {

	static RecordReimbursementPaymentResponse from(RecordReimbursementPaymentOutput output) {
		return new RecordReimbursementPaymentResponse(
				output.paymentId().value(),
				output.receiptTransactionId().value(),
				ReimbursementClaimResponse.from(output.claim()));
	}
}
