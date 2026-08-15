package com.talktally.application.category;

import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class FindVisibleCategoryByCodeUseCase {

	private final CategoryCatalog categoryCatalog;

	public FindVisibleCategoryByCodeUseCase(CategoryCatalog categoryCatalog) {
		this.categoryCatalog = Objects.requireNonNull(
				categoryCatalog, "category catalog must not be null");
	}

	@Transactional(readOnly = true)
	public CategoryMetadata execute(UserId actorId, String code) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (code == null || code.isBlank()) {
			throw new CategoryCodeNotFoundException("<blank>");
		}
		String normalizedCode = code.strip().toUpperCase(Locale.ROOT);
		return categoryCatalog.findBuiltInByCode(normalizedCode)
				.orElseThrow(() -> new CategoryCodeNotFoundException(normalizedCode));
	}
}
