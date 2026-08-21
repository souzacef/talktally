package com.talktally.application.reimbursement;

import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.input.ListReimbursementsInput;
import com.talktally.application.reimbursement.output.ReimbursementPageOutput;
import com.talktally.application.transaction.TransactionPolicy;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaimPage;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementClaimSearchCriteria;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class ListReimbursementsUseCase {

	private final ReimbursementClaimRepository claimRepository;
	private final PersonRepository personRepository;
	private final ReimbursementClaimOutputAssembler outputAssembler;

	public ListReimbursementsUseCase(
			ReimbursementClaimRepository claimRepository,
			PersonRepository personRepository,
			ReimbursementClaimOutputAssembler outputAssembler) {
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
		this.outputAssembler = Objects.requireNonNull(
				outputAssembler, "output assembler must not be null");
	}

	@Transactional(readOnly = true)
	public ReimbursementPageOutput execute(UserId actorId, ListReimbursementsInput input) {
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
		if (input.personId() != null
				&& personRepository.findById(actorId, input.personId()).isEmpty()) {
			throw new PersonNotFoundException(input.personId());
		}
		ReimbursementClaimPage page = claimRepository.search(
				actorId,
				new ReimbursementClaimSearchCriteria(
						Optional.ofNullable(input.personId()),
						Optional.ofNullable(input.status()),
						input.page(),
						input.size()));
		return new ReimbursementPageOutput(
				page.content().stream()
						.map(claim -> outputAssembler.assemble(actorId, claim))
						.toList(),
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages());
	}

	private static InvalidReimbursementInputException invalid(String message) {
		return new InvalidReimbursementInputException(message);
	}
}
