package com.fxai.market.provider.oanda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OandaCandleDto(String time, OandaCandlestickDataDto mid, long volume, boolean complete) {}
