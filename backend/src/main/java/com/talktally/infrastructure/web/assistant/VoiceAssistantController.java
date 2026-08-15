package com.talktally.infrastructure.web.assistant;

import com.talktally.application.speech.SpeechAudioPolicy;
import com.talktally.application.speech.VoiceAssistantInput;
import com.talktally.application.speech.VoiceAssistantOutput;
import com.talktally.application.speech.VoiceAssistantUseCase;
import com.talktally.application.speech.exception.InvalidAudioException;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/assistant/voice")
public class VoiceAssistantController {

	private final VoiceAssistantUseCase voiceAssistantUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public VoiceAssistantController(
			VoiceAssistantUseCase voiceAssistantUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.voiceAssistantUseCase = Objects.requireNonNull(
				voiceAssistantUseCase, "voice assistant use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public VoiceAssistantResponse send(@RequestPart("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidAudioException("audio must not be empty");
		}
		SpeechAudioPolicy.validateSize(file.getSize());
		String mediaType = file.getContentType();
		if (mediaType == null || mediaType.isBlank()) {
			throw new InvalidAudioException("audio media type is required");
		}
		byte[] audio;
		try {
			audio = file.getBytes();
		}
		catch (IOException exception) {
			throw new InvalidAudioException("audio could not be read", exception);
		}
		UserId actorId = authenticatedUserProvider.currentUserId();
		VoiceAssistantOutput output = voiceAssistantUseCase.execute(
				actorId, new VoiceAssistantInput(audio, mediaType));
		return VoiceAssistantResponse.from(output);
	}
}
