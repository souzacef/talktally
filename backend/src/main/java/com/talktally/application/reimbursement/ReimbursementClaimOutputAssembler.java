package com.talktally.application.reimbursement;

import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.Person;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
final class ReimbursementClaimOutputAssembler {

	private final PersonRepository personRepository;
	private final FinancialTransactionRepository transactionRepository;

	ReimbursementClaimOutputAssembler(
			PersonRepository personRepository,
			FinancialTransactionRepository transactionRepository) {
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository, "transaction repository must not be null");
	}

	ReimbursementClaimOutput assemble(UserId actorId, ReimbursementClaim claim) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(claim, "claim must not be null");
		Person person = personRepository.findById(actorId, claim.personId())
				.orElseThrow(() -> new PersonNotFoundException(claim.personId()));
		return assemble(actorId, claim, person);
	}

	ReimbursementClaimOutput assemble(
			UserId actorId,
			ReimbursementClaim claim,
			Person person) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(claim, "claim must not be null");
		Objects.requireNonNull(person, "person must not be null");
		if (!claim.ownerId().equals(actorId)
				|| !person.ownerId().equals(actorId)
				|| !person.id().equals(claim.personId())) {
			throw new IllegalStateException("reimbursement ownership invariant failed");
		}
		FinancialTransaction sourceExpense = transactionRepository
				.findById(actorId, claim.expenseTransactionId())
				.orElseThrow(() -> new IllegalStateException(
						"reimbursement source expense is unavailable"));
		return ReimbursementOutputMapper.toOutput(claim, person, sourceExpense);
	}
}
