package com.talktally.application.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.domain.TransactionKind;

public final class TransactionPolicy {

	public static final int MAX_INSTALLMENTS = 120;
	public static final int MAX_PAGE_SIZE = 100;
	public static final int MAX_DESCRIPTION_LENGTH = 500;

	private TransactionPolicy() {
	}

	public static void requireUserManagedKind(TransactionKind kind) {
		if (kind == TransactionKind.REIMBURSEMENT_RECEIPT) {
			throw new InvalidTransactionInputException(
					"reimbursement receipts can only be created by the reimbursement workflow");
		}
	}
}
