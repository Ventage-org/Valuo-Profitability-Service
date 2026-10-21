package pe.ventage.valuo.profitability.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;
import pe.ventage.valuo.profitability.domain.model.valueobjects.FinancialCascade;
import pe.ventage.valuo.profitability.domain.model.valueobjects.OerRatio;
import pe.ventage.valuo.profitability.domain.model.valueobjects.ReturnMetrics;
import pe.ventage.valuo.profitability.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado Raíz del Bounded Context de Rentabilidad Financiera.
 * Representa la valoración financiera integral combinando los modelos de Venta y Alquiler
 * bajo supuestos de adquisición al contado (unlevered) con ratio OER parametrizado.
 */
@Getter
@Entity
@Table(name = "profitability_calculations", schema = "profitability")
public class ProfitabilityCalculation extends AuditableAbstractAggregateRoot<ProfitabilityCalculation> {

    public static final String AVISO_NORMATIVO_LEGAL =
            "Este indicador refleja el rendimiento para una compra al contado sin financiamiento; " +
            "si el comprador utiliza crédito hipotecario, el retorno real de su capital variará de acuerdo a las cuotas de su préstamo.";

    public static final BigDecimal DEFAULT_EXCHANGE_RATE = new BigDecimal("3.7500");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agenteId;

    @Column(name = "property_id")
    private UUID inmuebleId;

    @Column(name = "sale_prediction_id", nullable = false)
    private UUID prediccionVentaId;

    @Column(name = "rental_prediction_id", nullable = false)
    private UUID prediccionAlquilerId;

