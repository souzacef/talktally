package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.exception.TransactionNotFoundException;
import com.talktally.application.output.TransactionOutput;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.TransactionId;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GetTransactionUseCase {

	private final FinancialTransactionRepository transactionRepository;

	public GetTransactionUseCase(FinancialTransactionRepository transactionRepository) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
	}

	@Transactional(readOnly = true)
	public TransactionOutput execute(UserId actorId, TransactionId transactionId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (transactionId == null) {
			throw new InvalidTransactionInputException("transaction id is required");
		}

		FinancialTransaction transaction = transactionRepository.findById(actorId, transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
		return TransactionOutputMapper.toOutput(
				transaction,
				transactionRepository.isLinkedToReimbursement(actorId, transactionId));
	}
}
