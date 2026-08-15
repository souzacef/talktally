package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryId;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.CategoryJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.CategoryEntityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public class JpaCategoryCatalogAdapter implements CategoryCatalog {

	private final CategoryEntityRepository categoryRepository;

	public JpaCategoryCatalogAdapter(CategoryEntityRepository categoryRepository) {
		this.categoryRepository = Objects.requireNonNull(
				categoryRepository, "category repository must not be null");
	}

	@Override
	public Optional<CategoryMetadata> findVisibleById(UserId ownerId, CategoryId categoryId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(categoryId, "category id must not be null");

		return categoryRepository.findVisibleById(ownerId.value(), categoryId.value())
				.map(JpaCategoryCatalogAdapter::toDomain);
	}

	private static CategoryMetadata toDomain(CategoryJpaEntity entity) {
		return new CategoryMetadata(
				CategoryId.from(entity.getId()),
				allowedKinds(entity.getAllowedKind()));
	}

	private static Set<TransactionKind> allowedKinds(String allowedKind) {
		if ("ANY".equals(allowedKind)) {
			return EnumSet.allOf(TransactionKind.class);
		}
		try {
			return EnumSet.of(TransactionKind.valueOf(allowedKind));
		}
		catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalStateException("unsupported category allowed_kind", exception);
		}
	}
}
