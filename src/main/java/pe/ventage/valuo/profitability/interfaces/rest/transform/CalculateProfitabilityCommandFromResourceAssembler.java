package pe.ventage.valuo.profitability.interfaces.rest.transform;

import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;
import pe.ventage.valuo.profitability.interfaces.rest.resources.CalculateProfitabilityResource;

import java.util.UUID;

/**
 * Assembler estático para transformar CalculateProfitabilityResource a CalculateProfitabilityCommand.
 * Cero lógica de negocio, solo mapeo de campos.
 */
public final class CalculateProfitabilityCommandFromResourceAssembler {

    private CalculateProfitabilityCommandFromResourceAssembler() {}

    public static CalculateProfitabilityCommand toCommandFromResource(UUID agenteId, CalculateProfitabilityResource resource) {
        return new CalculateProfitabilityCommand(
                agenteId,
                resource.inmuebleId(),
                resource.prediccionVentaId(),
                resource.prediccionAlquilerId(),
                resource.valorVentaUsd(),
                null,
                resource.valorVentaPen(),
                resource.alquilerMensualPen(),
                resource.oerRatio(),
                resource.tipoCambioUsado(),
                resource.distritoNombre(),
                resource.observaciones()
        );
    }
}
