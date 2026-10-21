package pe.ventage.valuo.profitability.interfaces.rest.transform;

import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.interfaces.rest.resources.BaseValuesResource;
import pe.ventage.valuo.profitability.interfaces.rest.resources.FinancialCascadeResource;
import pe.ventage.valuo.profitability.interfaces.rest.resources.ProfitabilityCalculationResource;
import pe.ventage.valuo.profitability.interfaces.rest.resources.ReturnMetricsResource;

/**
 * Assembler estático para transformar el Agregado ProfitabilityCalculation a su Resource público.
 * Cero lógica de negocio, solo mapeo de campos.
 */
public final class ProfitabilityCalculationResourceFromEntityAssembler {

    private ProfitabilityCalculationResourceFromEntityAssembler() {}

    public static ProfitabilityCalculationResource toResourceFromEntity(ProfitabilityCalculation entity) {
        if (entity == null) {
            return null;
        }

        var baseValues = new BaseValuesResource(
                entity.getValorVentaUsd(),
                entity.getAlquilerMensualUsd(),
                entity.getTipoCambioUsado(),
                entity.getValorVentaPen(),
                entity.getAlquilerMensualPen(),
                entity.getDistritoNombre()
        );

        var cascade = entity.getFinancialCascade();
        var cascadeResource = new FinancialCascadeResource(
                cascade.getGrossScheduledIncomeUsd(),
                entity.getOerRatio().toPercentage(),
                cascade.getOperatingExpensesUsd(),
                cascade.getNetOperatingIncomeUsd(),
                cascade.getGrossScheduledIncomePen(),
                cascade.getNetOperatingIncomePen()
        );

        var metrics = entity.getReturnMetrics();
        var benchmark = metrics.getBenchmarkCategory();
        var metricsResource = new ReturnMetricsResource(
                metrics.getCapRatePercentage(),
                metrics.getGrossYieldPercentage(),
                metrics.getGrossRentMultiplierYears(),
                metrics.getNetPaybackYears(),
                benchmark.name(),
                benchmark.getEtiqueta(),
                benchmark.getDescripcion()
        );

        return new ProfitabilityCalculationResource(
                entity.getId(),
                entity.getAgenteId(),
                entity.getInmuebleId(),
                entity.getPrediccionVentaId(),
                entity.getPrediccionAlquilerId(),
                baseValues,
                cascadeResource,
                metricsResource,
                entity.getAvisoNormativo(),
                entity.getObservacionesSupuestos(),
                entity.getCreatedAt()
        );
    }
}
