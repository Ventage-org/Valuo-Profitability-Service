package pe.ventage.valuo.profitability.interfaces.rest.resources;

import java.math.BigDecimal;

/**
 * Sub-recurso de respuesta que detalla la cascada operativa:
 * GSI -> OpEx -> NOI (en USD y PEN).
 */
public record FinancialCascadeResource(
        BigDecimal ingresoBrutoAnualUsd,
        BigDecimal oerPorcentaje,
        BigDecimal gastosOperativosAnualesUsd,
        BigDecimal noiAnualUsd,
        BigDecimal ingresoBrutoAnualPen,
        BigDecimal noiAnualPen
) {}
