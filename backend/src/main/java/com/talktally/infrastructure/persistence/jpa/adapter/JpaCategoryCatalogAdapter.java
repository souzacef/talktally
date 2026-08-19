package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.domain.CategoryAllowedKind;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryDefinition;
import com.talktally.domain.CategoryId;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.CategoryJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.CategoryEntityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
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
					.map(JpaCategoryCatalogAdapter::toMetadata);
	}

	@Override
	public List<CategoryDefinition> findAllVisible(UserId ownerId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		return categoryRepository.findAllVisible(ownerId.value()).stream()
				.map(JpaCategoryCatalogAdapter::toDefinition)
				.toList();
	}

	@Override
	public Optional<CategoryMetadata> findBuiltInByCode(String code) {
		Objects.requireNonNull(code, "code must not be null");
		return categoryRepository.findByCodeAndOwnerUserIdIsNull(code)
				.map(JpaCategoryCatalogAdapter::toMetadata);
	}

	private static CategoryMetadata toMetadata(CategoryJpaEntity entity) {
		return new CategoryMetadata(
				CategoryId.from(entity.getId()),
				allowedKinds(entity.getAllowedKind()));
	}

	private static CategoryDefinition toDefinition(CategoryJpaEntity entity) {
		return new CategoryDefinition(
				CategoryId.from(entity.getId()),
				entity.getCode(),
				entity.getDisplayName(),
				allowedKind(entity.getAllowedKind()),
				entity.isBuiltIn());
	}

	private static Set<TransactionKind> allowedKinds(String allowedKind) {
		CategoryAllowedKind parsed = allowedKind(allowedKind);
		if (parsed == CategoryAllowedKind.ANY) {
			return EnumSet.allOf(TransactionKind.class);
		}
		return EnumSet.of(TransactionKind.valueOf(parsed.name()));
	}

	private static CategoryAllowedKind allowedKind(String allowedKind) {
		try {
			return CategoryAllowedKind.valueOf(allowedKind);
		}
		catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalStateException("unsupported category allowed_kind", exception);
		}
	}
}
