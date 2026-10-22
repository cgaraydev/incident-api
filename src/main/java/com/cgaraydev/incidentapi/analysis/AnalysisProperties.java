package com.cgaraydev.incidentapi.analysis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.analysis")
public record AnalysisProperties(
        @DefaultValue("false") boolean enabled,
        String model,
        @DefaultValue("20s") Duration timeout,
        @DefaultValue("512") int maxOutputTokens
) {

    public AnalysisProperties {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Analysis timeout must be positive"
            );
        }

        if (maxOutputTokens < 1 || maxOutputTokens > 2048) {
            throw new IllegalArgumentException(
                    "Analysis maxOutputTokens must be between 1 and 2048"
            );
        }

        if (enabled && (model == null || model.isBlank())) {
            throw new IllegalArgumentException(
                    "Analysis model must be configured when analysis is enabled"
            );
        }
    }
}