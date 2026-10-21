package pe.ventage.valuo.profitability.domain.model.exceptions;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando un cálculo de rentabilidad no existe.
 */
public class ProfitabilityCalculationNotFoundException extends RuntimeException {

    public ProfitabilityCalculationNotFoundException(UUID id) {
        super("No se encontró el cálculo de rentabilidad con ID: " + id);
    }

    public ProfitabilityCalculationNotFoundException(String message) {
        super(message);
    }
}
