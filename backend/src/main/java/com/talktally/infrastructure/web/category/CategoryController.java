package com.talktally.infrastructure.web.category;

import com.talktally.application.category.ListVisibleCategoriesUseCase;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final ListVisibleCategoriesUseCase listCategoriesUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public CategoryController(
			ListVisibleCategoriesUseCase listCategoriesUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.listCategoriesUseCase = Objects.requireNonNull(
				listCategoriesUseCase, "list categories use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@GetMapping
	public List<CategoryResponse> list() {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return listCategoriesUseCase.execute(actorId).stream()
				.map(CategoryResponse::from)
				.toList();
	}
}