    @Column(name = "sale_value_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorVentaUsd;

    @Column(name = "monthly_rent_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal alquilerMensualUsd;

    @Embedded
    private OerRatio oerRatio;

    @Embedded
    private FinancialCascade financialCascade;

    @Embedded
    private ReturnMetrics returnMetrics;

    @Column(name = "exchange_rate_used", nullable = false, precision = 6, scale = 4)
    private BigDecimal tipoCambioUsado;

    @Column(name = "sale_value_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorVentaPen;

    @Column(name = "monthly_rent_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal alquilerMensualPen;

    @Column(name = "district_name", length = 100)
    private String distritoNombre;

    @Column(name = "legal_disclaimer", nullable = false, columnDefinition = "TEXT")
    private String avisoNormativo;

    @Column(name = "assumption_notes", columnDefinition = "TEXT")
    private String observacionesSupuestos;

    /**
     * Constructor protegido requerido exclusivamente por JPA.
     */
    protected ProfitabilityCalculation() {
        super();
    }

    /**
     * Constructor rico desde el Command.
     * Valida todos los invariantes de negocio, resuelve la bimoneda PEN/USD con el TC único
     * y computa la cascada financiera y métricas.
     *
     * @param command Intención de cálculo con las variables validadas
     */
    public ProfitabilityCalculation(CalculateProfitabilityCommand command) {
        super();
        validateInvariants(command);

        this.agenteId = command.agenteId();
        this.inmuebleId = command.inmuebleId();
        this.prediccionVentaId = command.prediccionVentaId();
        this.prediccionAlquilerId = command.prediccionAlquilerId();
        this.distritoNombre = command.distritoNombre();
        this.observacionesSupuestos = command.observaciones();
        this.avisoNormativo = AVISO_NORMATIVO_LEGAL;

        // Tipo de cambio unificado (si no se especifica, usa el estándar 3.7500)
        BigDecimal tc = (command.tipoCambioUsado() != null && command.tipoCambioUsado().compareTo(BigDecimal.ZERO) > 0)
                ? command.tipoCambioUsado()
                : DEFAULT_EXCHANGE_RATE;
        this.tipoCambioUsado = tc.setScale(4, RoundingMode.HALF_UP);
        this.oerRatio = OerRatio.of(command.oerRatio());

        // Resolución Bimoneda (PEN <-> USD):
        // 1. Si viene en PEN (modelo predictivo mlService en Soles Nominales), se calcula el valor USD = PEN / TC
        if (command.valorVentaPen() != null && command.valorVentaPen().compareTo(BigDecimal.ZERO) > 0) {
            this.valorVentaPen = command.valorVentaPen().setScale(2, RoundingMode.HALF_UP);
            this.valorVentaUsd = this.valorVentaPen.divide(this.tipoCambioUsado, 2, RoundingMode.HALF_UP);
        } else {
            this.valorVentaUsd = command.valorVentaUsd().setScale(2, RoundingMode.HALF_UP);
            this.valorVentaPen = this.valorVentaUsd.multiply(this.tipoCambioUsado).setScale(2, RoundingMode.HALF_UP);
        }

        if (command.alquilerMensualPen() != null && command.alquilerMensualPen().compareTo(BigDecimal.ZERO) > 0) {
            this.alquilerMensualPen = command.alquilerMensualPen().setScale(2, RoundingMode.HALF_UP);
            this.alquilerMensualUsd = this.alquilerMensualPen.divide(this.tipoCambioUsado, 2, RoundingMode.HALF_UP);
        } else {
            this.alquilerMensualUsd = command.alquilerMensualUsd().setScale(2, RoundingMode.HALF_UP);
            this.alquilerMensualPen = this.alquilerMensualUsd.multiply(this.tipoCambioUsado).setScale(2, RoundingMode.HALF_UP);
        }

        // 1. Cascada Operativa: GSI, OpEx, NOI (en USD y PEN)
        this.financialCascade = FinancialCascade.calculate(this.alquilerMensualUsd, this.oerRatio, this.tipoCambioUsado);

        // 2. Indicadores de Retorno: Cap Rate, Gross Yield, GRM, Benchmark
        this.returnMetrics = ReturnMetrics.calculate(
                this.valorVentaUsd,
                this.financialCascade.getGrossScheduledIncomeUsd(),
                this.financialCascade.getNetOperatingIncomeUsd(),
                this.distritoNombre
        );
    }

    /**
     * Permite reajustar la sensibilidad del análisis modificando el ratio OER y/o el Tipo de Cambio.
     * Mantiene los precios base de mercado en Soles (PEN nominales) y recalibra los flujos en USD.
     *
     * @param nuevoOer        Nuevo ratio de gastos operativos entre 0 y 0.35 (opcional si es nulo)
     * @param nuevoTipoCambio Nuevo tipo de cambio USD/PEN referencial (opcional si es nulo)
     */
    public void recalcularSensibilidad(BigDecimal nuevoOer, BigDecimal nuevoTipoCambio) {
        if (nuevoOer != null) {
            this.oerRatio = OerRatio.of(nuevoOer);
        }
        if (nuevoTipoCambio != null && nuevoTipoCambio.compareTo(BigDecimal.ZERO) > 0) {
            this.tipoCambioUsado = nuevoTipoCambio.setScale(4, RoundingMode.HALF_UP);
            // Al cambiar el TC, re-escalamos los valores en USD a partir de los precios de mercado en Soles
            this.valorVentaUsd = this.valorVentaPen.divide(this.tipoCambioUsado, 2, RoundingMode.HALF_UP);
            this.alquilerMensualUsd = this.alquilerMensualPen.divide(this.tipoCambioUsado, 2, RoundingMode.HALF_UP);
        }

        this.financialCascade = FinancialCascade.calculate(this.alquilerMensualUsd, this.oerRatio, this.tipoCambioUsado);
        this.returnMetrics = ReturnMetrics.calculate(
                this.valorVentaUsd,
                this.financialCascade.getGrossScheduledIncomeUsd(),
                this.financialCascade.getNetOperatingIncomeUsd(),
                this.distritoNombre
        );
    }

    /**
     * Permite reajustar la sensibilidad del análisis modificando el ratio OER conservando el tipo de cambio.
     *
     * @param nuevoOer Nuevo ratio de gastos operativos entre 0 y 0.35
     */
    public void recalcularSensibilidadOer(BigDecimal nuevoOer) {
        recalcularSensibilidad(nuevoOer, this.tipoCambioUsado);
    }

    private void validateInvariants(CalculateProfitabilityCommand c) {
        if (c == null) {
            throw new InvalidFinancialInputException("El comando de cálculo de rentabilidad no puede ser nulo");
        }
        if (c.agenteId() == null) {
            throw new InvalidFinancialInputException("El identificador del agente inmobiliario es obligatorio");
        }
        if (c.prediccionVentaId() == null) {
            throw new InvalidFinancialInputException("El ID de la predicción de venta es obligatorio");
        }
        if (c.prediccionAlquilerId() == null) {
            throw new InvalidFinancialInputException("El ID de la predicción de alquiler es obligatorio");
        }
        boolean hasSalePrice = (c.valorVentaUsd() != null && c.valorVentaUsd().compareTo(BigDecimal.ZERO) > 0)
                || (c.valorVentaPen() != null && c.valorVentaPen().compareTo(BigDecimal.ZERO) > 0);
        if (!hasSalePrice) {
            throw new InvalidFinancialInputException("El valor estimado de venta debe ser mayor a 0 (en USD o PEN)");
        }
        boolean hasRentalPrice = (c.alquilerMensualUsd() != null && c.alquilerMensualUsd().compareTo(BigDecimal.ZERO) > 0)
                || (c.alquilerMensualPen() != null && c.alquilerMensualPen().compareTo(BigDecimal.ZERO) > 0);
        if (!hasRentalPrice) {
            throw new InvalidFinancialInputException("El canon de alquiler mensual debe ser mayor a 0 (en USD o PEN)");
        }
        if (c.oerRatio() == null) {
            throw new InvalidFinancialInputException("El ratio OER debe ser especificado");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfitabilityCalculation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
