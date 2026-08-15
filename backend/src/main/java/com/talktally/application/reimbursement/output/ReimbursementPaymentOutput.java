package com.talktally.application.reimbursement.output;

import com.talktally.domain.ReimbursementPaymentId;
import com.talktally.domain.TransactionId;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReimbursementPaymentOutput(
		ReimbursementPaymentId paymentId,
		BigDecimal amount,
		String currency,
		LocalDate receivedDate,
		TransactionId receiptTransactionId,
		String note) {
}
