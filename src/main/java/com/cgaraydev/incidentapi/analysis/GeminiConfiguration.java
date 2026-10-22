package com.cgaraydev.incidentapi.analysis;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "app.analysis.enabled",
        havingValue = "true"
)
public class GeminiConfiguration {

    @Bean(destroyMethod = "close")
    public Client geminiClient(
            AnalysisProperties properties,
            @Value("${GOOGLE_API_KEY:}") String apiKey
    ) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_API_KEY must be configured when analysis is enabled"
            );
        }

        int timeoutMillis = Math.toIntExact(
                properties.timeout().toMillis()
        );

        if (timeoutMillis < 1) {
            throw new IllegalStateException(
                    "Analysis timeout must be at least one millisecond"
            );
        }

        HttpOptions httpOptions = HttpOptions.builder()
                .timeout(timeoutMillis)
                .retryOptions(
                        HttpRetryOptions.builder()
                                .attempts(1)
                                .build()
                )
                .build();

        return Client.builder()
                .vertexAI(false)
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();
    }
}