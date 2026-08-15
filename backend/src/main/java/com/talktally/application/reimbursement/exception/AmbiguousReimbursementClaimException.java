package com.talktally.application.reimbursement.exception;

import com.talktally.application.reimbursement.output.ReimbursementClaimOutput;

import java.util.List;

public final class AmbiguousReimbursementClaimException extends RuntimeException {

	private final List<ReimbursementClaimOutput> candidates;

	public AmbiguousReimbursementClaimException(List<ReimbursementClaimOutput> candidates) {
		super("multiple open reimbursement claims require clarification");
		this.candidates = List.copyOf(candidates);
	}

	public List<ReimbursementClaimOutput> candidates() {
		return candidates;
	}
}
