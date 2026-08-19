package com.talktally.application.category;

import com.talktally.application.category.output.CategoryOutput;
import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryDefinition;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ListVisibleCategoriesUseCase {

	private final CategoryCatalog categoryCatalog;

	public ListVisibleCategoriesUseCase(CategoryCatalog categoryCatalog) {
		this.categoryCatalog = Objects.requireNonNull(
				categoryCatalog, "category catalog must not be null");
	}

	@Transactional(readOnly = true)
	public List<CategoryOutput> execute(UserId actorId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		return categoryCatalog.findAllVisible(actorId).stream()
				.map(ListVisibleCategoriesUseCase::toOutput)
				.toList();
	}

	private static CategoryOutput toOutput(CategoryDefinition category) {
		return new CategoryOutput(
				category.id(),
				category.code(),
				category.displayName(),
				category.allowedKind(),
				category.builtIn());
	}
}
