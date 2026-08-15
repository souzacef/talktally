package com.talktally.application.auth.port;

import com.talktally.domain.UserId;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(UserId userId);
}
