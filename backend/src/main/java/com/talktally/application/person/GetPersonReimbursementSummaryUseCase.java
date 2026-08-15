package com.talktally.application.person;

import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.person.output.PersonReimbursementSummaryOutput;
import com.talktally.domain.Money;
import com.talktally.domain.Person;
import com.talktally.domain.PersonId;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Objects;

@Service
public class GetPersonReimbursementSummaryUseCase {

	private final PersonRepository personRepository;
	private final ReimbursementClaimRepository claimRepository;

	public GetPersonReimbursementSummaryUseCase(
			PersonRepository personRepository,
			ReimbursementClaimRepository claimRepository) {
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
	}

	@Transactional(readOnly = true)
	public PersonReimbursementSummaryOutput execute(UserId actorId, PersonId personId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(personId, "person id must not be null");
		Person person = personRepository.findById(actorId, personId)
				.orElseThrow(() -> new PersonNotFoundException(personId));
		List<ReimbursementClaim> claims = claimRepository.findAllByPerson(actorId, personId);
		Currency brl = Currency.getInstance("BRL");
		Money original = Money.zero(brl);
		Money reimbursed = Money.zero(brl);
		Money outstanding = Money.zero(brl);
		long openClaims = 0;
		for (ReimbursementClaim claim : claims) {
			original = original.add(claim.originalAmount());
			reimbursed = reimbursed.add(claim.amountReimbursed());
			outstanding = outstanding.add(claim.remainingAmount());
			if (claim.status() != ReimbursementStatus.PAID) {
				openClaims++;
			}
		}
		return new PersonReimbursementSummaryOutput(
				person.id(),
				person.displayName(),
				original.amount(),
				reimbursed.amount(),
				outstanding.amount(),
				brl.getCurrencyCode(),
				openClaims);
	}
}
