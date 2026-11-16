package pe.ventage.valuo.profitability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;
import pe.ventage.valuo.profitability.domain.model.valueobjects.BenchmarkCategory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de Dominio: Cálculo de Rentabilidad (Cap Rate y NOI)")
class ProfitabilityCalculationTest {

    @Test
    @DisplayName("Debe calcular exactamente la cascada financiera y métricas según el caso de la maqueta")
    void shouldCalculateFinancialCascadeAndMetricsAccurately() {
        // Arrange: Datos del mockup de la maqueta (Dúplex en Miraflores)
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("285000.00"), // V_venta
                new BigDecimal("1450.00"),   // P_alquiler mensual
                new BigDecimal("0.1500"),    // OER 15%
                new BigDecimal("3.7500"),    // TC USD/PEN
                "Miraflores",
                "Prueba unitaria según maqueta"
        );

        // Act
        var calculation = new ProfitabilityCalculation(command);

        // Assert: Cascada Operativa
        var cascade = calculation.getFinancialCascade();
        assertEquals(new BigDecimal("17400.00"), cascade.getGrossScheduledIncomeUsd(), "GSI anual USD = 1450 * 12 = 17,400");
        assertEquals(new BigDecimal("2610.00"), cascade.getOperatingExpensesUsd(), "OpEx anual USD = 17,400 * 0.15 = 2,610");
        assertEquals(new BigDecimal("14790.00"), cascade.getNetOperatingIncomeUsd(), "NOI anual USD = 17,400 - 2,610 = 14,790");

        // Assert: Bimoneda
        assertEquals(new BigDecimal("65250.00"), cascade.getGrossScheduledIncomePen(), "GSI PEN = 17,400 * 3.75");
        assertEquals(new BigDecimal("55462.50"), cascade.getNetOperatingIncomePen(), "NOI PEN = 14,790 * 3.75");

        // Assert: Indicadores de Retorno
        var metrics = calculation.getReturnMetrics();
        assertEquals(new BigDecimal("5.1895"), metrics.getCapRatePercentage(), "Cap Rate = (14790 / 285000) * 100 = 5.1895%");
        assertEquals(new BigDecimal("6.1053"), metrics.getGrossYieldPercentage(), "Gross Yield = (17400 / 285000) * 100 = 6.1053%");
        assertEquals(new BigDecimal("16.38"), metrics.getGrossRentMultiplierYears(), "GRM = 285000 / 17400 = 16.38 años (~16.4)");
        assertEquals(new BigDecimal("19.27"), metrics.getNetPaybackYears(), "Payback Neto = 285000 / 14790 = 19.27 años");

        // Assert: Semáforo Benchmark BCRP
        assertEquals(BenchmarkCategory.PROMEDIO, metrics.getBenchmarkCategory(), "5.19% pertenece al rango PROMEDIO (4.5% - 6.5%)");

