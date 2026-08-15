package com.talktally.application.reimbursement;

import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.reimbursement.exception.ReimbursementClaimNotFoundException;
import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.domain.Person;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GetReimbursementUseCase {

	private final ReimbursementClaimRepository claimRepository;
	private final PersonRepository personRepository;

	public GetReimbursementUseCase(
			ReimbursementClaimRepository claimRepository,
			PersonRepository personRepository) {
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
	}

	@Transactional(readOnly = true)
	public ReimbursementClaimOutput execute(UserId actorId, ReimbursementClaimId claimId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(claimId, "claim id must not be null");
		ReimbursementClaim claim = claimRepository.findById(actorId, claimId)
				.orElseThrow(() -> new ReimbursementClaimNotFoundException(claimId));
		Person person = personRepository.findById(actorId, claim.personId())
				.orElseThrow(() -> new PersonNotFoundException(claim.personId()));
		return ReimbursementOutputMapper.toOutput(claim, person);
	}
}
