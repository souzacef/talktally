package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.application.assistant.AssistantConversationMessage;
import com.talktally.application.assistant.AssistantConversationPort;
import com.talktally.application.assistant.AssistantConversationRole;
import com.talktally.application.assistant.AssistantOutput;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.AssistantMessageJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.AssistantMessageEntityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
@Transactional(readOnly = true)
public class JpaAssistantConversationAdapter implements AssistantConversationPort {

	private final AssistantMessageEntityRepository repository;

	public JpaAssistantConversationAdapter(AssistantMessageEntityRepository repository) {
		this.repository = Objects.requireNonNull(
				repository, "assistant message repository must not be null");
	}

	@Override
	public List<AssistantConversationMessage> findRecent(UserId actorId, int limit) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		PageRequest page = PageRequest.of(
				0,
				limit,
				Sort.by(Sort.Direction.DESC, "id"));
		List<AssistantMessageJpaEntity> newestFirst = repository.findByUserId(
				actorId.value(), page);
		List<AssistantConversationMessage> chronological = new ArrayList<>(newestFirst.size());
		for (int index = newestFirst.size() - 1; index >= 0; index--) {
			chronological.add(toApplication(newestFirst.get(index)));
		}
		return List.copyOf(chronological);
	}

	@Override
	@Transactional
	public void appendExchange(
			UserId actorId,
			TransactionSource source,
			String userMessage,
			AssistantOutput assistantOutput) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(userMessage, "user message must not be null");
		Objects.requireNonNull(assistantOutput, "assistant output must not be null");
		Instant now = Instant.now();
		repository.saveAllAndFlush(List.of(
				new AssistantMessageJpaEntity(
						actorId.value(),
						AssistantConversationRole.USER,
						userMessage,
						source,
						null,
						now),
				new AssistantMessageJpaEntity(
						actorId.value(),
						AssistantConversationRole.ASSISTANT,
						assistantOutput.message(),
						null,
						assistantOutput.status(),
						now)));
	}

	@Override
	@Transactional
	public void clear(UserId actorId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		repository.deleteAllByUserId(actorId.value());
	}

	private static AssistantConversationMessage toApplication(AssistantMessageJpaEntity entity) {
		return new AssistantConversationMessage(
				entity.getId(),
				entity.getRole(),
				entity.getContent(),
				entity.getSource(),
				entity.getStatus(),
				entity.getCreatedAt());
	}
}
