package com.talktally.application.reimbursement;

public final class NoOpenReimbursementClaimException extends RuntimeException {

	public NoOpenReimbursementClaimException() {
		super("no open reimbursement claim found");
	}
}
