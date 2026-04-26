package com.arete.webapi.service;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.models.ChatChoice;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatResponseMessage;
import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.ai.PremiumResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiModelServiceTest {

    @Mock
    private ChatCompletionsClient chatCompletionsClient;

    private AiModelService service;

    private static final String TEMPLATE =
            "profile={profile}, age={age}, postcode={postcode}, coverPartner={coverPartner}, " +
            "coverChildren={coverChildren}, numberOfChildren={numberOfChildren}, " +
            "opticalNeeds={opticalNeeds}, dentalNeeds={dentalNeeds}, " +
            "alternativeMedicine={alternativeMedicine}, " +
            "hospitalisationPreference={hospitalisationPreference}, doctorChoice={doctorChoice}";

    @BeforeEach
    void setUp() {
        service = new AiModelService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "systemPrompt", "System prompt");
        ReflectionTestUtils.setField(service, "userPromptTemplate", TEMPLATE);
        ReflectionTestUtils.setField(service, "chatClient", chatCompletionsClient);
    }

    @Test
    void fetchPremium_throwsWhenClientNotInitialised() {
        ReflectionTestUtils.setField(service, "chatClient", null);
        assertThatThrownBy(() -> service.fetchPremium(new FormData()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MODEL_API_URL");
    }

    @Test
    void fetchPremium_callsClientAndReturnsParsedPremium() {
        mockClientResponse("{\"monthlyPremium\": 85.5, \"annualPremium\": 1026.0}");

        FormData data = buildFormData();
        PremiumResult result = service.fetchPremium(data);

        assertThat(result.monthlyPremium()).isEqualTo(85.5);
        assertThat(result.annualPremium()).isEqualTo(1026.0);
        verify(chatCompletionsClient).complete(any(ChatCompletionsOptions.class));
    }

    @Test
    void fetchPremium_throwsOnEmptyChoices() {
        ChatCompletions completions = mock(ChatCompletions.class);
        when(completions.getChoices()).thenReturn(List.of());
        when(chatCompletionsClient.complete((ChatCompletionsOptions) any())).thenReturn(completions);

        assertThatThrownBy(() -> service.fetchPremium(buildFormData()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty response");
    }

    @Test
    void buildUserPrompt_replacesAllPlaceholders() {
        String prompt = service.buildUserPrompt(buildFormData());

        assertThat(prompt).contains("profile=employee");
        assertThat(prompt).contains("age=35");
        assertThat(prompt).contains("postcode=75001");
        assertThat(prompt).contains("coverPartner=true");
        assertThat(prompt).contains("coverChildren=false");
        assertThat(prompt).contains("numberOfChildren=0");
        assertThat(prompt).contains("opticalNeeds=standard");
        assertThat(prompt).contains("dentalNeeds=maintenance");
        assertThat(prompt).contains("alternativeMedicine=one_two");
        assertThat(prompt).contains("hospitalisationPreference=private_preferred");
        assertThat(prompt).contains("doctorChoice=gp_specialist");
    }

    @Test
    void buildUserPrompt_handlesNullFields() {
        String prompt = service.buildUserPrompt(new FormData());
        assertThat(prompt).doesNotContain("{profile}");
        assertThat(prompt).doesNotContain("{age}");
    }

    @Test
    void parseModelResponse_parsesCleanJson() {
        PremiumResult result = service.parseModelResponse("{\"monthlyPremium\": 85.5, \"annualPremium\": 1026.0}");
        assertThat(result.monthlyPremium()).isEqualTo(85.5);
        assertThat(result.annualPremium()).isEqualTo(1026.0);
    }

    @Test
    void parseModelResponse_parsesJsonWrappedInMarkdownFences() {
        PremiumResult result = service.parseModelResponse(
                "```json\n{\"monthlyPremium\": 85.5, \"annualPremium\": 1026.0}\n```");
        assertThat(result.monthlyPremium()).isEqualTo(85.5);
        assertThat(result.annualPremium()).isEqualTo(1026.0);
    }

    @Test
    void parseModelResponse_parsesJsonWithSurroundingText() {
        PremiumResult result = service.parseModelResponse(
                "Based on your profile: {\"monthlyPremium\": 92.0, \"annualPremium\": 1104.0}");
        assertThat(result.monthlyPremium()).isEqualTo(92.0);
        assertThat(result.annualPremium()).isEqualTo(1104.0);
    }

    @Test
    void parseModelResponse_throwsOnInvalidContent() {
        assertThatThrownBy(() -> service.parseModelResponse("not valid json at all"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse premium");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void mockClientResponse(String content) {
        ChatResponseMessage message = mock(ChatResponseMessage.class);
        when(message.getContent()).thenReturn(content);

        ChatChoice choice = mock(ChatChoice.class);
        when(choice.getMessage()).thenReturn(message);

        ChatCompletions completions = mock(ChatCompletions.class);
        when(completions.getChoices()).thenReturn(List.of(choice));

        when(chatCompletionsClient.complete(any(ChatCompletionsOptions.class))).thenReturn(completions);
    }

    private FormData buildFormData() {
        FormData data = new FormData();
        data.setProfile("employee");
        data.setAge("35");
        data.setPostcode("75001");
        data.setCoverPartner(true);
        data.setCoverChildren(false);
        data.setNumberOfChildren(0);
        data.setOpticalNeeds("standard");
        data.setDentalNeeds("maintenance");
        data.setAlternativeMedicine("one_two");
        data.setHospitalisationPreference("private_preferred");
        data.setDoctorChoice("gp_specialist");
        return data;
    }
}
