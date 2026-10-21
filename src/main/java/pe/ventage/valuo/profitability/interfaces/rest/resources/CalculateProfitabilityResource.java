package pe.ventage.valuo.profitability.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Recurso de entrada para solicitar el cálculo y persistencia del análisis de rentabilidad.
 * Soporta montos base en Soles (PEN) nominales provenientes de mlService o en Dólares (USD).
 */
public record CalculateProfitabilityResource(
        UUID inmuebleId,

        @NotNull(message = "El ID de la predicción de venta es obligatorio")
        UUID prediccionVentaId,

        @NotNull(message = "El ID de la predicción de alquiler es obligatorio")
        UUID prediccionAlquilerId,

        @Positive(message = "El valor estimado de venta en USD debe ser positivo")
        BigDecimal valorVentaUsd,

        @Positive(message = "El valor estimado de venta en PEN debe ser positivo")
        BigDecimal valorVentaPen,

        @Positive(message = "El canon mensual de alquiler en PEN debe ser positivo")
        BigDecimal alquilerMensualPen,

        @NotNull(message = "El ratio de gastos operativos (OER) es obligatorio")
        @DecimalMin(value = "0.0000", message = "El ratio OER no puede ser negativo")
        @DecimalMax(value = "0.3500", message = "El ratio OER no puede exceder 0.3500 (35%)")
        BigDecimal oerRatio,

        @Positive(message = "El tipo de cambio debe ser positivo")
        BigDecimal tipoCambioUsado,

        String distritoNombre,

        String observaciones
) {}
