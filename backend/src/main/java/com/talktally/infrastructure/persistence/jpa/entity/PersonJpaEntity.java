package com.talktally.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "person")
public class PersonJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(name = "normalized_name", nullable = false, length = 120)
	private String normalizedName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PersonJpaEntity() {
	}

	public PersonJpaEntity(
			UUID id,
			UUID userId,
			String displayName,
			String normalizedName,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.userId = userId;
		this.displayName = displayName;
		this.normalizedName = normalizedName;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void update(String displayName, String normalizedName, Instant updatedAt) {
		this.displayName = displayName;
		this.normalizedName = normalizedName;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getNormalizedName() {
		return normalizedName;
	}
}
