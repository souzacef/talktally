package com.talktally.application.exception;

import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionKind;

public final class CategoryIncompatibleException extends InvalidTransactionInputException {

	public CategoryIncompatibleException(CategoryId categoryId, TransactionKind kind) {
		super("category " + categoryId.value() + " is incompatible with " + kind);
	}
}
