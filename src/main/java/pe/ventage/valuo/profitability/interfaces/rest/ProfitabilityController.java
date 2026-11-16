package pe.ventage.valuo.profitability.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;
import pe.ventage.valuo.profitability.domain.model.exceptions.ProfitabilityCalculationNotFoundException;
import pe.ventage.valuo.profitability.domain.model.queries.GetLatestProfitabilityByInmuebleIdQuery;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilitiesByInmuebleIdQuery;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilityByIdQuery;
import pe.ventage.valuo.profitability.domain.model.valueobjects.FinancialCascade;
import pe.ventage.valuo.profitability.domain.model.valueobjects.OerRatio;
import pe.ventage.valuo.profitability.domain.model.valueobjects.ReturnMetrics;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityCommandService;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityQueryService;
import pe.ventage.valuo.profitability.interfaces.rest.resources.*;
import pe.ventage.valuo.profitability.interfaces.rest.transform.CalculateProfitabilityCommandFromResourceAssembler;
import pe.ventage.valuo.profitability.interfaces.rest.transform.ProfitabilityCalculationResourceFromEntityAssembler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST público expuesto a través del API Gateway (/api/v1/rentabilidad).
 * Controlador delgado: recibe Resources, delega en servicios de aplicación y retorna DTOs.
 */
@RestController
@RequestMapping("/api/v1/rentabilidad")
public class ProfitabilityController {

    private final ProfitabilityCommandService commandService;
    private final ProfitabilityQueryService queryService;

