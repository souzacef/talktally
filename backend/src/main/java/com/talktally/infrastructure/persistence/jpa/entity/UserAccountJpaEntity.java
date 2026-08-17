package com.talktally.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccountJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "email", nullable = false, length = 320, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(name = "default_currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private String defaultCurrency;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserAccountJpaEntity() {
	}

	public UserAccountJpaEntity(
			UUID id,
			String email,
			String passwordHash,
			String displayName,
			String defaultCurrency,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.defaultCurrency = defaultCurrency;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void update(
			String email,
			String passwordHash,
			String displayName,
			String defaultCurrency,
			Instant updatedAt) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.defaultCurrency = defaultCurrency;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDefaultCurrency() {
		return defaultCurrency;
	}
}
