package com.tradeops.model.response;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ErrorDetail(
        String code,
        String message,
        Map<String, List<String>> fields,
        @com.fasterxml.jackson.annotation.JsonProperty("trace_id")
        String traceId
){}
