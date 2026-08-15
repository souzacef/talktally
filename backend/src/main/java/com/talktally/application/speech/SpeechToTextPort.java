package com.talktally.application.speech;

public interface SpeechToTextPort {

	String transcribe(SpeechAudioInput input);
}
