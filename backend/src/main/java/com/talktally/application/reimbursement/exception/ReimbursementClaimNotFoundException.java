package com.talktally.application.reimbursement.exception;

import com.talktally.domain.ReimbursementClaimId;

public final class ReimbursementClaimNotFoundException extends RuntimeException {

	public ReimbursementClaimNotFoundException(ReimbursementClaimId claimId) {
		super("reimbursement claim not found: " + claimId.value());
	}
}
