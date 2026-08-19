package com.talktally.infrastructure.web.category;

import com.talktally.application.category.output.CategoryOutput;

import java.util.UUID;

public record CategoryResponse(
		UUID id,
		String code,
		String displayName,
		String allowedKind,
		boolean builtIn) {

	static CategoryResponse from(CategoryOutput output) {
		return new CategoryResponse(
				output.id().value(),
				output.code(),
				output.displayName(),
				output.allowedKind().name(),
				output.builtIn());
	}
}
