package com.talktally.application.speech;

public interface TextToSpeechPort {

	SpeechAudio synthesize(String text);
}
