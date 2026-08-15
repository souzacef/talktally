package com.talktally.application.input;

import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionKind;

import java.time.LocalDate;

public record ListTransactionsInput(
		TransactionKind kind,
		CategoryId categoryId,
		LocalDate effectiveDateFrom,
		LocalDate effectiveDateTo,
		String searchText,
		int page,
		int size) {
}
