package com.fxai.market.provider.oanda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OandaClientPriceDto(
        String instrument, String time, String status, boolean tradeable,
        List<OandaPriceBucketDto> bids, List<OandaPriceBucketDto> asks) {}
