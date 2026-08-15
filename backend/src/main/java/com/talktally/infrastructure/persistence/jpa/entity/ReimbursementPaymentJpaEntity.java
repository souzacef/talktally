package com.talktally.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reimbursement_payment")
public class ReimbursementPaymentJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "claim_id", nullable = false)
	private UUID claimId;

	@Column(name = "receipt_transaction_id", nullable = false)
	private UUID receiptTransactionId;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "received_date", nullable = false)
	private LocalDate receivedDate;

	@Column(name = "note", length = 500)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ReimbursementPaymentJpaEntity() {
	}

	public ReimbursementPaymentJpaEntity(
			UUID id,
			UUID userId,
			UUID claimId,
			UUID receiptTransactionId,
			BigDecimal amount,
			String currency,
			LocalDate receivedDate,
			String note,
			Instant createdAt) {
		this.id = id;
		this.userId = userId;
		this.claimId = claimId;
		this.receiptTransactionId = receiptTransactionId;
		this.amount = amount;
		this.currency = currency;
		this.receivedDate = receivedDate;
		this.note = note;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getClaimId() {
		return claimId;
	}

	public UUID getReceiptTransactionId() {
		return receiptTransactionId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public LocalDate getReceivedDate() {
		return receivedDate;
	}

	public String getNote() {
		return note;
	}
}
