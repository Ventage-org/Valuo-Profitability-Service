package pe.ventage.valuo.profitability.domain.services;

import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.queries.GetLatestProfitabilityByInmuebleIdQuery;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilitiesByInmuebleIdQuery;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilityByIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio (puerto de entrada) para consultas de lectura de rentabilidad financiera.
 * Métodos puros de lectura sin efectos secundarios.
 */
public interface ProfitabilityQueryService {

    /**
     * Obtiene un cálculo de rentabilidad por su ID único.
     */
    Optional<ProfitabilityCalculation> handle(GetProfitabilityByIdQuery query);

    /**
     * Obtiene el cálculo de rentabilidad más reciente asociado a un inmueble.
     */
    Optional<ProfitabilityCalculation> handle(GetLatestProfitabilityByInmuebleIdQuery query);

    /**
     * Lista el historial completo de análisis de rentabilidad generados para un inmueble.
     */
    List<ProfitabilityCalculation> handle(GetProfitabilitiesByInmuebleIdQuery query);
}
