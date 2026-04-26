package com.arete.webapi.service;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.ChatCompletionsClientBuilder;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatRequestSystemMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.ai.PremiumResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiModelService {

    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);

    @Value("${model.api.url:}")
    private String modelApiUrl;

    @Value("${model.api.key:}")
    private String modelApiKey;

    @Value("${model.system.prompt}")
    private String systemPrompt;

    @Value("${model.user.prompt.template}")
    private String userPromptTemplate;

    private ChatCompletionsClient chatClient;
    private final ObjectMapper objectMapper;

    public AiModelService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initClient() {
        if (modelApiUrl != null && !modelApiUrl.isBlank()) {
            chatClient = new ChatCompletionsClientBuilder()
                    .credential(new AzureKeyCredential(modelApiKey))
                    .endpoint(modelApiUrl)
                    .buildClient();
            log.info("Azure AI Inference client initialised for endpoint: {}", modelApiUrl);
        } else {
            log.warn("MODEL_API_URL not configured — AI model calls will fail at runtime");
        }
    }

    public PremiumResult fetchPremium(FormData formData) {
        if (chatClient == null) {
            throw new IllegalStateException("MODEL_API_URL is not configured");
        }

        String userPrompt = buildUserPrompt(formData);
        log.info("Calling Azure AI Inference model for premium calculation");
        log.debug("User prompt: {}", userPrompt);

        ChatCompletionsOptions options = new ChatCompletionsOptions(List.of(
                new ChatRequestSystemMessage(systemPrompt),
                new ChatRequestUserMessage(userPrompt)
        ));

        ChatCompletions completions = chatClient.complete(options);

        if (completions == null || completions.getChoices() == null || completions.getChoices().isEmpty()) {
            throw new IllegalStateException("Empty response from AI model");
        }

        String content = completions.getChoices().get(0).getMessage().getContent();
        log.debug("AI model raw response: {}", content);

        return parseModelResponse(content);
    }

    String buildUserPrompt(FormData formData) {
        return userPromptTemplate
                .replace("{profile}", nullSafe(formData.getProfile()))
                .replace("{age}", nullSafe(formData.getAge()))
                .replace("{postcode}", nullSafe(formData.getPostcode()))
                .replace("{coverPartner}", String.valueOf(formData.isCoverPartner()))
                .replace("{coverChildren}", String.valueOf(formData.isCoverChildren()))
                .replace("{numberOfChildren}", String.valueOf(formData.getNumberOfChildren()))
                .replace("{opticalNeeds}", nullSafe(formData.getOpticalNeeds()))
                .replace("{dentalNeeds}", nullSafe(formData.getDentalNeeds()))
                .replace("{alternativeMedicine}", nullSafe(formData.getAlternativeMedicine()))
                .replace("{hospitalisationPreference}", nullSafe(formData.getHospitalisationPreference()))
                .replace("{doctorChoice}", nullSafe(formData.getDoctorChoice()));
    }

    PremiumResult parseModelResponse(String content) {
        try {
            String cleaned = content.replaceAll("```(?:json)?\\s*", "").trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(cleaned);
            double monthly = node.get("monthlyPremium").asDouble();
            double annual = node.get("annualPremium").asDouble();
            log.info("AI model returned premium: monthly={} annual={}", monthly, annual);
            return new PremiumResult(monthly, annual);
        } catch (Exception e) {
            log.error("Failed to parse AI model response: {}", content, e);
            throw new IllegalStateException("Failed to parse premium from AI model response", e);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
