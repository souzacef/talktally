package com.talktally.application.reimbursement.input;

import com.talktally.domain.CategoryId;
import com.talktally.domain.PersonId;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReimbursableExpenseInput(
		String description,
		BigDecimal amount,
		CategoryId categoryId,
		LocalDate eventDate,
		LocalDate firstOccurrenceDate,
		int installmentCount,
		PersonId personId,
		BigDecimal amountOwed,
		String note) {
}
