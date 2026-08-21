package com.talktally.application.reporting;

import com.talktally.domain.CategoryId;
import com.talktally.domain.Money;
import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementClaimPage;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementClaimSearchCriteria;
import com.talktally.domain.ReimbursementPayment;
import com.talktally.domain.ReimbursementPaymentId;
import com.talktally.domain.TransactionId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportingUseCasesTests {

	private static final UserId USER = UserId.generate();
	private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
	private static final LocalDate TO = LocalDate.of(2026, 10, 31);
	private static final CategoryId FOOD = CategoryId.generate();
	private static final CategoryId SHOPPING = CategoryId.generate();

	@Test
	void summarySeparatesFlowsComputesNetAndAddsCurrentSnapshot() {
		FakeReportingRepository reporting = new FakeReportingRepository();
		reporting.summary = new FinancialPeriodTotals(
				new BigDecimal("1000.00"),
				new BigDecimal("174.00"),
				new BigDecimal("50.00"),
				3,
				3);
		ReimbursementClaim open = claim("174.00")
				.addPayment(payment("50.00"));
		GetFinancialSummaryUseCase useCase = new GetFinancialSummaryUseCase(
				reporting,
				new FakeClaimRepository(List.of(open)));

		FinancialSummaryOutput output = useCase.execute(USER, FROM, FROM);

		assertEquals(new BigDecimal("1000.00"), output.period().earnedIncome());
		assertEquals(new BigDecimal("174.00"), output.period().expenses());
		assertEquals(new BigDecimal("50.00"), output.period().reimbursementsReceived());
		assertEquals(new BigDecimal("876.00"), output.period().netCashFlow());
		assertEquals(new BigDecimal("124.00"), output.owedToMe().outstanding());
		assertEquals(1, output.owedToMe().openClaims());
	}

	@Test
	void emptySummaryReturnsExactZerosAndRejectsInvalidRange() {
		FakeReportingRepository reporting = new FakeReportingRepository();
		GetFinancialSummaryUseCase useCase = new GetFinancialSummaryUseCase(
				reporting,
				new FakeClaimRepository(List.of()));

		FinancialSummaryOutput output = useCase.execute(USER, FROM, FROM);

		assertEquals(BigDecimal.ZERO, output.period().earnedIncome());
		assertEquals(BigDecimal.ZERO, output.period().expenses());
		assertEquals(BigDecimal.ZERO, output.period().reimbursementsReceived());
		assertEquals(BigDecimal.ZERO, output.period().netCashFlow());
		assertEquals(BigDecimal.ZERO.setScale(2), output.owedToMe().outstanding());
		assertThrows(
				InvalidReportingInputException.class,
				() -> useCase.execute(USER, TO, FROM));
	}

	@Test
	void categoryBreakdownOrdersDeterministicallyAndUsesHalfUpPercentages() {
		FakeReportingRepository reporting = new FakeReportingRepository();
		reporting.categories = List.of(
				new CategoryFinancialTotal(
						SHOPPING, "SHOPPING", "Shopping", new BigDecimal("1.00"), 1, 1),
				new CategoryFinancialTotal(
						FOOD, "FOOD_DINING", "Food and dining", new BigDecimal("2.00"), 2, 1));
		GetCategoryBreakdownUseCase useCase = new GetCategoryBreakdownUseCase(reporting);

		CategoryBreakdownOutput output = useCase.execute(
				USER, FROM, TO, TransactionKind.EXPENSE);

		assertEquals(new BigDecimal("3.00"), output.total());
		assertEquals("FOOD_DINING", output.categories().getFirst().code());
		assertEquals(new BigDecimal("66.67"), output.categories().getFirst().percentage());
		assertEquals(new BigDecimal("33.33"), output.categories().get(1).percentage());
		assertEquals(2, output.categories().getFirst().occurrenceCount());
		assertEquals(1, output.categories().getFirst().transactionCount());
	}

	@Test
	void categoryBreakdownHandlesZeroAndRejectsReimbursementKind() {
		GetCategoryBreakdownUseCase useCase =
				new GetCategoryBreakdownUseCase(new FakeReportingRepository());

		CategoryBreakdownOutput empty = useCase.execute(
				USER, FROM, TO, TransactionKind.INCOME);

		assertEquals(BigDecimal.ZERO, empty.total());
		assertEquals(List.of(), empty.categories());
		assertThrows(
				InvalidReportingInputException.class,
				() -> useCase.execute(
						USER, FROM, TO, TransactionKind.REIMBURSEMENT_RECEIPT));
	}

	@Test
	void monthlyCashFlowAddsZeroMonthsAndKeepsReimbursementsSeparate() {
		FakeReportingRepository reporting = new FakeReportingRepository();
		reporting.monthly = List.of(
				new MonthlyFinancialTotal(
						YearMonth.of(2026, 8),
						new BigDecimal("1000.00"),
						new BigDecimal("33.33"),
						BigDecimal.ZERO),
				new MonthlyFinancialTotal(
						YearMonth.of(2026, 10),
						BigDecimal.ZERO,
						new BigDecimal("33.34"),
						new BigDecimal("50.00")));
		GetMonthlyCashFlowUseCase useCase = new GetMonthlyCashFlowUseCase(reporting);

		MonthlyCashFlowOutput output = useCase.execute(USER, FROM, TO);

		assertEquals(3, output.buckets().size());
		assertEquals(new BigDecimal("966.67"), output.buckets().getFirst().netCashFlow());
		assertEquals(BigDecimal.ZERO, output.buckets().get(1).earnedIncome());
		assertEquals(BigDecimal.ZERO, output.buckets().get(1).expenses());
		assertEquals(new BigDecimal("16.66"), output.buckets().get(2).netCashFlow());
	}

	@Test
	void monthlyCashFlowRejectsReversedAndExcessiveRanges() {
		GetMonthlyCashFlowUseCase useCase =
				new GetMonthlyCashFlowUseCase(new FakeReportingRepository());

		assertThrows(
				InvalidReportingInputException.class,
				() -> useCase.execute(USER, TO, FROM));
		assertThrows(
				InvalidReportingInputException.class,
				() -> useCase.execute(
						USER,
						LocalDate.of(2020, 1, 1),
						LocalDate.of(2025, 1, 1)));
	}

	private static ReimbursementClaim claim(String amount) {
		return ReimbursementClaim.create(
				USER,
				TransactionId.generate(),
				PersonId.generate(),
				Money.brl(new BigDecimal(amount)),
				null);
	}

	private static ReimbursementPayment payment(String amount) {
		return new ReimbursementPayment(
				ReimbursementPaymentId.generate(),
				Money.brl(new BigDecimal(amount)),
				FROM,
				TransactionId.generate(),
				null);
	}

	private static final class FakeReportingRepository implements FinancialReportingRepository {

		private FinancialPeriodTotals summary =
				new FinancialPeriodTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
		private List<CategoryFinancialTotal> categories = List.of();
		private List<MonthlyFinancialTotal> monthly = List.of();

		@Override
		public FinancialPeriodTotals summarize(
				UserId ownerId, LocalDate from, LocalDate to) {
			return summary;
		}

		@Override
		public List<CategoryFinancialTotal> categoryBreakdown(
				UserId ownerId,
				LocalDate from,
				LocalDate to,
				TransactionKind kind) {
			return categories;
		}

		@Override
		public List<MonthlyFinancialTotal> monthlyCashFlow(
				UserId ownerId, LocalDate from, LocalDate to) {
			return monthly;
		}
	}

	private static final class FakeClaimRepository implements ReimbursementClaimRepository {

		private final List<ReimbursementClaim> claims;

		private FakeClaimRepository(List<ReimbursementClaim> claims) {
			this.claims = new ArrayList<>(claims);
		}

		@Override
		public ReimbursementClaim save(ReimbursementClaim claim) {
			return claim;
		}

		@Override
		public Optional<ReimbursementClaim> findById(
				UserId ownerId, ReimbursementClaimId claimId) {
			return claims.stream().filter(claim -> claim.id().equals(claimId)).findFirst();
		}

		@Override
		public Optional<ReimbursementClaim> findByIdForRepayment(
				UserId ownerId, ReimbursementClaimId claimId) {
			return findById(ownerId, claimId);
		}

		@Override
		public ReimbursementClaimPage search(
				UserId ownerId, ReimbursementClaimSearchCriteria criteria) {
			int from = Math.min(criteria.page() * criteria.size(), claims.size());
			int to = Math.min(from + criteria.size(), claims.size());
			return new ReimbursementClaimPage(
					claims.subList(from, to),
					criteria.page(),
					criteria.size(),
					claims.size());
		}

		@Override
		public List<ReimbursementClaim> findAllByPerson(
				UserId ownerId, PersonId personId) {
			return claims.stream().filter(claim -> claim.personId().equals(personId)).toList();
		}

		@Override
		public boolean isTransactionLinked(UserId ownerId, TransactionId transactionId) {
			return false;
		}
	}
}
