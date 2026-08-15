package com.talktally.application.speech;

import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.application.speech.exception.InvalidAudioException;
import com.talktally.application.speech.exception.SpeechSynthesisException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

import java.util.Objects;
import java.util.Optional;

public class VoiceAssistantUseCase {

	private final SpeechToTextPort speechToTextPort;
	private final AssistantUseCase assistantUseCase;
	private final TextToSpeechPort textToSpeechPort;

	public VoiceAssistantUseCase(
			SpeechToTextPort speechToTextPort,
			AssistantUseCase assistantUseCase,
			TextToSpeechPort textToSpeechPort) {
		this.speechToTextPort = Objects.requireNonNull(
				speechToTextPort, "speech-to-text port must not be null");
		this.assistantUseCase = Objects.requireNonNull(
				assistantUseCase, "assistant use case must not be null");
		this.textToSpeechPort = Objects.requireNonNull(
				textToSpeechPort, "text-to-speech port must not be null");
	}

	public VoiceAssistantOutput execute(UserId actorId, VoiceAssistantInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (input == null) {
			throw new InvalidAudioException("voice assistant input is required");
		}
		SpeechAudioInput speechInput = input.toSpeechInput();
		SpeechAudioPolicy.validateAndNormalize(speechInput);
		String transcription = speechToTextPort.transcribe(speechInput);
		if (transcription == null || transcription.isBlank()) {
			throw new InvalidAudioException("audio did not contain recognizable speech");
		}
		String transcript = transcription.strip();
		AssistantOutput assistant = assistantUseCase.execute(
				actorId, TransactionSource.VOICE, transcript);
		try {
			SpeechAudio speech = textToSpeechPort.synthesize(assistant.message());
			return new VoiceAssistantOutput(
					transcript,
					assistant.message(),
					assistant.status(),
					SpeechStatus.GENERATED,
					Optional.of(speech));
		}
		catch (SpeechSynthesisException exception) {
			return new VoiceAssistantOutput(
					transcript,
					assistant.message(),
					assistant.status(),
					SpeechStatus.UNAVAILABLE,
					Optional.empty());
		}
	}
}
