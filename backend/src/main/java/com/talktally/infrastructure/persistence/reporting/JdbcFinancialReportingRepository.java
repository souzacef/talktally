package com.talktally.infrastructure.persistence.reporting;

import com.talktally.application.reporting.CategoryFinancialTotal;
import com.talktally.application.reporting.FinancialPeriodTotals;
import com.talktally.application.reporting.FinancialReportingRepository;
import com.talktally.application.reporting.MonthlyFinancialTotal;
import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Repository
@Transactional(readOnly = true)
public class JdbcFinancialReportingRepository implements FinancialReportingRepository {

	private static final String SUMMARY_SQL = """
			SELECT
			    COALESCE(SUM(CASE WHEN ft.kind = 'INCOME'
			        THEN occurrence.amount ELSE CAST(0 AS NUMERIC(19, 2)) END),
			        CAST(0 AS NUMERIC(19, 2))) AS earned_income,
			    COALESCE(SUM(CASE WHEN ft.kind = 'EXPENSE'
			        THEN occurrence.amount ELSE CAST(0 AS NUMERIC(19, 2)) END),
			        CAST(0 AS NUMERIC(19, 2))) AS expenses,
			    COALESCE(SUM(CASE WHEN ft.kind = 'REIMBURSEMENT_RECEIPT'
			        THEN occurrence.amount ELSE CAST(0 AS NUMERIC(19, 2)) END),
			        CAST(0 AS NUMERIC(19, 2))) AS reimbursements_received,
			    COUNT(occurrence.id) AS occurrence_count,
			    COUNT(DISTINCT ft.id) AS transaction_count
			FROM transaction_occurrence occurrence
			JOIN financial_transaction ft
			  ON ft.id = occurrence.transaction_id
			 AND ft.user_id = occurrence.user_id
			WHERE ft.user_id = ?
			  AND occurrence.effective_date BETWEEN ? AND ?
			""";

	private static final String CATEGORY_SQL = """
			SELECT
			    category.id AS category_id,
			    category.code AS category_code,
			    category.display_name AS category_display_name,
			    SUM(occurrence.amount) AS total,
			    COUNT(occurrence.id) AS occurrence_count,
			    COUNT(DISTINCT ft.id) AS transaction_count
			FROM transaction_occurrence occurrence
			JOIN financial_transaction ft
			  ON ft.id = occurrence.transaction_id
			 AND ft.user_id = occurrence.user_id
			JOIN category
			  ON category.id = ft.category_id
			WHERE ft.user_id = ?
			  AND occurrence.effective_date BETWEEN ? AND ?
			  AND ft.kind = ?
			  AND (category.owner_user_id IS NULL OR category.owner_user_id = ft.user_id)
			GROUP BY category.id, category.code, category.display_name
			ORDER BY total DESC, category.code ASC
			""";

	private static final String MONTHLY_SQL = """
			SELECT
			    EXTRACT(YEAR FROM occurrence.effective_date) AS report_year,
			    EXTRACT(MONTH FROM occurrence.effective_date) AS report_month,
			    COALESCE(SUM(CASE WHEN ft.kind = 'INCOME'
			        THEN occurrence.amount ELSE CAST(0 AS NUMERIC(19, 2)) END),
			        CAST(0 AS NUMERIC(19, 2))) AS earned_income,
			    COALESCE(SUM(CASE WHEN ft.kind = 'EXPENSE'
			        THEN occurrence.amount ELSE CAST(0 AS NUMERIC(19, 2)) END),
			        CAST(0 AS NUMERIC(19, 2))) AS expenses,
			    COALESCE(SUM(CASE WHEN ft.kind = 'REIMBURSEMENT_RECEIPT'
			        THEN occurrence.amount ELSE CAST(0 AS NUMERIC(19, 2)) END),
			        CAST(0 AS NUMERIC(19, 2))) AS reimbursements_received
			FROM transaction_occurrence occurrence
			JOIN financial_transaction ft
			  ON ft.id = occurrence.transaction_id
			 AND ft.user_id = occurrence.user_id
			WHERE ft.user_id = ?
			  AND occurrence.effective_date BETWEEN ? AND ?
			GROUP BY
			    EXTRACT(YEAR FROM occurrence.effective_date),
			    EXTRACT(MONTH FROM occurrence.effective_date)
			ORDER BY report_year, report_month
			""";

	private final JdbcTemplate jdbcTemplate;

	public JdbcFinancialReportingRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbc template must not be null");
	}

	@Override
	public FinancialPeriodTotals summarize(UserId ownerId, LocalDate from, LocalDate to) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		return jdbcTemplate.queryForObject(
				SUMMARY_SQL,
				(resultSet, rowNumber) -> new FinancialPeriodTotals(
						resultSet.getBigDecimal("earned_income"),
						resultSet.getBigDecimal("expenses"),
						resultSet.getBigDecimal("reimbursements_received"),
						resultSet.getLong("occurrence_count"),
						resultSet.getLong("transaction_count")),
				ownerId.value(),
				from,
				to);
	}

	@Override
	public List<CategoryFinancialTotal> categoryBreakdown(
			UserId ownerId,
			LocalDate from,
			LocalDate to,
			TransactionKind kind) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		return jdbcTemplate.query(
				CATEGORY_SQL,
				(resultSet, rowNumber) -> new CategoryFinancialTotal(
						CategoryId.from(resultSet.getObject("category_id", java.util.UUID.class)),
						resultSet.getString("category_code"),
						resultSet.getString("category_display_name"),
						resultSet.getBigDecimal("total"),
						resultSet.getLong("occurrence_count"),
						resultSet.getLong("transaction_count")),
				ownerId.value(),
				from,
				to,
				kind.name());
	}

	@Override
	public List<MonthlyFinancialTotal> monthlyCashFlow(
			UserId ownerId,
			LocalDate from,
			LocalDate to) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		return jdbcTemplate.query(
				MONTHLY_SQL,
				(resultSet, rowNumber) -> new MonthlyFinancialTotal(
						YearMonth.of(
								resultSet.getInt("report_year"),
								resultSet.getInt("report_month")),
						resultSet.getBigDecimal("earned_income"),
						resultSet.getBigDecimal("expenses"),
						resultSet.getBigDecimal("reimbursements_received")),
				ownerId.value(),
				from,
				to);
	}
}
