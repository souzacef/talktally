package com.talktally.application.exception;

import com.talktally.domain.CategoryId;

public final class CategoryUnavailableException extends InvalidTransactionInputException {

	public CategoryUnavailableException(CategoryId categoryId) {
		super("category is unavailable: " + categoryId.value());
	}
}