        // Assert: Aviso Legal Normativo
        assertTrue(calculation.getAvisoNormativo().contains("Este indicador refleja el rendimiento para una compra al contado"));
    }

    @Test
    @DisplayName("Debe rechazar OER superior al límite máximo permitido de 35%")
    void shouldRejectOerExceedingMaxLimit() {
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("285000.00"),
                new BigDecimal("1450.00"),
                new BigDecimal("0.4000"), // 40% > 35%
                new BigDecimal("3.7500"),
                "Miraflores",
                "OER excesivo"
        );

        assertThrows(InvalidFinancialInputException.class, () -> new ProfitabilityCalculation(command));
    }

    @Test
    @DisplayName("Debe recalcular la sensibilidad al variar el OER con el método de intención del agregado")
    void shouldRecalculateSensitivityWhenOerChanges() {
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("285000.00"),
                new BigDecimal("1450.00"),
                new BigDecimal("0.1500"),
                new BigDecimal("3.7500"),
                "Miraflores",
                "Ajuste de slider"
        );

        var calculation = new ProfitabilityCalculation(command);
        assertEquals(new BigDecimal("14790.00"), calculation.getFinancialCascade().getNetOperatingIncomeUsd());

        // Modificar OER a 20%
        calculation.recalcularSensibilidadOer(new BigDecimal("0.2000"));

        // OpEx = 17400 * 0.20 = 3480; NOI = 17400 - 3480 = 13920
        assertEquals(new BigDecimal("13920.00"), calculation.getFinancialCascade().getNetOperatingIncomeUsd());
        // Cap Rate = (13920 / 285000) * 100 = 4.8842%
        assertEquals(new BigDecimal("4.8842"), calculation.getReturnMetrics().getCapRatePercentage());
    }

    @Test
    @DisplayName("Debe clasificar como BAJO si el Cap Rate es menor a 4.5% (San Isidro Golf)")
    void shouldClassifyAsBajoWhenCapRateIsLessThanFourPointFive() {
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500000.00"), // Precio alto
                new BigDecimal("1500.00"),   // Alquiler moderado -> Cap Rate bajo
                new BigDecimal("0.1500"),
                new BigDecimal("3.7500"),
                "San Isidro",
                "Golf"
        );

        var calculation = new ProfitabilityCalculation(command);
        assertEquals(BenchmarkCategory.BAJO, calculation.getReturnMetrics().getBenchmarkCategory());
    }

    @Test
    @DisplayName("Debe calcular rentabilidad recibiendo valores predichos en Soles Nominales (PEN) y convertir a USD con TC 3.75")
    void shouldCalculateProfitabilityWhenBaseInputsAreInSolesNominales() {
        // Arrange: 1,068,750 PEN venta (~285,000 USD a TC 3.75), 5,437.50 PEN alquiler (~1,450 USD a TC 3.75)
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,                           // Venta USD no viene
                null,                           // Alquiler USD no viene
                new BigDecimal("1068750.00"),   // Venta PEN nominales desde mlService
                new BigDecimal("5437.50"),      // Alquiler PEN nominales desde mlService
                new BigDecimal("0.1500"),       // OER 15%
                new BigDecimal("3.7500"),       // TC
                "Miraflores",
                "Predicción nativa en Soles"
        );

        // Act
        var calculation = new ProfitabilityCalculation(command);

        // Assert: Conversión correcta a USD
        assertEquals(new BigDecimal("285000.00"), calculation.getValorVentaUsd());
        assertEquals(new BigDecimal("1450.00"), calculation.getAlquilerMensualUsd());
        assertEquals(new BigDecimal("1068750.00"), calculation.getValorVentaPen());
        assertEquals(new BigDecimal("5437.50"), calculation.getAlquilerMensualPen());

        // Cap Rate debe ser idéntico al caso USD (5.1895%)
        assertEquals(new BigDecimal("5.1895"), calculation.getReturnMetrics().getCapRatePercentage());
        assertEquals(new BigDecimal("6.1053"), calculation.getReturnMetrics().getGrossYieldPercentage());
    }

    @Test
    @DisplayName("Debe recalcular la sensibilidad cuando cambia el Tipo de Cambio re-escalando los flujos en USD")
    void shouldRecalculateSensitivityWhenExchangeRateChanges() {
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                new BigDecimal("1140000.00"), // 1,140,000 PEN
                new BigDecimal("5700.00"),    // 5,700 PEN
                new BigDecimal("0.1500"),
                new BigDecimal("3.8000"),     // TC inicial 3.80 -> Venta USD = 300,000, Alquiler USD = 1,500
                "San Isidro",
                "Sensibilidad TC"
        );

        var calculation = new ProfitabilityCalculation(command);
        assertEquals(new BigDecimal("300000.00"), calculation.getValorVentaUsd());
        assertEquals(new BigDecimal("1500.00"), calculation.getAlquilerMensualUsd());

        // Simulamos variación del Tipo de Cambio a 3.70 (Soles se aprecian frente al dólar)
        calculation.recalcularSensibilidad(null, new BigDecimal("3.7000"));

        // Venta USD re-escalada = 1,140,000 / 3.70 = 308,108.11 USD
        assertEquals(new BigDecimal("308108.11"), calculation.getValorVentaUsd());
        // Alquiler USD re-escalado = 5,700 / 3.70 = 1,540.54 USD
        assertEquals(new BigDecimal("1540.54"), calculation.getAlquilerMensualUsd());
        // El TC almacenado se actualiza a 3.7000
        assertEquals(new BigDecimal("3.7000"), calculation.getTipoCambioUsado());
    }
}
