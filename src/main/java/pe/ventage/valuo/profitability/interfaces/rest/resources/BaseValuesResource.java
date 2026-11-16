package pe.ventage.valuo.profitability.interfaces.rest.resources;

import java.math.BigDecimal;

/**
 * Sub-recurso que encapsula los valores base del inmueble y tipo de cambio al momento del análisis.
 */
public record BaseValuesResource(
        BigDecimal valorVentaUsd,
        BigDecimal alquilerMensualUsd,
        BigDecimal tipoCambioUsado,
        BigDecimal valorVentaPen,
        BigDecimal alquilerMensualPen,
        String distritoNombre
) {}
