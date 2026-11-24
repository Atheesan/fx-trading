package com.fxai.alert.api;

import com.fxai.alert.service.PriceAlertService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
	private final PriceAlertService priceAlertService;

	public AlertController(PriceAlertService priceAlertService) {
		this.priceAlertService = priceAlertService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PriceAlertResponse create(@AuthenticationPrincipal Jwt user, @Valid @RequestBody CreatePriceAlertRequest request) {
		return PriceAlertResponse.from(priceAlertService.create(
                UUID.fromString(user.getSubject()),
				request.symbol(),
				request.condition(),
				request.targetPrice()
		));
	}

	@GetMapping
	public List<PriceAlertResponse> findAll(@AuthenticationPrincipal Jwt user) {
		return priceAlertService.findAll(UUID.fromString(user.getSubject())).stream()
				.map(PriceAlertResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt user) {
		priceAlertService.delete(id, UUID.fromString(user.getSubject()));
	}
}
