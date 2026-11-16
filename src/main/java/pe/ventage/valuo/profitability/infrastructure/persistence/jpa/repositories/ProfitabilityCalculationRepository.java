package pe.ventage.valuo.profitability.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para el agregado ProfitabilityCalculation.
 */
@Repository
public interface ProfitabilityCalculationRepository extends
        JpaRepository<ProfitabilityCalculation, UUID>,
        JpaSpecificationExecutor<ProfitabilityCalculation> {

    /**
     * Recupera el cálculo de rentabilidad más reciente para un inmueble dado.
     */
    Optional<ProfitabilityCalculation> findTopByInmuebleIdOrderByCreatedAtDesc(UUID inmuebleId);

    /**
     * Recupera el historial completo de análisis de rentabilidad para un inmueble.
     */
    List<ProfitabilityCalculation> findAllByInmuebleIdOrderByCreatedAtDesc(UUID inmuebleId);

    /**
     * Recupera los análisis de rentabilidad generados por un agente inmobiliario.
     */
    List<ProfitabilityCalculation> findAllByAgenteIdOrderByCreatedAtDesc(UUID agenteId);
}
