package com.talktally.application.reporting;

import com.talktally.domain.Money;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimPage;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementClaimSearchCriteria;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;

@Service
public class GetFinancialSummaryUseCase {

	private static final int SNAPSHOT_PAGE_SIZE = 100;
	private static final Currency BRL = Currency.getInstance("BRL");

	private final FinancialReportingRepository reportingRepository;
	private final ReimbursementClaimRepository claimRepository;

	public GetFinancialSummaryUseCase(
			FinancialReportingRepository reportingRepository,
			ReimbursementClaimRepository claimRepository) {
		this.reportingRepository = Objects.requireNonNull(
				reportingRepository, "reporting repository must not be null");
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
	}

	@Transactional(readOnly = true)
	public FinancialSummaryOutput execute(UserId actorId, LocalDate from, LocalDate to) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		ReportingPolicy.requireValidRange(from, to);
		FinancialPeriodTotals totals = reportingRepository.summarize(actorId, from, to);
		BigDecimal netCashFlow = totals.earnedIncome()
				.add(totals.reimbursementsReceived())
				.subtract(totals.expenses());
		return new FinancialSummaryOutput(
				from,
				to,
				BRL.getCurrencyCode(),
				new FinancialPeriodOutput(
						totals.earnedIncome(),
						totals.expenses(),
						totals.reimbursementsReceived(),
						netCashFlow,
						totals.occurrenceCount(),
						totals.transactionCount()),
				currentOwedToMe(actorId));
	}

	private OwedToMeSnapshotOutput currentOwedToMe(UserId actorId) {
		Money outstanding = Money.zero(BRL);
		long openClaims = 0;
		int pageNumber = 0;
		ReimbursementClaimPage page;
		do {
			page = claimRepository.search(
					actorId,
					new ReimbursementClaimSearchCriteria(
							Optional.empty(),
							Optional.empty(),
							pageNumber,
							SNAPSHOT_PAGE_SIZE));
			for (ReimbursementClaim claim : page.content()) {
				if (claim.status() != ReimbursementStatus.PAID) {
					outstanding = outstanding.add(claim.remainingAmount());
					openClaims++;
				}
			}
			pageNumber++;
		}
		while (pageNumber < page.totalPages());
		return new OwedToMeSnapshotOutput(outstanding.amount(), openClaims);
	}
}
