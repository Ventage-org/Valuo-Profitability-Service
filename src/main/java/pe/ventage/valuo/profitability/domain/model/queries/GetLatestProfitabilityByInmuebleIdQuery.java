package pe.ventage.valuo.profitability.domain.model.queries;

import java.util.UUID;

/**
 * Query para obtener el cálculo de rentabilidad más reciente asociado a un inmueble.
 */
public record GetLatestProfitabilityByInmuebleIdQuery(UUID inmuebleId) {}
