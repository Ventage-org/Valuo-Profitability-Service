package pe.ventage.valuo.profitability.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

/**
 * Recurso de salida principal para el análisis de rentabilidad financiera.
 * Utilizado por el frontend Angular y por el microservicio de reportes PDF.
 */
public record ProfitabilityCalculationResource(
        UUID id,
        UUID agenteId,
        UUID inmuebleId,
        UUID prediccionVentaId,
        UUID prediccionAlquilerId,
        BaseValuesResource valoresBase,
        FinancialCascadeResource cascadaFinanciera,
        ReturnMetricsResource metricasRetorno,
        String avisoNormativo,
        String observaciones,
        Instant createdAt
) {}
