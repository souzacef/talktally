package com.talktally.domain;

import java.util.Optional;

public interface UserAccountRepository {

	UserAccount save(UserAccount account);

	Optional<UserAccount> findById(UserId userId);

	Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);

	boolean existsByNormalizedEmail(String normalizedEmail);
}
