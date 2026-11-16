package pe.ventage.valuo.profitability.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.ventage.valuo.profitability.domain.model.exceptions.ProfitabilityCalculationNotFoundException;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilityByIdQuery;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityQueryService;
import pe.ventage.valuo.profitability.interfaces.rest.resources.ProfitabilityCalculationResource;
import pe.ventage.valuo.profitability.interfaces.rest.transform.ProfitabilityCalculationResourceFromEntityAssembler;

import java.util.UUID;

/**
 * Controlador interno expuesto exclusivamente para la comunicación entre microservicios (/internal/v1/rentabilidad).
 * No debe ser publicado por el API Gateway. Consumido por msReportes para consolidar el PDF de tasación y rentabilidad.
 */
@RestController
@RequestMapping("/internal/v1/rentabilidad")
public class ProfitabilityInternalController {

    private final ProfitabilityQueryService queryService;

    public ProfitabilityInternalController(ProfitabilityQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Endpoint interno para obtener el snapshot del análisis de rentabilidad por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfitabilityCalculationResource> getByIdInternal(@PathVariable UUID id) {
        var calculation = queryService.handle(new GetProfitabilityByIdQuery(id))
                .orElseThrow(() -> new ProfitabilityCalculationNotFoundException(id));

        return ResponseEntity.ok(ProfitabilityCalculationResourceFromEntityAssembler.toResourceFromEntity(calculation));
    }
}
