package com.talktally.infrastructure.persistence.jpa.entity;

import com.talktally.application.assistant.AssistantConversationRole;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.domain.TransactionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assistant_message")
public class AssistantMessageJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 16)
	private AssistantConversationRole role;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", length = 32)
	private TransactionSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 32)
	private AssistantStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AssistantMessageJpaEntity() {
	}

	public AssistantMessageJpaEntity(
			UUID userId,
			AssistantConversationRole role,
			String content,
			TransactionSource source,
			AssistantStatus status,
			Instant createdAt) {
		this.userId = userId;
		this.role = role;
		this.content = content;
		this.source = source;
		this.status = status;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public AssistantConversationRole getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public TransactionSource getSource() {
		return source;
	}

	public AssistantStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
