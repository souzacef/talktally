package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.input.CreateTransactionInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CreateTransactionUseCase {

	private final FinancialTransactionRepository transactionRepository;
	private final TransactionInputValidator validator;

	public CreateTransactionUseCase(
			FinancialTransactionRepository transactionRepository,
			CategoryCatalog categoryCatalog) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
		this.validator = new TransactionInputValidator(categoryCatalog);
	}

	@Transactional
	public TransactionOutput execute(
			UserId actorId,
			TransactionSource source,
			CreateTransactionInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(source, "source must not be null");
		if (input == null) {
			throw new InvalidTransactionInputException("transaction input is required");
		}

		ValidatedTransactionInput validated = validator.validate(
				actorId,
				input.kind(),
				input.description(),
				input.amount(),
				input.categoryId(),
				input.eventDate(),
				input.firstOccurrenceDate(),
				input.installmentCount());
		FinancialTransaction transaction = TransactionAggregateFactory.create(
				actorId, source, validated);

		return TransactionOutputMapper.toOutput(
				transactionRepository.save(transaction),
				false);
	}
}
