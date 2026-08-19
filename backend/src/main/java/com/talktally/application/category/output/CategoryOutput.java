package com.talktally.application.category.output;

import com.talktally.domain.CategoryAllowedKind;
import com.talktally.domain.CategoryId;

public record CategoryOutput(
		CategoryId id,
		String code,
		String displayName,
		CategoryAllowedKind allowedKind,
		boolean builtIn) {
}
