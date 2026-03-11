package com.example.localchat.application.ai;

import com.example.localchat.adapters.llm.OllamaRestClient;
import com.example.localchat.domain.dto.IncidentClassificationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI-powered incident classification service.
 * Sends worker messages to Llama2 via Ollama for classification.
 * 
 * Responsibilities:
 * - Build structured prompt for LLM
 * - Call Llama2 for classification
 * - Parse JSON response
 * - Convert to IncidentClassificationDto
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentClassificationService {

    private final OllamaRestClient ollamaRestClient;
    private final ObjectMapper objectMapper;

    @Value("${incident.ai.temperature:0.5}")
    private double temperature;

    @Value("${incident.ai.max-tokens:500}")
    private int maxTokens;

    /**
     * Classify an incident message using Llama2.
     * 
     * Example Input:
     * "Machine stopped due to overheating"
     * 
     * Example Output:
     * IncidentClassificationDto(
     *   incidentType="MACHINE_FAILURE",
     *   severity="HIGH",
     *   department="MAINTENANCE",
     *   priority="P2",
     *   suggestedActions=["Check coolant levels", ...],
     *   confidenceScore=0.95
     * )
     */
    public IncidentClassificationDto classifyIncident(String workerMessage) {
        log.debug("Classifying incident message: {} chars", workerMessage.length());

        try {
            // 1. Build prompt
            String prompt = buildClassificationPrompt(workerMessage);
            log.debug("Sending prompt to Llama2: {} chars", prompt.length());

            // 2. Call Llama2 via Ollama
            String response = callLlama2(prompt);
            log.debug("Llama2 response: {} chars", response.length());

            // 3. Parse JSON response
            IncidentClassificationDto classification = parseClassificationResponse(response, workerMessage);
            log.info("Incident classified: type={}, severity={}, department={}", 
                    classification.incidentType(), 
                    classification.severity(),
                    classification.department());

            return classification;

        } catch (Exception ex) {
            log.error("Classification failed: {}", ex.getMessage(), ex);
            // Return safe default classification
            return buildDefaultClassification();
        }
    }

    /**
     * Build the structured prompt for Llama2.
     * 
     * Format:
     * System: You are a factory incident classifier...
     * User: [worker message]
     * Assistant: [respond with JSON]
     */
    private String buildClassificationPrompt(String workerMessage) {
        return String.format(
            """
            You are a factory incident classifier AI. Classify this incident.
            
            Worker Message: "%s"
            
            JSON format (use exact values):
            {"incidentType":"MACHINE_FAILURE|SAFETY_HAZARD|QUALITY_ISSUE|MAINTENANCE_REQUEST|ENVIRONMENTAL_ISSUE","severity":"CRITICAL|HIGH|MEDIUM|LOW","department":"PROD|MAINT|QA|SAFETY|ENV","priority":"P1|P2|P3|P4","suggestedActions":["action1","action2"],"confidenceScore":0.85}
            
            Respond ONLY with the JSON object, no markdown, no extra text.
            """,
            workerMessage
        );
    }

    /**
     * Call Llama2 via Ollama REST API.
     */
    private String callLlama2(String prompt) {
        try {
            String response = ollamaRestClient.generate("llama2", prompt, temperature);
            log.debug("Raw Llama2 response: {}", response);
            return response;
        } catch (Exception ex) {
            log.error("Error calling Llama2: {}", ex.getMessage(), ex);
            throw new RuntimeException("Ollama/Llama2 call failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Parse the JSON response from Llama2.
     */
    private IncidentClassificationDto parseClassificationResponse(String jsonResponse, String originalMessage) {
        try {
            // Clean response - remove markdown code blocks if present
            String cleanJson = cleanJsonResponse(jsonResponse);
            log.debug("Cleaned JSON: {}", cleanJson);

            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            // Extract fields
            String incidentType = getStringField(jsonNode, "incidentType", "MAINTENANCE_REQUEST");
            String severity = getStringField(jsonNode, "severity", "MEDIUM");
            String department = getStringField(jsonNode, "department", "PROD");
            String priority = getStringField(jsonNode, "priority", "P3");
            List<String> actions = getArrayField(jsonNode, "suggestedActions");
            Double confidence = getDoubleField(jsonNode, "confidenceScore", 0.5);

            return new IncidentClassificationDto(
                    incidentType,
                    severity,
                    department,
                    priority,
                    actions,
                    confidence
            );

        } catch (Exception ex) {
            log.error("Failed to parse Llama2 JSON response: {}", ex.getMessage(), ex);
            log.error("Response was: {}", jsonResponse);
            // Try to extract department from response as fallback
            if (jsonResponse.contains("department")) {
                try {
                    String dept = extractDepartmentFallback(jsonResponse);
                    if (dept != null) {
                        return new IncidentClassificationDto(
                                "MAINTENANCE_REQUEST",
                                "MEDIUM",
                                dept,
                                "P3",
                                List.of("Investigate issue further"),
                                0.3
                        );
                    }
                } catch (Exception e2) {
                    log.debug("Fallback department extraction also failed");
                }
            }
            return buildDefaultClassification();
        }
    }

    /**
     * Extract department from response as last resort.
     */
    private String extractDepartmentFallback(String response) {
        // Try to find department value in response
        String[] deptCodes = {"PROD", "MAINT", "QA", "SAFETY", "ENV"};
        for (String code : deptCodes) {
            if (response.contains("\"" + code + "\"") || response.contains("'" + code + "'")) {
                return code;
            }
        }
        return "PROD";
    }

    /**
     * Clean JSON response (remove markdown code blocks, extra whitespace).
     */
    private String cleanJsonResponse(String response) {
        // Remove markdown code blocks
        String cleaned = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // Find first { and last }
        int startIdx = cleaned.indexOf('{');
        int endIdx = cleaned.lastIndexOf('}');

        if (startIdx >= 0 && endIdx > startIdx) {
            return cleaned.substring(startIdx, endIdx + 1);
        }

        return cleaned;
    }

    /**
     * Safely extract string field from JSON.
     */
    private String getStringField(JsonNode node, String fieldName, String defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Safely extract double field from JSON.
     */
    private Double getDoubleField(JsonNode node, String fieldName, Double defaultValue) {
        if (node.has(fieldName) && node.get(fieldName).isNumber()) {
            return node.get(fieldName).asDouble(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Safely extract array field from JSON.
     */
    private List<String> getArrayField(JsonNode node, String fieldName) {
        List<String> result = new ArrayList<>();
        if (node.has(fieldName) && node.get(fieldName).isArray()) {
            node.get(fieldName).forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    /**
     * Return safe default classification.
     */
    private IncidentClassificationDto buildDefaultClassification() {
        return new IncidentClassificationDto(
                "MAINTENANCE_REQUEST",
                "MEDIUM",
                "PROD",
                "P3",
                List.of("Investigate issue further"),
                0.3
        );
    }
}

