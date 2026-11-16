package pe.ventage.valuo.profitability.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.queries.GetLatestProfitabilityByInmuebleIdQuery;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilitiesByInmuebleIdQuery;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilityByIdQuery;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityQueryService;
import pe.ventage.valuo.profitability.infrastructure.persistence.jpa.repositories.ProfitabilityCalculationRepository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de consultas de rentabilidad.
 * Todas las operaciones son de solo lectura (@Transactional(readOnly = true)).
 */
@Service
@Transactional(readOnly = true)
public class ProfitabilityQueryServiceImpl implements ProfitabilityQueryService {

    private final ProfitabilityCalculationRepository repository;

    public ProfitabilityQueryServiceImpl(ProfitabilityCalculationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProfitabilityCalculation> handle(GetProfitabilityByIdQuery query) {
        if (query == null || query.id() == null) {
            return Optional.empty();
        }
        return repository.findById(query.id());
    }

    @Override
    public Optional<ProfitabilityCalculation> handle(GetLatestProfitabilityByInmuebleIdQuery query) {
        if (query == null || query.inmuebleId() == null) {
            return Optional.empty();
        }
        return repository.findTopByInmuebleIdOrderByCreatedAtDesc(query.inmuebleId());
    }

    @Override
    public List<ProfitabilityCalculation> handle(GetProfitabilitiesByInmuebleIdQuery query) {
        if (query == null || query.inmuebleId() == null) {
            return List.of();
        }
        return repository.findAllByInmuebleIdOrderByCreatedAtDesc(query.inmuebleId());
    }
}
