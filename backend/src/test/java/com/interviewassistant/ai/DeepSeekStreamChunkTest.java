package com.interviewassistant.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekStreamChunkTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStreamChunkWithProviderMetadata() throws Exception {
        String json = """
                {
                  "id": "abc",
                  "object": "chat.completion.chunk",
                  "created": 1710000000,
                  "model": "deepseek-v4-pro",
                  "choices": [
                    {
                      "index": 0,
                      "delta": {
                        "role": "assistant",
                        "content": "你好"
                      },
                      "logprobs": null,
                      "finish_reason": null
                    }
                  ]
                }
                """;

        DeepSeekStreamChunk chunk = objectMapper.readValue(json, DeepSeekStreamChunk.class);

        assertThat(chunk.getChoices()).hasSize(1);
        assertThat(chunk.getChoices().get(0).getDelta().getContent()).isEqualTo("你好");
    }
}
