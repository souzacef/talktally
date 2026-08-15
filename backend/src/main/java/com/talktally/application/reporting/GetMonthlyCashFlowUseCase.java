package com.talktally.application.reporting;

import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GetMonthlyCashFlowUseCase {

	private final FinancialReportingRepository reportingRepository;

	public GetMonthlyCashFlowUseCase(FinancialReportingRepository reportingRepository) {
		this.reportingRepository = Objects.requireNonNull(
				reportingRepository, "reporting repository must not be null");
	}

	@Transactional(readOnly = true)
	public MonthlyCashFlowOutput execute(UserId actorId, LocalDate from, LocalDate to) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		ReportingPolicy.requireValidMonthlyRange(from, to);
		Map<YearMonth, MonthlyFinancialTotal> totalsByMonth = reportingRepository
				.monthlyCashFlow(actorId, from, to)
				.stream()
				.collect(Collectors.toMap(MonthlyFinancialTotal::month, Function.identity()));
		List<MonthlyCashFlowBucketOutput> buckets = new ArrayList<>();
		YearMonth current = YearMonth.from(from);
		YearMonth last = YearMonth.from(to);
		while (!current.isAfter(last)) {
			MonthlyFinancialTotal total = totalsByMonth.getOrDefault(
					current,
					new MonthlyFinancialTotal(
							current, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
			BigDecimal net = total.earnedIncome()
					.add(total.reimbursementsReceived())
					.subtract(total.expenses());
			buckets.add(new MonthlyCashFlowBucketOutput(
					current.getYear(),
					current.getMonthValue(),
					total.earnedIncome(),
					total.expenses(),
					total.reimbursementsReceived(),
					net));
			current = current.plusMonths(1);
		}
		return new MonthlyCashFlowOutput(from, to, "BRL", buckets);
	}
}
