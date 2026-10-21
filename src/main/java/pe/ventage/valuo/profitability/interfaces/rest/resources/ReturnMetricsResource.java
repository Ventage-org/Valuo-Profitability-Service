package pe.ventage.valuo.profitability.interfaces.rest.resources;

import java.math.BigDecimal;

/**
 * Sub-recurso de respuesta que detalla los indicadores de rendimiento financiero:
 * Cap Rate Unlevered, Gross Yield, Años de Retorno Simple (GRM) y Semáforo Benchmark BCRP.
 */
public record ReturnMetricsResource(
        BigDecimal capRatePorcentaje,
        BigDecimal grossYieldPorcentaje,
        BigDecimal aniosRetornoGrm,
        BigDecimal aniosPaybackNeto,
        String clasificacionBenchmark,
        String etiquetaBenchmark,
        String descripcionBenchmark
) {}
