package com.fxai.market.provider.oanda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OandaCandlestickDataDto(
        @JsonProperty("o") String open,
        @JsonProperty("h") String high,
        @JsonProperty("l") String low,
        @JsonProperty("c") String close) {}
