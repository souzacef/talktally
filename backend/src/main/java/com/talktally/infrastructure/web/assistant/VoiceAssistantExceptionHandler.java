package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.assistant.exception.InvalidAssistantInputException;
import com.talktally.application.speech.exception.AudioTooLargeException;
import com.talktally.application.speech.exception.InvalidAudioException;
import com.talktally.application.speech.exception.SpeechRecognitionUnavailableException;
import com.talktally.infrastructure.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = VoiceAssistantController.class)
public class VoiceAssistantExceptionHandler {

	@ExceptionHandler(AudioTooLargeException.class)
	public ResponseEntity<ApiError> audioTooLarge() {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
				.body(new ApiError("AUDIO_TOO_LARGE", "audio exceeds the 8 MiB limit"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> multipartTooLarge() {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
				.body(new ApiError("AUDIO_TOO_LARGE", "audio exceeds the 8 MiB limit"));
	}

	@ExceptionHandler(InvalidAudioException.class)
	public ResponseEntity<ApiError> invalidAudio() {
		return ResponseEntity.badRequest()
				.body(new ApiError("INVALID_AUDIO", "audio is empty, unsupported, or invalid"));
	}

	@ExceptionHandler(InvalidAssistantInputException.class)
	public ResponseEntity<ApiError> invalidTranscript() {
		return ResponseEntity.badRequest()
				.body(new ApiError("INVALID_TRANSCRIPT", "transcribed voice command is invalid"));
	}

	@ExceptionHandler(SpeechRecognitionUnavailableException.class)
	public ResponseEntity<ApiError> recognitionUnavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError(
						"SPEECH_RECOGNITION_UNAVAILABLE",
						"speech recognition is temporarily unavailable"));
	}

	@ExceptionHandler(AssistantUnavailableException.class)
	public ResponseEntity<ApiError> assistantUnavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError("ASSISTANT_UNAVAILABLE", "assistant is temporarily unavailable"));
	}
}
