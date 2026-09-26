package com.codereviewer.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiApiClientTest {

    private static final String ENDPOINT = "http://10.0.35.2:4000/chat/completions";

    @Test
    void shouldSendExpectedRequestAndReturnOnlyVisibleAssistantContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiApiClient client = new AiApiClient(properties(null), builder);
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(content().json("""
                        {
                          "model": "zai-org/GLM-5.3",
                          "messages": [{"role": "user", "content": "Review this diff"}]
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "choices": [{
                            "index": 0,
                            "message": {
                              "role": "assistant",
                              "content": "Visible review",
                              "reasoning_content": "Hidden reasoning"
                            }
                          }],
                          "usage": {"total_tokens": 42}
                        }
                        """, MediaType.APPLICATION_JSON));

        String response = client.complete("Review this diff");

        assertThat(response).isEqualTo("Visible review");
        server.verify();
    }

    @Test
    void shouldSendOptionalBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiApiClient client = new AiApiClient(properties("secret"), builder);
        server.expect(requestTo(ENDPOINT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.complete("prompt")).isEqualTo("ok");
        server.verify();
    }

    @Test
    void shouldRejectResponseWithoutAssistantContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiApiClient client = new AiApiClient(properties(null), builder);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("prompt"))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI API returned no assistant content");
    }

    @Test
    void shouldWrapNonSuccessfulResponseWithoutExposingItsBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiApiClient client = new AiApiClient(properties(null), builder);
        server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

        assertThatThrownBy(() -> client.complete("prompt"))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI API call failed with status 500");
    }

    private AiProperties properties(String token) {
        return new AiProperties(ENDPOINT, "zai-org/GLM-5.3", token, 50_000);
    }
}
