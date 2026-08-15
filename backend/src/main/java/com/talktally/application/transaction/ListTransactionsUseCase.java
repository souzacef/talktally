package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.input.ListTransactionsInput;
import com.talktally.application.output.TransactionPageOutput;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.FinancialTransactionPage;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.FinancialTransactionSearchCriteria;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class ListTransactionsUseCase {

	private final FinancialTransactionRepository transactionRepository;
	private final TransactionInputValidator validator;

	public ListTransactionsUseCase(
			FinancialTransactionRepository transactionRepository,
			CategoryCatalog categoryCatalog) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
		this.validator = new TransactionInputValidator(categoryCatalog);
	}

	@Transactional(readOnly = true)
	public TransactionPageOutput execute(UserId actorId, ListTransactionsInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (input == null) {
			throw invalid("list input is required");
		}
		if (input.page() < 0) {
			throw invalid("page must not be negative");
		}
		if (input.size() < 1 || input.size() > TransactionPolicy.MAX_PAGE_SIZE) {
			throw invalid("size must be between 1 and " + TransactionPolicy.MAX_PAGE_SIZE);
		}
		if (input.effectiveDateFrom() != null
				&& input.effectiveDateTo() != null
				&& input.effectiveDateFrom().isAfter(input.effectiveDateTo())) {
			throw invalid("effective date from must not be after effective date to");
		}
		if (input.categoryId() != null) {
			validator.requireVisibleCategory(actorId, input.categoryId());
		}

		Optional<String> searchText = Optional.ofNullable(input.searchText())
				.map(String::strip)
				.filter(value -> !value.isEmpty());
		FinancialTransactionSearchCriteria criteria = new FinancialTransactionSearchCriteria(
				Optional.ofNullable(input.kind()),
				Optional.ofNullable(input.categoryId()),
				Optional.ofNullable(input.effectiveDateFrom()),
				Optional.ofNullable(input.effectiveDateTo()),
				searchText,
				input.page(),
				input.size());
		FinancialTransactionPage result = transactionRepository.search(actorId, criteria);

		return new TransactionPageOutput(
				result.content().stream().map(TransactionOutputMapper::toOutput).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages());
	}

	private static InvalidTransactionInputException invalid(String message) {
		return new InvalidTransactionInputException(message);
	}
}
