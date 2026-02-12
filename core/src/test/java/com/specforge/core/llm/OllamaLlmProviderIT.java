package com.specforge.core.llm;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Disabled by default. Requires local Ollama running at http://localhost:11434 with model llama3")
class OllamaLlmProviderIT {

    @Test
    void shouldGenerateResponseFromLocalOllama() {
        LlmProvider provider = new OllamaLlmProvider();

        String response = provider.generate("Hola mundo");

        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}
