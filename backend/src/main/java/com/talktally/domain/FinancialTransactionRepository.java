package com.talktally.domain;

import java.util.Optional;

public interface FinancialTransactionRepository {

	FinancialTransaction save(FinancialTransaction transaction);

	Optional<FinancialTransaction> findById(UserId ownerId, TransactionId transactionId);
}
