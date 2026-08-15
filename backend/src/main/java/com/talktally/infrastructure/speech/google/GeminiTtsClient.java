package com.talktally.infrastructure.speech.google;

import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

public interface GeminiTtsClient {

	GenerateContentResponse generate(String model, String prompt, GenerateContentConfig config);
}
