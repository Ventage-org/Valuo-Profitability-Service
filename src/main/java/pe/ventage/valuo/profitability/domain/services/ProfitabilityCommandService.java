package pe.ventage.valuo.profitability.domain.services;

import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;

import java.util.Optional;

/**
 * Servicio de dominio (puerto de entrada) para comandos de escritura de rentabilidad financiera.
 */
public interface ProfitabilityCommandService {

    /**
     * Procesa la intención de cálculo y persistencia del análisis de rentabilidad.
     *
     * @param command Comando con datos validados
     * @return Agregado de rentabilidad persistido
     */
    Optional<ProfitabilityCalculation> handle(CalculateProfitabilityCommand command);
}
