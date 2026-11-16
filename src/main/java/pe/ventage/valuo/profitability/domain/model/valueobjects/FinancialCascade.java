package pe.ventage.valuo.profitability.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object que modela la cascada financiera del flujo operativo unlevered.
 * GSI (Gross Scheduled Income) -> OpEx (Operating Expenses) -> NOI (Net Operating Income)
 * en USD y con su contraparte en Soles (PEN).
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class FinancialCascade {

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    @Column(name = "annual_gross_income_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossScheduledIncomeUsd;

    @Column(name = "annual_operating_expenses_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal operatingExpensesUsd;

    @Column(name = "annual_noi_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal netOperatingIncomeUsd;

    @Column(name = "annual_gross_income_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossScheduledIncomePen;

    @Column(name = "annual_noi_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal netOperatingIncomePen;

    protected FinancialCascade() {}

    public FinancialCascade(BigDecimal grossScheduledIncomeUsd,
                            BigDecimal operatingExpensesUsd,
                            BigDecimal netOperatingIncomeUsd,
                            BigDecimal grossScheduledIncomePen,
                            BigDecimal netOperatingIncomePen) {
        this.grossScheduledIncomeUsd = grossScheduledIncomeUsd;
        this.operatingExpensesUsd = operatingExpensesUsd;
        this.netOperatingIncomeUsd = netOperatingIncomeUsd;
        this.grossScheduledIncomePen = grossScheduledIncomePen;
        this.netOperatingIncomePen = netOperatingIncomePen;
    }

    /**
     * Calcula la cascada financiera a partir del canon de alquiler mensual, el OER y el tipo de cambio.
     *
     * @param monthlyRentalUsd Canon mensual predicho en USD
     * @param oerRatio         Ratio de gastos operativos parametrizado
     * @param exchangeRate     Tipo de cambio USD/PEN
     * @return Cascada financiera calculada
     */
    public static FinancialCascade calculate(BigDecimal monthlyRentalUsd, OerRatio oerRatio, BigDecimal exchangeRate) {
        if (monthlyRentalUsd == null || monthlyRentalUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFinancialInputException("El canon de arrendamiento mensual debe ser mayor a 0");
        }
        if (oerRatio == null) {
            throw new InvalidFinancialInputException("El OER es requerido para calcular la cascada financiera");
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFinancialInputException("El tipo de cambio referencial debe ser mayor a 0");
        }

        // GSI = Alquiler mensual * 12
        BigDecimal gsiUsd = monthlyRentalUsd.multiply(MONTHS_PER_YEAR).setScale(2, RoundingMode.HALF_UP);

        // OpEx = GSI * OER
        BigDecimal opexUsd = gsiUsd.multiply(oerRatio.getValue()).setScale(2, RoundingMode.HALF_UP);

        // NOI = GSI - OpEx
        BigDecimal noiUsd = gsiUsd.subtract(opexUsd).setScale(2, RoundingMode.HALF_UP);

        // Bimoneda (PEN)
        BigDecimal gsiPen = gsiUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal noiPen = noiUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        return new FinancialCascade(gsiUsd, opexUsd, noiUsd, gsiPen, noiPen);
    }
}
