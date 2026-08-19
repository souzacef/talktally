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

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Column(name = "allowed_kind", nullable = false, length = 32)
	private String allowedKind;

	@Column(name = "built_in", nullable = false)
	private boolean builtIn;

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

	public String getDisplayName() {
		return displayName;
	}

	public String getAllowedKind() {
		return allowedKind;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}
}
