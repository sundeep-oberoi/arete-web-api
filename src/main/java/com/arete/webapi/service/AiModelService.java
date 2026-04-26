package com.arete.webapi.service;

import com.openai.azure.credential.AzureApiKeyCredential;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.ai.PremiumResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiModelService {

    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);

    @Value("${model.api.url:}")
    private String modelApiUrl;

    @Value("${model.api.key:}")
    private String modelApiKey;

    @Value("${model.name:}")
    private String modelName;

    @Value("${model.system.prompt}")
    private String systemPrompt;

    @Value("${model.user.prompt.template}")
    private String userPromptTemplate;

    private OpenAIClient openAiClient;
    private final ObjectMapper objectMapper;

    public AiModelService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initClient() {
        if (modelApiUrl != null && !modelApiUrl.isBlank()) {
            openAiClient = OpenAIOkHttpClient.builder()
                    .baseUrl(modelApiUrl)
                    .credential(AzureApiKeyCredential.create(modelApiKey))
                    .build();
            log.info("OpenAI client initialised for Azure AI Foundry endpoint: {}", modelApiUrl);
        } else {
            log.warn("MODEL_API_URL not configured — AI model calls will fail at runtime");
        }
    }

    public PremiumResult fetchPremium(FormData formData) {
        if (openAiClient == null) {
            throw new IllegalStateException("MODEL_API_URL is not configured");
        }

        String userPrompt = buildUserPrompt(formData);
        log.info("Calling Azure AI Foundry model for premium calculation");
        log.info("User prompt: {}", userPrompt);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(modelName != null ? modelName : "")
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        ChatCompletion completion = openAiClient.chat().completions().create(params);

        if (completion.choices().isEmpty()) {
            throw new IllegalStateException("Empty response from AI model");
        }

        String content = completion.choices().get(0).message().content()
                .orElseThrow(() -> new IllegalStateException("Empty response from AI model"));
        log.info("AI model raw response: {}", content);

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
