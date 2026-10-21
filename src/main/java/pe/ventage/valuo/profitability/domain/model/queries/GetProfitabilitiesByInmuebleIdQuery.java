package pe.ventage.valuo.profitability.domain.model.queries;

import java.util.UUID;

/**
 * Query para listar el historial de análisis de rentabilidad asociados a un inmueble.
 */
public record GetProfitabilitiesByInmuebleIdQuery(UUID inmuebleId) {}
