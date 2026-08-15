package com.talktally.application.auth;

import com.talktally.application.auth.output.UserAccountOutput;
import com.talktally.domain.UserAccount;

final class UserAccountOutputMapper {

	private UserAccountOutputMapper() {
	}

	static UserAccountOutput toOutput(UserAccount account) {
		return new UserAccountOutput(
				account.id(),
				account.normalizedEmail(),
				account.displayName(),
				account.defaultCurrency().getCurrencyCode());
	}
}
