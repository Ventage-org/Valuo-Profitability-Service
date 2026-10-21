package pe.ventage.valuo.profitability.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Recurso de entrada para simulación en vivo de sliders (OER y Tipo de Cambio) en Angular (sin persistir en BD).
 * Acepta base en USD o en Soles (PEN) nominales.
 */
public record SimulateSensitivityResource(
        @Positive(message = "El valor de venta en USD debe ser positivo")
        BigDecimal valorVentaUsd,

        @Positive(message = "El valor de venta en PEN debe ser positivo")
        BigDecimal valorVentaPen,

        @NotNull(message = "El canon de alquiler mensual en PEN es obligatorio")
        @Positive(message = "El canon de alquiler mensual en PEN debe ser positivo")
        BigDecimal alquilerMensualPen,

        @NotNull(message = "El ratio OER es obligatorio")
        @DecimalMin(value = "0.0000", message = "El ratio OER no puede ser negativo")
        @DecimalMax(value = "0.3500", message = "El ratio OER no puede superar 0.3500")
        BigDecimal oerRatio,

        @NotNull(message = "El tipo de cambio es obligatorio")
        @Positive(message = "El tipo de cambio debe ser positivo")
        BigDecimal tipoCambioUsado,

        String distritoNombre
) {}
