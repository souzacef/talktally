package com.talktally.domain;

import java.util.List;
import java.util.Optional;

public interface CategoryCatalog {

	Optional<CategoryMetadata> findVisibleById(UserId ownerId, CategoryId categoryId);

	List<CategoryDefinition> findAllVisible(UserId ownerId);

	default Optional<CategoryMetadata> findBuiltInByCode(String code) {
		return Optional.empty();
	}
}
