package com.talktally.application.reimbursement;

import com.talktally.application.input.CreateTransactionInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.input.CreateReimbursableExpenseInput;
import com.talktally.application.reimbursement.output.CreateReimbursableExpenseOutput;
import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.application.transaction.CreateTransactionUseCase;
import com.talktally.domain.Money;
import com.talktally.domain.Person;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class CreateReimbursableExpenseUseCase {

	private final PersonRepository personRepository;
	private final ReimbursementClaimRepository claimRepository;
	private final CreateTransactionUseCase createTransactionUseCase;

	public CreateReimbursableExpenseUseCase(
			PersonRepository personRepository,
			ReimbursementClaimRepository claimRepository,
			CreateTransactionUseCase createTransactionUseCase) {
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
		this.createTransactionUseCase = Objects.requireNonNull(
				createTransactionUseCase, "create transaction use case must not be null");
	}

	@Transactional
	public CreateReimbursableExpenseOutput execute(
			UserId actorId,
			TransactionSource source,
			CreateReimbursableExpenseInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(source, "source must not be null");
		if (input == null || input.personId() == null) {
			throw invalid("person is required");
		}
		Person person = personRepository.findById(actorId, input.personId())
				.orElseThrow(() -> new PersonNotFoundException(input.personId()));
		Money expenseAmount = positiveBrl(input.amount(), "expense amount");
		Money owedAmount = input.amountOwed() == null
				? expenseAmount
				: positiveBrl(input.amountOwed(), "amount owed");
		if (owedAmount.amount().compareTo(expenseAmount.amount()) > 0) {
			throw invalid("amount owed must not exceed expense amount");
		}

		TransactionOutput expense = createTransactionUseCase.execute(
				actorId,
				source,
				new CreateTransactionInput(
						TransactionKind.EXPENSE,
						input.description(),
						input.amount(),
						input.categoryId(),
						input.eventDate(),
						input.installmentCount()));
		ReimbursementClaim claim;
		try {
			claim = ReimbursementClaim.create(
					actorId,
					expense.transactionId(),
					person.id(),
					owedAmount,
					input.note());
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidReimbursementInputException(exception.getMessage(), exception);
		}
		ReimbursementClaim saved = claimRepository.save(claim);
		ReimbursementClaimOutput claimOutput = ReimbursementOutputMapper.toOutput(saved, person);
		return new CreateReimbursableExpenseOutput(expense, claimOutput);
	}

	private static Money positiveBrl(BigDecimal amount, String field) {
		if (amount == null) {
			throw invalid(field + " is required");
		}
		try {
			Money money = Money.brl(amount);
			if (!money.isPositive()) {
				throw new IllegalArgumentException(field + " must be greater than zero");
			}
			return money;
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidReimbursementInputException("invalid " + field, exception);
		}
	}

	private static InvalidReimbursementInputException invalid(String message) {
		return new InvalidReimbursementInputException(message);
	}
}