    public ProfitabilityController(ProfitabilityCommandService commandService,
                                   ProfitabilityQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /**
     * Registra y persiste un análisis de rentabilidad financiera combinando Venta y Alquiler.
     */
    @PostMapping
    public ResponseEntity<ProfitabilityCalculationResource> calculate(
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @Valid @RequestBody CalculateProfitabilityResource resource) {

        UUID agenteId = headerUserId != null ? headerUserId : UUID.randomUUID();
        var command = CalculateProfitabilityCommandFromResourceAssembler.toCommandFromResource(agenteId, resource);

        ProfitabilityCalculation calculation = commandService.handle(command)
                .orElseThrow(() -> new IllegalStateException("No se pudo procesar el análisis de rentabilidad"));

        var responseBody = ProfitabilityCalculationResourceFromEntityAssembler.toResourceFromEntity(calculation);
        URI location = URI.create("/api/v1/rentabilidad/" + responseBody.id());

        return ResponseEntity.created(location).body(responseBody);
    }

    /**
     * Simulación instantánea para el Slider OER de la interfaz de usuario en tiempo real.
     * Cálculo puro en memoria sin acceder ni persistir en la base de datos (< 5ms).
     */
    @PostMapping("/simular")
    public ResponseEntity<ProfitabilityCalculationResource> simulate(
            @Valid @RequestBody SimulateSensitivityResource resource) {

        BigDecimal tc = (resource.tipoCambioUsado() != null && resource.tipoCambioUsado().compareTo(BigDecimal.ZERO) > 0)
                ? resource.tipoCambioUsado()
                : ProfitabilityCalculation.DEFAULT_EXCHANGE_RATE;

        BigDecimal ventaUsd;
        BigDecimal ventaPen;
        if (resource.valorVentaPen() != null && resource.valorVentaPen().compareTo(BigDecimal.ZERO) > 0) {
            ventaPen = resource.valorVentaPen().setScale(2, RoundingMode.HALF_UP);
            ventaUsd = ventaPen.divide(tc, 2, RoundingMode.HALF_UP);
        } else if (resource.valorVentaUsd() != null) {
            ventaUsd = resource.valorVentaUsd().setScale(2, RoundingMode.HALF_UP);
            ventaPen = ventaUsd.multiply(tc).setScale(2, RoundingMode.HALF_UP);
        } else {
            throw new InvalidFinancialInputException("Se requiere el valor de venta en USD o PEN para la simulación");
        }

        BigDecimal alquilerPen;
        BigDecimal alquilerUsd;
        if (resource.alquilerMensualPen() != null && resource.alquilerMensualPen().compareTo(BigDecimal.ZERO) > 0) {
            alquilerPen = resource.alquilerMensualPen().setScale(2, RoundingMode.HALF_UP);
            alquilerUsd = alquilerPen.divide(tc, 2, RoundingMode.HALF_UP);
        } else {
            throw new InvalidFinancialInputException("Se requiere el canon de alquiler en PEN para la simulación");
        }

        var oer = OerRatio.of(resource.oerRatio());
        var cascade = FinancialCascade.calculate(alquilerUsd, oer, tc);
        var metrics = ReturnMetrics.calculate(ventaUsd, cascade.getGrossScheduledIncomeUsd(), cascade.getNetOperatingIncomeUsd(), resource.distritoNombre());
        var benchmark = metrics.getBenchmarkCategory();

        var baseValues = new BaseValuesResource(
                ventaUsd,
                alquilerUsd,
                tc,
                ventaPen,
                alquilerPen,
                resource.distritoNombre()
        );

        var cascadeResource = new FinancialCascadeResource(
                cascade.getGrossScheduledIncomeUsd(),
                oer.toPercentage(),
                cascade.getOperatingExpensesUsd(),
                cascade.getNetOperatingIncomeUsd(),
                cascade.getGrossScheduledIncomePen(),
                cascade.getNetOperatingIncomePen()
        );

        var metricsResource = new ReturnMetricsResource(
                metrics.getCapRatePercentage(),
                metrics.getGrossYieldPercentage(),
                metrics.getGrossRentMultiplierYears(),
                metrics.getNetPaybackYears(),
                benchmark.name(),
                benchmark.getEtiqueta(),
                benchmark.getDescripcion()
        );

        var simulatedResponse = new ProfitabilityCalculationResource(
                null,
                null,
                null,
                null,
                null,
                baseValues,
                cascadeResource,
                metricsResource,
                ProfitabilityCalculation.AVISO_NORMATIVO_LEGAL,
                "Simulación en memoria de sensibilidad OER",
                null
        );

        return ResponseEntity.ok(simulatedResponse);
    }

    /**
     * Consulta el detalle de un cálculo de rentabilidad por su ID único.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfitabilityCalculationResource> getById(@PathVariable UUID id) {
        var calculation = queryService.handle(new GetProfitabilityByIdQuery(id))
                .orElseThrow(() -> new ProfitabilityCalculationNotFoundException(id));

        return ResponseEntity.ok(ProfitabilityCalculationResourceFromEntityAssembler.toResourceFromEntity(calculation));
    }

    /**
     * Consulta el cálculo de rentabilidad más reciente registrado para un inmueble.
     */
    @GetMapping("/inmueble/{inmuebleId}/ultimo")
    public ResponseEntity<ProfitabilityCalculationResource> getLatestByInmueble(@PathVariable UUID inmuebleId) {
        var calculation = queryService.handle(new GetLatestProfitabilityByInmuebleIdQuery(inmuebleId))
                .orElseThrow(() -> new ProfitabilityCalculationNotFoundException(
                        "No se encontró un análisis de rentabilidad para el inmueble ID: " + inmuebleId));

        return ResponseEntity.ok(ProfitabilityCalculationResourceFromEntityAssembler.toResourceFromEntity(calculation));
    }

    /**
     * Consulta el historial completo de análisis de rentabilidad generados para un inmueble.
     */
    @GetMapping("/inmueble/{inmuebleId}")
    public ResponseEntity<List<ProfitabilityCalculationResource>> getAllByInmueble(@PathVariable UUID inmuebleId) {
        var calculations = queryService.handle(new GetProfitabilitiesByInmuebleIdQuery(inmuebleId));
        var resources = calculations.stream()
                .map(ProfitabilityCalculationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }
}
