package com.fxai.market.api;

import java.time.Instant;

record ApiErrorResponse(String code, String message, Instant timestamp) {
}
