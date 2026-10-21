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
 * Value Object para el Operating Expense Ratio (OER).
 * Invariante: valor decimal entre 0.0000 (0% sin gastos) y 0.3500 (35% conservador).
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class OerRatio {

    public static final BigDecimal MIN_RATIO = BigDecimal.ZERO;
    public static final BigDecimal MAX_RATIO = new BigDecimal("0.3500");
    public static final BigDecimal DEFAULT_LIMA_RATIO = new BigDecimal("0.1500");

    @Column(name = "oer_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal value;

    protected OerRatio() {
        this.value = DEFAULT_LIMA_RATIO;
    }

    public OerRatio(BigDecimal value) {
        if (value == null) {
            throw new InvalidFinancialInputException("El ratio de gastos operativos (OER) no puede ser nulo");
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(MIN_RATIO) < 0 || normalized.compareTo(MAX_RATIO) > 0) {
            throw new InvalidFinancialInputException(
                    String.format("El ratio OER debe estar entre 0.0000 (0%%) y 0.3500 (35%%). Valor recibido: %s", normalized));
        }
        this.value = normalized;
    }

    public static OerRatio of(BigDecimal value) {
        return new OerRatio(value);
    }

    public static OerRatio defaultRatio() {
        return new OerRatio(DEFAULT_LIMA_RATIO);
    }

    public BigDecimal toPercentage() {
        return this.value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
}
