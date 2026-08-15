package com.talktally.infrastructure.web.dashboard;

import com.talktally.application.reporting.GetCategoryBreakdownUseCase;
import com.talktally.application.reporting.GetFinancialSummaryUseCase;
import com.talktally.application.reporting.GetMonthlyCashFlowUseCase;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

	private final GetFinancialSummaryUseCase summaryUseCase;
	private final GetCategoryBreakdownUseCase categoryBreakdownUseCase;
	private final GetMonthlyCashFlowUseCase monthlyCashFlowUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public DashboardController(
			GetFinancialSummaryUseCase summaryUseCase,
			GetCategoryBreakdownUseCase categoryBreakdownUseCase,
			GetMonthlyCashFlowUseCase monthlyCashFlowUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.summaryUseCase = Objects.requireNonNull(
				summaryUseCase, "summary use case must not be null");
		this.categoryBreakdownUseCase = Objects.requireNonNull(
				categoryBreakdownUseCase, "category breakdown use case must not be null");
		this.monthlyCashFlowUseCase = Objects.requireNonNull(
				monthlyCashFlowUseCase, "monthly cash flow use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@GetMapping("/summary")
	public FinancialSummaryResponse summary(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return FinancialSummaryResponse.from(summaryUseCase.execute(actorId, from, to));
	}

	@GetMapping("/category-breakdown")
	public CategoryBreakdownResponse categoryBreakdown(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam TransactionKind kind) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return CategoryBreakdownResponse.from(
				categoryBreakdownUseCase.execute(actorId, from, to, kind));
	}

	@GetMapping("/monthly-cash-flow")
	public MonthlyCashFlowResponse monthlyCashFlow(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return MonthlyCashFlowResponse.from(monthlyCashFlowUseCase.execute(actorId, from, to));
	}
}
