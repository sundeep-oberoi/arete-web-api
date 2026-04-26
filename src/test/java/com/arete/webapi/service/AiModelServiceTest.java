package com.arete.webapi.service;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.ai.PremiumResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiModelServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient openAiClient;

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
        ReflectionTestUtils.setField(service, "modelName", "test-model");
        ReflectionTestUtils.setField(service, "openAiClient", openAiClient);
    }

    @Test
    void fetchPremium_throwsWhenClientNotInitialised() {
        ReflectionTestUtils.setField(service, "openAiClient", null);
        assertThatThrownBy(() -> service.fetchPremium(new FormData()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MODEL_API_URL");
    }

    @Test
    void fetchPremium_callsClientAndReturnsParsedPremium() {
        mockClientResponse("{\"monthlyPremium\": 85.5, \"annualPremium\": 1026.0}");

        PremiumResult result = service.fetchPremium(buildFormData());

        assertThat(result.monthlyPremium()).isEqualTo(85.5);
        assertThat(result.annualPremium()).isEqualTo(1026.0);
        verify(openAiClient.chat().completions()).create(any(ChatCompletionCreateParams.class));
    }

    @Test
    void fetchPremium_throwsOnEmptyChoices() {
        ChatCompletion completion = mock(ChatCompletion.class);
        when(completion.choices()).thenReturn(List.of());
        when(openAiClient.chat().completions().create(any(ChatCompletionCreateParams.class))).thenReturn(completion);

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
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);
        when(message.content()).thenReturn(Optional.of(content));

        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        when(choice.message()).thenReturn(message);

        ChatCompletion completion = mock(ChatCompletion.class);
        when(completion.choices()).thenReturn(List.of(choice));

        when(openAiClient.chat().completions().create(any(ChatCompletionCreateParams.class))).thenReturn(completion);
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
