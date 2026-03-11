package com.example.localchat.application.port.out;

/**
 * Secondary (driven) port: abstraction over the LLM text-generation backend.
 */
public interface LlmGenerationPort {

    /**
     * Send a formatted prompt to the LLM and return its text response.
     *
     * @param model       model name (e.g. "llama2")
     * @param prompt      fully formatted prompt including history
     * @param temperature sampling temperature
     * @return the raw generated text from the model
     */
    String generate(String model, String prompt, double temperature);
}
