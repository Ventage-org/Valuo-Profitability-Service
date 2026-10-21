package pe.ventage.valuo.profitability.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object para los indicadores de retorno financiero:
 * Cap Rate Unlevered, Gross Yield, Años de Retorno Simple (GRM) y Semáforo Benchmark BCRP.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class ReturnMetrics {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Column(name = "cap_rate_percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal capRatePercentage;

    @Column(name = "gross_yield_percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal grossYieldPercentage;

    @Column(name = "grm_years", nullable = false, precision = 5, scale = 2)
    private BigDecimal grossRentMultiplierYears;

    @Column(name = "net_payback_years", nullable = false, precision = 5, scale = 2)
    private BigDecimal netPaybackYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "benchmark_category", nullable = false, length = 20)
    private BenchmarkCategory benchmarkCategory;

    protected ReturnMetrics() {}

    public ReturnMetrics(BigDecimal capRatePercentage,
                         BigDecimal grossYieldPercentage,
                         BigDecimal grossRentMultiplierYears,
                         BigDecimal netPaybackYears,
                         BenchmarkCategory benchmarkCategory) {
        this.capRatePercentage = capRatePercentage;
        this.grossYieldPercentage = grossYieldPercentage;
        this.grossRentMultiplierYears = grossRentMultiplierYears;
        this.netPaybackYears = netPaybackYears;
        this.benchmarkCategory = benchmarkCategory;
    }

    /**
     * Constructor sobrecargado para retrocompatibilidad interna.
     */
    public ReturnMetrics(BigDecimal capRatePercentage,
                         BigDecimal grossYieldPercentage,
                         BigDecimal grossRentMultiplierYears,
                         BenchmarkCategory benchmarkCategory) {
        this(capRatePercentage, grossYieldPercentage, grossRentMultiplierYears,
                (capRatePercentage != null && capRatePercentage.compareTo(BigDecimal.ZERO) > 0)
                        ? HUNDRED.divide(capRatePercentage, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                benchmarkCategory);
    }

    /**
     * Calcula los indicadores de retorno financiero a partir del valor de venta, GSI y NOI.
     *
     * @param salePriceUsd Valor comercial de venta estimado en USD
     * @param gsiUsd       Ingreso bruto anual en USD
     * @param noiUsd       Ingreso operativo neto anual en USD
     * @param districtName Distrito del inmueble, para el benchmark BCRP (puede ser nulo)
     * @return Métricas de retorno financiero calculadas
     */
    public static ReturnMetrics calculate(BigDecimal salePriceUsd, BigDecimal gsiUsd, BigDecimal noiUsd, String districtName) {
        if (salePriceUsd == null || salePriceUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFinancialInputException("El valor comercial de venta debe ser mayor a 0");
        }
        if (gsiUsd == null || gsiUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFinancialInputException("El ingreso bruto anual (GSI) debe ser mayor a 0");
        }
        if (noiUsd == null || noiUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFinancialInputException("El ingreso operativo neto (NOI) debe ser mayor a 0");
        }

        // Cap Rate = (NOI / Venta) * 100
        BigDecimal capRate = noiUsd.divide(salePriceUsd, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(4, RoundingMode.HALF_UP);

        // Gross Yield = (GSI / Venta) * 100
        BigDecimal grossYield = gsiUsd.divide(salePriceUsd, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(4, RoundingMode.HALF_UP);

        // GRM (Años de alquiler bruto / Benchmark BCRP) = Venta / GSI
        BigDecimal grm = salePriceUsd.divide(gsiUsd, 2, RoundingMode.HALF_UP);

        // Payback Operativo Neto (Años de recuperación real descontando OpEx) = Venta / NOI
        BigDecimal netPayback = salePriceUsd.divide(noiUsd, 2, RoundingMode.HALF_UP);

        // Categoría Benchmark: Gross Yield del inmueble vs rendimiento bruto de mercado del distrito (BCRP PER)
        BenchmarkCategory category = BenchmarkCategory.classify(grossYield, districtName);

        return new ReturnMetrics(capRate, grossYield, grm, netPayback, category);
    }
}
