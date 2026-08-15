package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.exception.TransactionNotFoundException;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.TransactionId;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class DeleteTransactionUseCase {

	private final FinancialTransactionRepository transactionRepository;

	public DeleteTransactionUseCase(FinancialTransactionRepository transactionRepository) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
	}

	@Transactional
	public void execute(UserId actorId, TransactionId transactionId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (transactionId == null) {
			throw new InvalidTransactionInputException("transaction id is required");
		}

		if (!transactionRepository.deleteById(actorId, transactionId)) {
			throw new TransactionNotFoundException(transactionId);
		}
	}
}
