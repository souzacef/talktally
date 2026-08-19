package com.talktally.application.transaction;

import com.talktally.domain.CategoryId;
import com.talktally.domain.Money;
import com.talktally.domain.TransactionKind;

import java.time.LocalDate;

record ValidatedTransactionInput(
		TransactionKind kind,
		String description,
		Money amount,
		CategoryId categoryId,
		LocalDate eventDate,
		LocalDate firstOccurrenceDate,
		int installmentCount) {
}
