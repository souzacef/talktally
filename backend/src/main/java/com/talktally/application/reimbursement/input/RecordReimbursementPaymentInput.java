package com.talktally.application.reimbursement.input;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordReimbursementPaymentInput(
		BigDecimal amount,
		LocalDate receivedDate,
		String note) {
}
