package pe.ventage.valuo.profitability.application.internal.commandservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ventage.valuo.profitability.application.internal.outboundservices.ExternalPredictionsService;
import pe.ventage.valuo.profitability.application.internal.outboundservices.PredictionSnapshot;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityCommandService;
import pe.ventage.valuo.profitability.infrastructure.persistence.jpa.repositories.ProfitabilityCalculationRepository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Implementación del servicio de comandos de rentabilidad.
 * Orquesta la validación con servicios externos, ejecución de invariantes del agregado y persistencia.
 */
@Service
public class ProfitabilityCommandServiceImpl implements ProfitabilityCommandService {

    private static final Logger log = LoggerFactory.getLogger(ProfitabilityCommandServiceImpl.class);

    private final ProfitabilityCalculationRepository repository;
    private final ExternalPredictionsService externalPredictionsService;

    public ProfitabilityCommandServiceImpl(ProfitabilityCalculationRepository repository,
                                           ExternalPredictionsService externalPredictionsService) {
        this.repository = repository;
        this.externalPredictionsService = externalPredictionsService;
    }

    @Override
    @Transactional
    public Optional<ProfitabilityCalculation> handle(CalculateProfitabilityCommand command) {
        if (command == null) {
            throw new InvalidFinancialInputException("El comando no puede ser nulo");
        }

        BigDecimal valorVentaUsd = command.valorVentaUsd();
        BigDecimal alquilerMensualUsd = command.alquilerMensualUsd();
        BigDecimal valorVentaPen = command.valorVentaPen();
        BigDecimal alquilerMensualPen = command.alquilerMensualPen();
        BigDecimal tipoCambioUsado = (command.tipoCambioUsado() != null && command.tipoCambioUsado().compareTo(BigDecimal.ZERO) > 0)
                ? command.tipoCambioUsado()
                : ProfitabilityCalculation.DEFAULT_EXCHANGE_RATE;
        String distritoNombre = command.distritoNombre();

        // Enriquecimiento / Validación opcional desde msPrediccion si algún valor no vino precargado
        if ((valorVentaUsd == null && valorVentaPen == null) || distritoNombre == null) {
            log.info("Consultando snapshot de predicción de venta ID: {}", command.prediccionVentaId());
            PredictionSnapshot ventaSnapshot = externalPredictionsService.findPredictionById(command.prediccionVentaId())
                    .orElseThrow(() -> new InvalidFinancialInputException(
                            "No se encontró la predicción de venta remota con ID: " + command.prediccionVentaId()));

            if (valorVentaUsd == null && valorVentaPen == null) {
                valorVentaUsd = ventaSnapshot.valorEstimadoUsd();
                valorVentaPen = ventaSnapshot.valorEstimadoPen();
            }
            if (distritoNombre == null) distritoNombre = ventaSnapshot.distritoNombre();
        }

        if (alquilerMensualUsd == null && alquilerMensualPen == null) {
            log.info("Consultando snapshot de predicción de alquiler ID: {}", command.prediccionAlquilerId());
            PredictionSnapshot alquilerSnapshot = externalPredictionsService.findPredictionById(command.prediccionAlquilerId())
                    .orElseThrow(() -> new InvalidFinancialInputException(
                            "No se encontró la predicción de alquiler remota con ID: " + command.prediccionAlquilerId()));

            alquilerMensualUsd = alquilerSnapshot.valorEstimadoUsd();
            alquilerMensualPen = alquilerSnapshot.valorEstimadoPen();
        }

        // Construir el comando enriquecido final
        CalculateProfitabilityCommand finalCommand = new CalculateProfitabilityCommand(
                command.agenteId(),
                command.inmuebleId(),
                command.prediccionVentaId(),
                command.prediccionAlquilerId(),
                valorVentaUsd,
                alquilerMensualUsd,
                valorVentaPen,
                alquilerMensualPen,
                command.oerRatio(),
                tipoCambioUsado,
                distritoNombre,
                command.observaciones()
        );

        // El dominio manda: el agregado valida invariantes y calcula la cascada y métricas
        ProfitabilityCalculation calculation = new ProfitabilityCalculation(finalCommand);
        ProfitabilityCalculation saved = repository.save(calculation);

        log.info("Cálculo de rentabilidad guardado exitosamente con ID: {}, Cap Rate: {}%",
                saved.getId(), saved.getReturnMetrics().getCapRatePercentage());

        return Optional.of(saved);
    }
}
