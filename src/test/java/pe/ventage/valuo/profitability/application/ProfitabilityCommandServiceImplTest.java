package pe.ventage.valuo.profitability.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.ventage.valuo.profitability.application.internal.commandservices.ProfitabilityCommandServiceImpl;
import pe.ventage.valuo.profitability.application.internal.outboundservices.ExternalPredictionsService;
import pe.ventage.valuo.profitability.application.internal.outboundservices.PredictionSnapshot;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;
import pe.ventage.valuo.profitability.infrastructure.persistence.jpa.repositories.ProfitabilityCalculationRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de Aplicación: ProfitabilityCommandServiceImpl")
class ProfitabilityCommandServiceImplTest {

    @Mock
    private ProfitabilityCalculationRepository repository;

    @Mock
    private ExternalPredictionsService externalPredictionsService;

    private ProfitabilityCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new ProfitabilityCommandServiceImpl(repository, externalPredictionsService);
    }

    @Test
    @DisplayName("Debe persistir el cálculo directamente cuando los valores ya vienen provistos en el comando")
    void shouldPersistCalculationDirectlyWhenValuesAreProvided() {
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
                "Notas"
        );

        when(repository.save(any(ProfitabilityCalculation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = commandService.handle(command);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("5.1895"), result.get().getReturnMetrics().getCapRatePercentage());
        verify(repository, times(1)).save(any(ProfitabilityCalculation.class));
        verifyNoInteractions(externalPredictionsService);
    }

    @Test
    @DisplayName("Debe enriquecer los datos desde msPrediccion cuando los valores vienen nulos")
    void shouldEnrichFromExternalServiceWhenValuesAreNull() {
        UUID ventaId = UUID.randomUUID();
        UUID alquilerId = UUID.randomUUID();

        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ventaId,
                alquilerId,
                null, // valorVentaUsd nulo
                null, // alquilerMensualUsd nulo
                new BigDecimal("0.1500"),
                null, // tipoCambio nulo
                null, // distrito nulo
                "Enriquecido"
        );

        when(externalPredictionsService.findPredictionById(ventaId)).thenReturn(Optional.of(
                new PredictionSnapshot(ventaId, "VENTA", new BigDecimal("285000.00"), new BigDecimal("3.7500"), "Miraflores")
        ));
        when(externalPredictionsService.findPredictionById(alquilerId)).thenReturn(Optional.of(
                new PredictionSnapshot(alquilerId, "ALQUILER", new BigDecimal("1450.00"), new BigDecimal("3.7500"), "Miraflores")
        ));
        when(repository.save(any(ProfitabilityCalculation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = commandService.handle(command);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("285000.00"), result.get().getValorVentaUsd());
        assertEquals(new BigDecimal("1450.00"), result.get().getAlquilerMensualUsd());
        assertEquals(new BigDecimal("5.1895"), result.get().getReturnMetrics().getCapRatePercentage());
        verify(externalPredictionsService, times(1)).findPredictionById(ventaId);
        verify(externalPredictionsService, times(1)).findPredictionById(alquilerId);
        verify(repository, times(1)).save(any(ProfitabilityCalculation.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la predicción de venta remota no existe")
    void shouldThrowExceptionWhenRemotePredictionNotFound() {
        UUID ventaId = UUID.randomUUID();
        var command = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ventaId,
                UUID.randomUUID(),
                null,
                new BigDecimal("1450.00"),
                new BigDecimal("0.1500"),
                new BigDecimal("3.7500"),
                "Miraflores",
                "Falla"
        );

        when(externalPredictionsService.findPredictionById(ventaId)).thenReturn(Optional.empty());

        assertThrows(InvalidFinancialInputException.class, () -> commandService.handle(command));
        verify(repository, never()).save(any());
    }
}
