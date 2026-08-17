package com.talktally.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transaction_occurrence")
public class TransactionOccurrenceJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "sequence_number", nullable = false)
	private int sequenceNumber;

	@Column(name = "effective_date", nullable = false)
	private LocalDate effectiveDate;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private String currency;

	protected TransactionOccurrenceJpaEntity() {
	}

	public TransactionOccurrenceJpaEntity(
			UUID id,
			UUID transactionId,
			UUID userId,
			int sequenceNumber,
			LocalDate effectiveDate,
			BigDecimal amount,
			String currency) {
		this.id = id;
		this.transactionId = transactionId;
		this.userId = userId;
		this.sequenceNumber = sequenceNumber;
		this.effectiveDate = effectiveDate;
		this.amount = amount;
		this.currency = currency;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public LocalDate getEffectiveDate() {
		return effectiveDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}
}
