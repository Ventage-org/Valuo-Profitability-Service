package pe.ventage.valuo.profitability.domain.model.queries;

import java.util.UUID;

/**
 * Query para obtener un cálculo de rentabilidad por su identificador único.
 */
public record GetProfitabilityByIdQuery(UUID id) {}
