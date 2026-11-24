package com.fxai.user;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserDataController {
    private final UserDataService service;
    public UserDataController(UserDataService service) { this.service = service; }

    @GetMapping("/watchlist")
    public UserDataService.Watchlist watchlist(@AuthenticationPrincipal Jwt user) {
        return service.watchlist(UUID.fromString(user.getSubject()));
    }
    @PutMapping("/watchlist")
    public UserDataService.Watchlist saveWatchlist(@AuthenticationPrincipal Jwt user, @Valid @RequestBody UserDataService.Watchlist state) {
        return service.saveWatchlist(UUID.fromString(user.getSubject()), state);
    }
    @GetMapping("/settings")
    public UserDataService.Settings settings(@AuthenticationPrincipal Jwt user) {
        return service.settings(UUID.fromString(user.getSubject()));
    }
    @PutMapping("/settings")
    public UserDataService.Settings saveSettings(@AuthenticationPrincipal Jwt user, @Valid @RequestBody UserDataService.Settings settings) {
        return service.saveSettings(UUID.fromString(user.getSubject()), settings);
    }
}
