package com.fxai.alert.api;

import com.fxai.alert.service.PriceAlertService;
import jakarta.validation.Valid;
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
	public PriceAlertResponse create(@Valid @RequestBody CreatePriceAlertRequest request) {
		return PriceAlertResponse.from(priceAlertService.create(
				request.symbol(),
				request.condition(),
				request.targetPrice()
		));
	}

	@GetMapping
	public List<PriceAlertResponse> findAll() {
		return priceAlertService.findAll().stream()
				.map(PriceAlertResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		priceAlertService.delete(id);
	}
}
