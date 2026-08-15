package com.talktally.infrastructure.security;

import com.talktally.domain.UserId;

public interface AuthenticatedUserProvider {

	UserId currentUserId();
}
