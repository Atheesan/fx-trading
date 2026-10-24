package com.fxai.alert.api;

import com.fxai.alert.domain.AlertCondition;
import com.fxai.alert.domain.PriceAlert;
import com.fxai.alert.service.PriceAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTests {
	private static final UUID ID = UUID.fromString("9bf3de5e-8176-4c91-bb47-9bf5f4aabbe8");
	private static final Instant CREATED_AT = Instant.parse("2026-01-02T03:04:05Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PriceAlertService priceAlertService;

	@Test
	void createsPriceAlert() throws Exception {
		PriceAlert alert = PriceAlert.active(
				ID,
				"EURUSD",
				AlertCondition.PRICE_ABOVE,
				new BigDecimal("1.12500"),
				CREATED_AT
		);
		when(priceAlertService.create(
				"EURUSD",
				AlertCondition.PRICE_ABOVE,
				new BigDecimal("1.12500")
		)).thenReturn(alert);

		mockMvc.perform(post("/api/v1/alerts")
					.contentType("application/json")
					.content("""
							{
							  "symbol": "EURUSD",
							  "condition": "PRICE_ABOVE",
							  "targetPrice": 1.12500
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(ID.toString()))
				.andExpect(jsonPath("$.symbol").value("EURUSD"))
				.andExpect(jsonPath("$.condition").value("PRICE_ABOVE"))
				.andExpect(jsonPath("$.targetPrice").value(1.12500))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-02T03:04:05Z"))
				.andExpect(jsonPath("$.triggeredAt").doesNotExist());
	}

	@Test
	void listsAlerts() throws Exception {
		when(priceAlertService.findAll()).thenReturn(List.of(PriceAlert.active(
				ID,
				"USDJPY",
				AlertCondition.PRICE_BELOW,
				new BigDecimal("149.50"),
				CREATED_AT
		)));

		mockMvc.perform(get("/api/v1/alerts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(ID.toString()))
				.andExpect(jsonPath("$[0].condition").value("PRICE_BELOW"));
	}

	@Test
	void deletesAlert() throws Exception {
		mockMvc.perform(delete("/api/v1/alerts/{id}", ID))
				.andExpect(status().isNoContent());

		verify(priceAlertService).delete(ID);
	}

	@Test
	void rejectsNonPositiveTargetPrice() throws Exception {
		mockMvc.perform(post("/api/v1/alerts")
					.contentType("application/json")
					.content("""
							{
							  "symbol": "EURUSD",
							  "condition": "PRICE_ABOVE",
							  "targetPrice": 0
							}
							"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsNotFoundWhenDeletingUnknownAlert() throws Exception {
		org.mockito.Mockito.doThrow(new AlertNotFoundException(ID))
				.when(priceAlertService).delete(ID);

		mockMvc.perform(delete("/api/v1/alerts/{id}", ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ALERT_NOT_FOUND"));
	}
}
