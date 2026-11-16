package pe.ventage.valuo.profitability.application.internal.outboundservices;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Modelo propio de la capa de aplicación para representar el snapshot de una predicción
 * obtenida desde el microservicio remoto msPrediccion.
 * Soporta bimoneda (PEN nominales y USD).
 */
public record PredictionSnapshot(
        UUID id,
        String tipoPrediccion,
        BigDecimal valorEstimadoUsd,
        BigDecimal valorEstimadoPen,
        BigDecimal tipoCambioUsado,
        String distritoNombre
) {
    public PredictionSnapshot(UUID id, String tipoPrediccion, BigDecimal valorEstimadoUsd, BigDecimal tipoCambioUsado, String distritoNombre) {
        this(id, tipoPrediccion, valorEstimadoUsd, null, tipoCambioUsado, distritoNombre);
    }
}
