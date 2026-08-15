package com.talktally.infrastructure.ai;

import com.talktally.application.reporting.GetCategoryBreakdownUseCase;
import com.talktally.application.reporting.GetFinancialSummaryUseCase;
import com.talktally.application.reporting.GetMonthlyCashFlowUseCase;
import com.talktally.application.reporting.InvalidReportingInputException;
import com.talktally.application.reporting.CategoryBreakdownItemOutput;
import com.talktally.application.reporting.CategoryBreakdownOutput;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class ReportingAssistantTools {

	private final GetFinancialSummaryUseCase getFinancialSummaryUseCase;
	private final GetCategoryBreakdownUseCase getCategoryBreakdownUseCase;
	private final GetMonthlyCashFlowUseCase getMonthlyCashFlowUseCase;

	public ReportingAssistantTools(
			GetFinancialSummaryUseCase getFinancialSummaryUseCase,
			GetCategoryBreakdownUseCase getCategoryBreakdownUseCase,
			GetMonthlyCashFlowUseCase getMonthlyCashFlowUseCase) {
		this.getFinancialSummaryUseCase = Objects.requireNonNull(
				getFinancialSummaryUseCase, "financial summary use case must not be null");
		this.getCategoryBreakdownUseCase = Objects.requireNonNull(
				getCategoryBreakdownUseCase, "category breakdown use case must not be null");
		this.getMonthlyCashFlowUseCase = Objects.requireNonNull(
				getMonthlyCashFlowUseCase, "monthly cash flow use case must not be null");
	}

	@Tool(name = "get_financial_summary", description = "Get deterministic financial totals and the current owed-to-me snapshot for an inclusive date range.")
	public ToolResult getFinancialSummary(
			@ToolParam(description = "Required inclusive period start", required = false) LocalDate from,
			@ToolParam(description = "Required inclusive period end", required = false) LocalDate to,
			ToolContext toolContext) {
		if (from == null || to == null) {
			return ToolResult.clarification("What start and end dates should the summary cover?");
		}
		try {
			UserId actorId = AssistantToolContext.requireActor(toolContext);
			return ToolResult.success(
					"Financial summary calculated by TalkTally.",
					getFinancialSummaryUseCase.execute(actorId, from, to));
		}
		catch (InvalidReportingInputException exception) {
			return ToolResult.rejected("The requested reporting period is invalid.");
		}
	}

	@Tool(name = "get_category_breakdown", description = "Get a deterministic income or expense category breakdown for an inclusive date range.")
	public ToolResult getCategoryBreakdown(
			@ToolParam(description = "Required inclusive period start", required = false) LocalDate from,
			@ToolParam(description = "Required inclusive period end", required = false) LocalDate to,
			@ToolParam(description = "Required INCOME or EXPENSE reporting kind", required = false) String kind,
			ToolContext toolContext) {
		if (from == null || to == null || kind == null || kind.isBlank()) {
			return ToolResult.clarification("Start date, end date, and INCOME or EXPENSE are required.");
		}
		try {
			UserId actorId = AssistantToolContext.requireActor(toolContext);
			TransactionKind parsedKind = TransactionKind.valueOf(kind.strip().toUpperCase(Locale.ROOT));
			CategoryBreakdownOutput output = getCategoryBreakdownUseCase.execute(
					actorId, from, to, parsedKind);
			return ToolResult.success(
					"Category breakdown calculated by TalkTally.",
					new CategoryBreakdownData(
							output.from(),
							output.to(),
							output.kind(),
							output.currency(),
							output.total(),
							output.categories().stream()
									.map(ReportingAssistantTools::safeCategory)
									.toList()));
		}
		catch (IllegalArgumentException | InvalidReportingInputException exception) {
			return ToolResult.rejected("The requested category breakdown is invalid.");
		}
	}

	@Tool(name = "get_monthly_cash_flow", description = "Get deterministic monthly cash-flow buckets for an inclusive date range.")
	public ToolResult getMonthlyCashFlow(
			@ToolParam(description = "Required inclusive period start", required = false) LocalDate from,
			@ToolParam(description = "Required inclusive period end", required = false) LocalDate to,
			ToolContext toolContext) {
		if (from == null || to == null) {
			return ToolResult.clarification("What start and end dates should monthly cash flow cover?");
		}
		try {
			UserId actorId = AssistantToolContext.requireActor(toolContext);
			return ToolResult.success(
					"Monthly cash flow calculated by TalkTally.",
					getMonthlyCashFlowUseCase.execute(actorId, from, to));
		}
		catch (InvalidReportingInputException exception) {
			return ToolResult.rejected("The requested monthly reporting period is invalid.");
		}
	}

	private static CategoryBreakdownItem safeCategory(CategoryBreakdownItemOutput item) {
		return new CategoryBreakdownItem(
				item.code(),
				item.displayName(),
				item.total(),
				item.percentage(),
				item.occurrenceCount(),
				item.transactionCount());
	}

	public record CategoryBreakdownData(
			LocalDate from,
			LocalDate to,
			TransactionKind kind,
			String currency,
			BigDecimal total,
			List<CategoryBreakdownItem> categories) {
	}

	public record CategoryBreakdownItem(
			String code,
			String displayName,
			BigDecimal total,
			BigDecimal percentage,
			long occurrenceCount,
			long transactionCount) {
	}
}
