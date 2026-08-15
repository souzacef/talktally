package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.input.RecordReimbursementPaymentInput;
import com.talktally.domain.ReimbursementPayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReimbursementPaymentRequest(
		@NotNull @DecimalMin(value = "0.00", inclusive = false)
		@Digits(integer = 17, fraction = 2) BigDecimal amount,
		@NotNull LocalDate receivedDate,
		@Size(max = ReimbursementPayment.MAX_NOTE_LENGTH) String note) {

	RecordReimbursementPaymentInput toInput() {
		return new RecordReimbursementPaymentInput(amount, receivedDate, note);
	}
}
