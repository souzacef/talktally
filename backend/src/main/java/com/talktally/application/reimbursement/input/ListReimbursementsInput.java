package com.talktally.application.reimbursement.input;

import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementStatus;

public record ListReimbursementsInput(
		PersonId personId,
		ReimbursementStatus status,
		int page,
		int size) {
}
