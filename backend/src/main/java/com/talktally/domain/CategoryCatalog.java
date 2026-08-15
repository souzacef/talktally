package com.talktally.domain;

import java.util.Optional;

public interface CategoryCatalog {

	Optional<CategoryMetadata> findVisibleById(UserId ownerId, CategoryId categoryId);
}
