package com.talktally.application.reimbursement.output;

import com.talktally.domain.ReimbursementPaymentId;
import com.talktally.domain.TransactionId;

public record RecordReimbursementPaymentOutput(
		ReimbursementPaymentId paymentId,
		TransactionId receiptTransactionId,
		ReimbursementClaimOutput claim) {
}
