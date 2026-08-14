package com.talktally.infrastructure.persistence.jpa.entity;

import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "financial_transaction")
public class FinancialTransactionJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, length = 32)
	private TransactionKind kind;

	@Column(name = "description", nullable = false, length = 500)
	private String description;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(name = "event_date", nullable = false)
	private LocalDate eventDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 32)
	private TransactionSource source;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected FinancialTransactionJpaEntity() {
	}

	public FinancialTransactionJpaEntity(
			UUID id,
			UUID userId,
			TransactionKind kind,
			String description,
			BigDecimal totalAmount,
			String currency,
			UUID categoryId,
			LocalDate eventDate,
			TransactionSource source,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.userId = userId;
		this.kind = kind;
		this.description = description;
		this.totalAmount = totalAmount;
		this.currency = currency;
		this.categoryId = categoryId;
		this.eventDate = eventDate;
		this.source = source;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void update(
			TransactionKind kind,
			String description,
			BigDecimal totalAmount,
			String currency,
			UUID categoryId,
			LocalDate eventDate,
			TransactionSource source,
			Instant updatedAt) {
		this.kind = kind;
		this.description = description;
		this.totalAmount = totalAmount;
		this.currency = currency;
		this.categoryId = categoryId;
		this.eventDate = eventDate;
		this.source = source;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public TransactionKind getKind() {
		return kind;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public LocalDate getEventDate() {
		return eventDate;
	}

	public TransactionSource getSource() {
		return source;
	}
}
