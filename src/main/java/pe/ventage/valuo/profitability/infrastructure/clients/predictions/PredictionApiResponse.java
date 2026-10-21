package pe.ventage.valuo.profitability.infrastructure.clients.predictions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO remoto privado a la infraestructura que mapea la respuesta HTTP de msPrediccion.
 * Nunca se expone fuera del paquete clients/predictions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PredictionApiResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("tipoPrediccion") String tipoPrediccion,
        @JsonProperty("valorEstimadoUsd") BigDecimal valorEstimadoUsd,
        @JsonProperty("valorEstimadoPen") BigDecimal valorEstimadoPen,
        @JsonProperty("tipoCambioUsado") BigDecimal tipoCambioUsado,
        @JsonProperty("distritoNombre") String distritoNombre
) {}
