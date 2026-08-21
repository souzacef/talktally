package com.talktally.application.reimbursement;

import com.talktally.application.reimbursement.exception.ReimbursementClaimNotFoundException;
import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
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
	private final ReimbursementClaimOutputAssembler outputAssembler;

	public GetReimbursementUseCase(
			ReimbursementClaimRepository claimRepository,
			ReimbursementClaimOutputAssembler outputAssembler) {
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
		this.outputAssembler = Objects.requireNonNull(
				outputAssembler, "output assembler must not be null");
	}

	@Transactional(readOnly = true)
	public ReimbursementClaimOutput execute(UserId actorId, ReimbursementClaimId claimId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(claimId, "claim id must not be null");
		ReimbursementClaim claim = claimRepository.findById(actorId, claimId)
				.orElseThrow(() -> new ReimbursementClaimNotFoundException(claimId));
		return outputAssembler.assemble(actorId, claim);
	}
}
