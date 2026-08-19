package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.input.CreateReimbursableExpenseInput;
import com.talktally.application.transaction.TransactionPolicy;
import com.talktally.domain.CategoryId;
import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementPayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateReimbursementRequest(
		@NotBlank @Size(max = TransactionPolicy.MAX_DESCRIPTION_LENGTH) String description,
		@NotNull @DecimalMin(value = "0.00", inclusive = false)
		@Digits(integer = 17, fraction = 2) BigDecimal amount,
		@NotNull UUID categoryId,
		@NotNull LocalDate eventDate,
		LocalDate firstOccurrenceDate,
		@Min(1) @Max(TransactionPolicy.MAX_INSTALLMENTS) int installmentCount,
		@NotNull UUID personId,
		@DecimalMin(value = "0.00", inclusive = false)
		@Digits(integer = 17, fraction = 2) BigDecimal amountOwed,
		@Size(max = ReimbursementPayment.MAX_NOTE_LENGTH) String note) {

	CreateReimbursableExpenseInput toInput() {
		return new CreateReimbursableExpenseInput(
				description,
				amount,
				categoryId == null ? null : CategoryId.from(categoryId),
				eventDate,
				firstOccurrenceDate,
				installmentCount,
				personId == null ? null : PersonId.from(personId),
				amountOwed,
				note);
	}
}
