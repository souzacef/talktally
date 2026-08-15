package com.talktally.infrastructure.security;

import com.talktally.domain.UserId;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityContextAuthenticatedUserProvider implements AuthenticatedUserProvider {

	@Override
	public UserId currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
				|| !authentication.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException(
					"authenticated user is unavailable");
		}

		String subject = jwtAuthentication.getToken().getSubject();
		try {
			return UserId.from(UUID.fromString(subject));
		}
		catch (IllegalArgumentException | NullPointerException exception) {
			throw new BadCredentialsException("authenticated user identity is invalid", exception);
		}
	}
}
