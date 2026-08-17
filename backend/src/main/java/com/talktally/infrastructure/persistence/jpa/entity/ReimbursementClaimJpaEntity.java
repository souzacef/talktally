package com.talktally.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reimbursement_claim")
public class ReimbursementClaimJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "expense_transaction_id", nullable = false)
	private UUID expenseTransactionId;

	@Column(name = "person_id", nullable = false)
	private UUID personId;

	@Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal originalAmount;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private String currency;

	@Column(name = "note", length = 500)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ReimbursementClaimJpaEntity() {
	}

	public ReimbursementClaimJpaEntity(
			UUID id,
			UUID userId,
			UUID expenseTransactionId,
			UUID personId,
			BigDecimal originalAmount,
			String currency,
			String note,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.userId = userId;
		this.expenseTransactionId = expenseTransactionId;
		this.personId = personId;
		this.originalAmount = originalAmount;
		this.currency = currency;
		this.note = note;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void update(
			UUID personId,
			BigDecimal originalAmount,
			String currency,
			String note,
			Instant updatedAt) {
		this.personId = personId;
		this.originalAmount = originalAmount;
		this.currency = currency;
		this.note = note;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getExpenseTransactionId() {
		return expenseTransactionId;
	}

	public UUID getPersonId() {
		return personId;
	}

	public BigDecimal getOriginalAmount() {
		return originalAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
