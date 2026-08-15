package com.talktally.application.transaction;

import com.talktally.application.exception.CategoryIncompatibleException;
import com.talktally.application.exception.CategoryUnavailableException;
import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryId;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.Money;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

final class TransactionInputValidator {

	private final CategoryCatalog categoryCatalog;

	TransactionInputValidator(CategoryCatalog categoryCatalog) {
		this.categoryCatalog = Objects.requireNonNull(categoryCatalog, "category catalog must not be null");
	}

	ValidatedTransactionInput validate(
			UserId actorId,
			TransactionKind kind,
			String description,
			BigDecimal amount,
			CategoryId categoryId,
			LocalDate eventDate,
			int installmentCount) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (kind == null) {
			throw invalid("kind is required");
		}
		TransactionPolicy.requireUserManagedKind(kind);
		if (description == null || description.isBlank()) {
			throw invalid("description must not be blank");
		}
		if (description.strip().length() > TransactionPolicy.MAX_DESCRIPTION_LENGTH) {
			throw invalid("description must not exceed "
					+ TransactionPolicy.MAX_DESCRIPTION_LENGTH + " characters");
		}
		if (amount == null) {
			throw invalid("amount is required");
		}
		if (categoryId == null) {
			throw invalid("category is required");
		}
		if (eventDate == null) {
			throw invalid("event date is required");
		}
		if (installmentCount < 1 || installmentCount > TransactionPolicy.MAX_INSTALLMENTS) {
			throw invalid("installment count must be between 1 and "
					+ TransactionPolicy.MAX_INSTALLMENTS);
		}

		Money money;
		try {
			money = Money.brl(amount);
			if (!money.isPositive()) {
				throw new IllegalArgumentException("amount must be greater than zero");
			}
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidTransactionInputException("invalid amount", exception);
		}

		CategoryMetadata category = requireVisibleCategory(actorId, categoryId);
		if (!category.allows(kind)) {
			throw new CategoryIncompatibleException(categoryId, kind);
		}

		return new ValidatedTransactionInput(
				kind, description.strip(), money, categoryId, eventDate, installmentCount);
	}

	CategoryMetadata requireVisibleCategory(UserId actorId, CategoryId categoryId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (categoryId == null) {
			throw invalid("category is required");
		}
		return categoryCatalog.findVisibleById(actorId, categoryId)
				.orElseThrow(() -> new CategoryUnavailableException(categoryId));
	}

	private static InvalidTransactionInputException invalid(String message) {
		return new InvalidTransactionInputException(message);
	}
}
