package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.InstallmentSchedule;
import com.talktally.domain.TransactionOccurrence;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

import java.util.List;

final class TransactionAggregateFactory {

	private TransactionAggregateFactory() {
	}

	static FinancialTransaction create(
			UserId actorId,
			TransactionSource source,
			ValidatedTransactionInput input) {
		try {
			if (input.installmentCount() == 1) {
				return FinancialTransaction.createSingleOccurrence(
						actorId,
						input.kind(),
						input.description(),
						input.amount(),
						input.categoryId(),
						input.eventDate(),
						source);
			}
			return FinancialTransaction.createInstallment(
					actorId,
					input.kind(),
					input.description(),
					input.amount(),
					input.categoryId(),
					input.eventDate(),
					source,
					input.installmentCount(),
					input.eventDate());
		}
		catch (IllegalArgumentException exception) {
			throw invalidFinancialRules(exception);
		}
	}

	static FinancialTransaction replace(
			FinancialTransaction existing,
			ValidatedTransactionInput input) {
		try {
			List<TransactionOccurrence> occurrences = input.installmentCount() == 1
					? List.of(new TransactionOccurrence(1, input.eventDate(), input.amount()))
					: InstallmentSchedule.allocate(
							input.amount(), input.installmentCount(), input.eventDate());

			return FinancialTransaction.reconstruct(
					existing.id(),
					existing.ownerId(),
					input.kind(),
					input.description(),
					input.amount(),
					input.categoryId(),
					input.eventDate(),
					existing.source(),
					occurrences);
		}
		catch (IllegalArgumentException exception) {
			throw invalidFinancialRules(exception);
		}
	}

	private static InvalidTransactionInputException invalidFinancialRules(Exception cause) {
		return new InvalidTransactionInputException(
				"transaction input violates financial rules", cause);
	}
}
