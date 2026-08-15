package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.exception.TransactionNotFoundException;
import com.talktally.application.input.UpdateTransactionInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.TransactionId;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UpdateTransactionUseCase {

	private final FinancialTransactionRepository transactionRepository;
	private final TransactionInputValidator validator;

	public UpdateTransactionUseCase(
			FinancialTransactionRepository transactionRepository,
			CategoryCatalog categoryCatalog) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
		this.validator = new TransactionInputValidator(categoryCatalog);
	}

	@Transactional
	public TransactionOutput execute(
			UserId actorId,
			TransactionId transactionId,
			UpdateTransactionInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (transactionId == null) {
			throw new InvalidTransactionInputException("transaction id is required");
		}
		if (input == null) {
			throw new InvalidTransactionInputException("transaction input is required");
		}

		FinancialTransaction existing = transactionRepository.findById(actorId, transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
		ValidatedTransactionInput validated = validator.validate(
				actorId,
				input.kind(),
				input.description(),
				input.amount(),
				input.categoryId(),
				input.eventDate(),
				input.installmentCount());
		FinancialTransaction replacement = TransactionAggregateFactory.replace(existing, validated);

		return TransactionOutputMapper.toOutput(transactionRepository.save(replacement));
	}
}
