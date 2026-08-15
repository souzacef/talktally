package com.talktally.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "category")
public class CategoryJpaEntity {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "owner_user_id")
	private UUID ownerUserId;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "allowed_kind", nullable = false, length = 32)
	private String allowedKind;

	protected CategoryJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerUserId() {
		return ownerUserId;
	}

	public String getCode() {
		return code;
	}

	public String getAllowedKind() {
		return allowedKind;
	}
}
