package com.talktally.application.reimbursement;

import com.talktally.application.reimbursement.exception.AmbiguousReimbursementClaimException;
import com.talktally.application.reimbursement.exception.ReimbursementClaimNotFoundException;
import com.talktally.application.reimbursement.input.ListReimbursementsInput;
import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;
import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ResolveOpenReimbursementClaimUseCase {

	private static final int MAX_OPEN_CLAIMS = 100;

	private final GetReimbursementUseCase getReimbursementUseCase;
	private final ListReimbursementsUseCase listReimbursementsUseCase;

	public ResolveOpenReimbursementClaimUseCase(
			GetReimbursementUseCase getReimbursementUseCase,
			ListReimbursementsUseCase listReimbursementsUseCase) {
		this.getReimbursementUseCase = Objects.requireNonNull(
				getReimbursementUseCase, "get reimbursement use case must not be null");
		this.listReimbursementsUseCase = Objects.requireNonNull(
				listReimbursementsUseCase, "list reimbursements use case must not be null");
	}

	@Transactional(readOnly = true)
	public ReimbursementClaimOutput execute(
			UserId actorId,
			PersonId personId,
			ReimbursementClaimId explicitClaimId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(personId, "person id must not be null");
		if (explicitClaimId != null) {
			ReimbursementClaimOutput claim = getReimbursementUseCase.execute(actorId, explicitClaimId);
			if (!claim.personId().equals(personId) || claim.status() == ReimbursementStatus.PAID) {
				throw new ReimbursementClaimNotFoundException(explicitClaimId);
			}
			return claim;
		}

		List<ReimbursementClaimOutput> openClaims = new ArrayList<>();
		openClaims.addAll(list(actorId, personId, ReimbursementStatus.PENDING));
		openClaims.addAll(list(actorId, personId, ReimbursementStatus.PARTIALLY_PAID));
		if (openClaims.isEmpty()) {
			throw new NoOpenReimbursementClaimException();
		}
		if (openClaims.size() > 1) {
			throw new AmbiguousReimbursementClaimException(openClaims);
		}
		return openClaims.getFirst();
	}

	private List<ReimbursementClaimOutput> list(
			UserId actorId,
			PersonId personId,
			ReimbursementStatus status) {
		return listReimbursementsUseCase.execute(
				actorId,
				new ListReimbursementsInput(personId, status, 0, MAX_OPEN_CLAIMS)).content();
	}
}
