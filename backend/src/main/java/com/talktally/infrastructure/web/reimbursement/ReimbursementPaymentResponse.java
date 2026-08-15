package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.output.ReimbursementPaymentOutput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReimbursementPaymentResponse(
		UUID id,
		BigDecimal amount,
		String currency,
		LocalDate receivedDate,
		UUID receiptTransactionId,
		String note) {

	static ReimbursementPaymentResponse from(ReimbursementPaymentOutput output) {
		return new ReimbursementPaymentResponse(
				output.paymentId().value(),
				output.amount(),
				output.currency(),
				output.receivedDate(),
				output.receiptTransactionId().value(),
				output.note());
	}
}
