package com.fxai.alert.api;

import java.time.Instant;

record AlertApiErrorResponse(String code, String message, Instant timestamp) {
}
