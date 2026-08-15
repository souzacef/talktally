package com.talktally.infrastructure.ai;

import com.talktally.application.category.CategoryCodeNotFoundException;
import com.talktally.application.category.FindVisibleCategoryByCodeUseCase;
import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.input.CreateTransactionInput;
import com.talktally.application.input.ListTransactionsInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.application.output.TransactionPageOutput;
import com.talktally.application.transaction.CreateTransactionUseCase;
import com.talktally.application.transaction.ListTransactionsUseCase;
import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class TransactionAssistantTools {

	private static final String DEFAULT_CATEGORY_CODE = "OTHER";
	private static final int DEFAULT_PAGE_SIZE = 20;

	private final CreateTransactionUseCase createTransactionUseCase;
	private final ListTransactionsUseCase listTransactionsUseCase;
	private final FindVisibleCategoryByCodeUseCase findCategoryByCodeUseCase;
	private final Clock clock;

	public TransactionAssistantTools(
			CreateTransactionUseCase createTransactionUseCase,
			ListTransactionsUseCase listTransactionsUseCase,
			FindVisibleCategoryByCodeUseCase findCategoryByCodeUseCase,
			Clock clock) {
		this.createTransactionUseCase = Objects.requireNonNull(
				createTransactionUseCase, "create transaction use case must not be null");
		this.listTransactionsUseCase = Objects.requireNonNull(
				listTransactionsUseCase, "list transactions use case must not be null");
		this.findCategoryByCodeUseCase = Objects.requireNonNull(
				findCategoryByCodeUseCase, "find category use case must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Tool(name = "record_transaction", description = "Record one complete ordinary income or expense in BRL. Never use for reimbursement receipts.")
	public ToolResult recordTransaction(
			@ToolParam(description = "INCOME or EXPENSE", required = false) String kind,
			@ToolParam(description = "A concise transaction description", required = false) String description,
			@ToolParam(description = "Positive BRL amount", required = false) BigDecimal amount,
			@ToolParam(description = "Stable category code such as GROCERIES, SALARY, or OTHER", required = false) String categoryCode,
			@ToolParam(description = "Financial event date in ISO-8601 format; defaults to today", required = false) LocalDate eventDate,
			@ToolParam(description = "Number of monthly installments; defaults to 1", required = false) Integer installmentCount,
			ToolContext toolContext) {
		UserId actorId = AssistantToolContext.requireActor(toolContext);
		if (kind == null || kind.isBlank()) {
			return ToolResult.clarification("Is this transaction income or an expense?");
		}
		if (description == null || description.isBlank()) {
			return ToolResult.clarification("What description should be recorded?");
		}
		if (amount == null) {
			return ToolResult.clarification("What amount should be recorded?");
		}
		TransactionKind parsedKind = ordinaryKind(kind);
		if (parsedKind == null) {
			return ToolResult.rejected("Only ordinary INCOME or EXPENSE transactions can be recorded with this tool.");
		}
		try {
			CategoryId categoryId = resolveCategory(actorId, categoryCode);
			TransactionOutput output = createTransactionUseCase.execute(
					actorId,
					AssistantToolContext.requireSource(toolContext),
					new CreateTransactionInput(
							parsedKind,
							description,
							amount,
							categoryId,
							eventDate == null ? LocalDate.now(clock) : eventDate,
							installmentCount == null ? 1 : installmentCount));
			return ToolResult.success("Transaction recorded successfully.", summarize(output));
		}
		catch (CategoryCodeNotFoundException exception) {
			return ToolResult.rejected("The requested category code is unavailable.");
		}
		catch (InvalidTransactionInputException exception) {
			return ToolResult.rejected("The transaction was rejected by financial validation.");
		}
	}

	@Tool(name = "search_transactions", description = "Search the authenticated user's transactions with bounded filters and paging.")
	public ToolResult searchTransactions(
			@ToolParam(description = "Optional INCOME, EXPENSE, or REIMBURSEMENT_RECEIPT filter", required = false) String kind,
			@ToolParam(description = "Optional stable category code", required = false) String categoryCode,
			@ToolParam(description = "Optional inclusive effective-date start", required = false) LocalDate from,
			@ToolParam(description = "Optional inclusive effective-date end", required = false) LocalDate to,
			@ToolParam(description = "Optional description search text", required = false) String searchText,
			@ToolParam(description = "Zero-based page; defaults to 0", required = false) Integer page,
			@ToolParam(description = "Bounded page size; defaults to 20", required = false) Integer size,
			ToolContext toolContext) {
		UserId actorId = AssistantToolContext.requireActor(toolContext);
		TransactionKind parsedKind;
		try {
			parsedKind = kind == null || kind.isBlank()
					? null
					: TransactionKind.valueOf(kind.strip().toUpperCase(Locale.ROOT));
			CategoryId categoryId = categoryCode == null || categoryCode.isBlank()
					? null
					: resolveCategory(actorId, categoryCode);
			TransactionPageOutput output = listTransactionsUseCase.execute(
					actorId,
					new ListTransactionsInput(
							parsedKind,
							categoryId,
							from,
							to,
							searchText,
							page == null ? 0 : page,
							size == null ? DEFAULT_PAGE_SIZE : size));
			return ToolResult.success("Transaction search completed.", new TransactionSearchData(
					output.content().stream().map(TransactionAssistantTools::summarize).toList(),
					output.page(), output.size(), output.totalElements(), output.totalPages()));
		}
		catch (IllegalArgumentException | CategoryCodeNotFoundException | InvalidTransactionInputException exception) {
			return ToolResult.rejected("The transaction search filters are invalid.");
		}
	}

	private CategoryId resolveCategory(UserId actorId, String categoryCode) {
		String code = categoryCode == null || categoryCode.isBlank()
				? DEFAULT_CATEGORY_CODE
				: categoryCode;
		return findCategoryByCodeUseCase.execute(actorId, code).id();
	}

	private static TransactionKind ordinaryKind(String kind) {
		try {
			TransactionKind parsed = TransactionKind.valueOf(kind.strip().toUpperCase(Locale.ROOT));
			return parsed == TransactionKind.INCOME || parsed == TransactionKind.EXPENSE
					? parsed
					: null;
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static TransactionSummary summarize(TransactionOutput output) {
		return new TransactionSummary(
				output.kind(),
				output.description(),
				output.amount(),
				output.currency(),
				output.eventDate(),
				output.installmentCount());
	}

	public record TransactionSummary(
			TransactionKind kind,
			String description,
			BigDecimal amount,
			String currency,
			LocalDate eventDate,
			int installmentCount) {
	}

	public record TransactionSearchData(
			List<TransactionSummary> transactions,
			int page,
			int size,
			long totalElements,
			int totalPages) {
	}
}
