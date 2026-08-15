package com.talktally.application.input;

import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionKind;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionInput(
		TransactionKind kind,
		String description,
		BigDecimal amount,
		CategoryId categoryId,
		LocalDate eventDate,
		int installmentCount) {
}
