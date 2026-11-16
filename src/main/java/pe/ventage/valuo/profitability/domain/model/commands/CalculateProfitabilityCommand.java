package pe.ventage.valuo.profitability.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de intención de escritura para registrar un cálculo de rentabilidad financiera.
 * Soporta montos base en Soles (PEN) nominales o en Dólares (USD), unificando el tipo de cambio.
 * Record inmutable según directivas CQRS.
 */
public record CalculateProfitabilityCommand(
        UUID agenteId,
        UUID inmuebleId,
        UUID prediccionVentaId,
        UUID prediccionAlquilerId,
        BigDecimal valorVentaUsd,
        BigDecimal alquilerMensualUsd,
        BigDecimal valorVentaPen,
        BigDecimal alquilerMensualPen,
        BigDecimal oerRatio,
        BigDecimal tipoCambioUsado,
        String distritoNombre,
        String observaciones
) {
    /**
     * Constructor sobrecargado para retrocompatibilidad con llamadas existentes (sin montos PEN explícitos).
     */
    public CalculateProfitabilityCommand(
            UUID agenteId,
            UUID inmuebleId,
            UUID prediccionVentaId,
            UUID prediccionAlquilerId,
            BigDecimal valorVentaUsd,
            BigDecimal alquilerMensualUsd,
            BigDecimal oerRatio,
            BigDecimal tipoCambioUsado,
            String distritoNombre,
            String observaciones
    ) {
        this(agenteId, inmuebleId, prediccionVentaId, prediccionAlquilerId,
                valorVentaUsd, alquilerMensualUsd, null, null,
                oerRatio, tipoCambioUsado, distritoNombre, observaciones);
    }
}
