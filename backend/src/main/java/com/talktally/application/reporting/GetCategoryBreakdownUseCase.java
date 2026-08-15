package com.talktally.application.reporting;

import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class GetCategoryBreakdownUseCase {

	public static final int PERCENTAGE_SCALE = 2;

	private final FinancialReportingRepository reportingRepository;

	public GetCategoryBreakdownUseCase(FinancialReportingRepository reportingRepository) {
		this.reportingRepository = Objects.requireNonNull(
				reportingRepository, "reporting repository must not be null");
	}

	@Transactional(readOnly = true)
	public CategoryBreakdownOutput execute(
			UserId actorId,
			LocalDate from,
			LocalDate to,
			TransactionKind kind) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		ReportingPolicy.requireValidRange(from, to);
		if (kind != TransactionKind.EXPENSE && kind != TransactionKind.INCOME) {
			throw new InvalidReportingInputException(
					"category breakdown kind must be EXPENSE or INCOME");
		}
		List<CategoryFinancialTotal> totals = reportingRepository
				.categoryBreakdown(actorId, from, to, kind)
				.stream()
				.sorted(Comparator.comparing(CategoryFinancialTotal::total)
						.reversed()
						.thenComparing(CategoryFinancialTotal::categoryCode))
				.toList();
		BigDecimal grandTotal = totals.stream()
				.map(CategoryFinancialTotal::total)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<CategoryBreakdownItemOutput> categories = totals.stream()
				.map(total -> new CategoryBreakdownItemOutput(
						total.categoryId(),
						total.categoryCode(),
						total.categoryDisplayName(),
						total.total(),
						percentage(total.total(), grandTotal),
						total.occurrenceCount(),
						total.transactionCount()))
				.toList();
		return new CategoryBreakdownOutput(
				from, to, kind, "BRL", grandTotal, categories);
	}

	private static BigDecimal percentage(BigDecimal value, BigDecimal total) {
		if (total.signum() == 0) {
			return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE);
		}
		return value.multiply(BigDecimal.valueOf(100))
				.divide(total, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
	}
}
