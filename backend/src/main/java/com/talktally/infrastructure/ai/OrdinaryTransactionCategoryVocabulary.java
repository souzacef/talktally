package com.talktally.infrastructure.ai;

import java.util.List;

final class OrdinaryTransactionCategoryVocabulary {

	static final String SALARY = "SALARY";
	static final String FREELANCE = "FREELANCE";
	static final String FOOD_DINING = "FOOD_DINING";
	static final String GROCERIES = "GROCERIES";
	static final String HOUSING = "HOUSING";
	static final String UTILITIES = "UTILITIES";
	static final String TRANSPORT = "TRANSPORT";
	static final String HEALTH = "HEALTH";
	static final String EDUCATION = "EDUCATION";
	static final String ENTERTAINMENT = "ENTERTAINMENT";
	static final String SHOPPING = "SHOPPING";
	static final String TRAVEL = "TRAVEL";
	static final String TAXES_FEES = "TAXES_FEES";
	static final String OTHER = "OTHER";

	static final String TOOL_PARAMETER_DESCRIPTION =
			"Stable ordinary category code. Allowed values: "
			+ SALARY + ", "
			+ FREELANCE + ", "
			+ FOOD_DINING + ", "
			+ GROCERIES + ", "
			+ HOUSING + ", "
			+ UTILITIES + ", "
			+ TRANSPORT + ", "
			+ HEALTH + ", "
			+ EDUCATION + ", "
			+ ENTERTAINMENT + ", "
			+ SHOPPING + ", "
			+ TRAVEL + ", "
			+ TAXES_FEES + ", or "
			+ OTHER
			+ ". Preserve an explicitly named valid category; otherwise infer semantically. "
			+ "Omit only when genuinely uncertain, which defaults to OTHER. Never use REIMBURSEMENT.";

	private static final List<CategoryGuidance> CATEGORIES = List.of(
			new CategoryGuidance(SALARY, "INCOME only", "salary, wages, and regular employment pay"),
			new CategoryGuidance(FREELANCE, "INCOME only", "freelance, contract, and independent work earnings"),
			new CategoryGuidance(FOOD_DINING, "EXPENSE only", "food and dining; restaurants, dinner, lunch, takeout, and coffee at a cafe"),
			new CategoryGuidance(GROCERIES, "EXPENSE only", "supermarkets, grocery stores, and food or household staples bought for home"),
			new CategoryGuidance(HOUSING, "EXPENSE only", "rent, mortgage, and other housing costs"),
			new CategoryGuidance(UTILITIES, "EXPENSE only", "electricity, water, gas, internet, and phone service"),
			new CategoryGuidance(TRANSPORT, "EXPENSE only", "public transit, rides, fuel, parking, and other transport"),
			new CategoryGuidance(HEALTH, "EXPENSE only", "medical, dental, pharmacy, and other health costs"),
			new CategoryGuidance(EDUCATION, "EXPENSE only", "tuition, courses, books, and other education costs"),
			new CategoryGuidance(ENTERTAINMENT, "EXPENSE only", "movies, games, events, and entertainment subscriptions"),
			new CategoryGuidance(SHOPPING, "EXPENSE only", "retail purchases such as clothing, electronics, and household goods"),
			new CategoryGuidance(TRAVEL, "EXPENSE only", "flights, lodging, and other trip costs"),
			new CategoryGuidance(TAXES_FEES, "EXPENSE only", "taxes and government, banking, or service fees"),
			new CategoryGuidance(OTHER, "INCOME or EXPENSE", "fallback only when no more specific category is reasonably supported"));

	private OrdinaryTransactionCategoryVocabulary() {
	}

	static List<String> codes() {
		return CATEGORIES.stream().map(CategoryGuidance::code).toList();
	}

	static String systemPromptGuidance() {
		String categoryLines = CATEGORIES.stream()
				.map(category -> "- %s (%s): %s."
						.formatted(category.code(), category.allowedKind(), category.meaning()))
				.collect(java.util.stream.Collectors.joining("\n"));
		return """
				For record_transaction, use this authoritative ordinary-transaction category vocabulary:
				%s

				Category-selection rules:
				- If the user explicitly names a valid category, that category wins. Do not substitute a nearby category.
				- Otherwise infer semantically from the description. Dinner, restaurant meals, lunch, and coffee at a cafe are FOOD_DINING; supermarket and grocery-store purchases are GROCERIES.
				- Use OTHER without unnecessary clarification only when the category is genuinely uncertain.
				- REIMBURSEMENT is reserved for reimbursement receipts and is never selectable by record_transaction.
				- Category availability and kind compatibility are always decided by TalkTally's application validation.
				""".formatted(categoryLines).strip();
	}

	private record CategoryGuidance(String code, String allowedKind, String meaning) {
	}
}
