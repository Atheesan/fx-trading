package com.fxai.market.provider.oanda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OandaCandlesResponse(String instrument, String granularity, List<OandaCandleDto> candles) {}
