package pe.ventage.valuo.profitability.interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.ventage.valuo.profitability.domain.model.aggregates.ProfitabilityCalculation;
import pe.ventage.valuo.profitability.domain.model.commands.CalculateProfitabilityCommand;
import pe.ventage.valuo.profitability.domain.model.queries.GetProfitabilityByIdQuery;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityCommandService;
import pe.ventage.valuo.profitability.domain.services.ProfitabilityQueryService;
import pe.ventage.valuo.profitability.interfaces.rest.ProfitabilityController;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfitabilityController.class)
@DisplayName("Pruebas de Capa Web: ProfitabilityController")
class ProfitabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfitabilityCommandService commandService;

    @MockitoBean
    private ProfitabilityQueryService queryService;

    @Test
    @DisplayName("POST /api/v1/rentabilidad debe responder 201 Created cuando el request es válido")
    void shouldReturn201CreatedWhenRequestIsValid() throws Exception {
        UUID inmuebleId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID alquilerId = UUID.randomUUID();


        var dummyCommand = new CalculateProfitabilityCommand(
                UUID.randomUUID(),
                inmuebleId,
                ventaId,
                alquilerId,
                new BigDecimal("285000.00"),
                new BigDecimal("1450.00"),
                new BigDecimal("0.1500"),
                new BigDecimal("3.7500"),
                "Miraflores",
                "Notas"
        );
        var dummyEntity = new ProfitabilityCalculation(dummyCommand);

        when(commandService.handle(any())).thenReturn(Optional.of(dummyEntity));

        String jsonPayload = """
                {
                    "inmuebleId": "%s",
                    "prediccionVentaId": "%s",
                    "prediccionAlquilerId": "%s",
                    "valorVentaUsd": 285000.00,
                    "alquilerMensualPen": 5437.50,
                    "oerRatio": 0.1500,
                    "tipoCambioUsado": 3.7500,
                    "distritoNombre": "Miraflores",
                    "observaciones": "Notas"
                }
                """.formatted(inmuebleId, ventaId, alquilerId);

        mockMvc.perform(post("/api/v1/rentabilidad")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cascadaFinanciera.noiAnualUsd").value(14790.00))
                .andExpect(jsonPath("$.metricasRetorno.capRatePorcentaje").value(5.1895))
                .andExpect(jsonPath("$.metricasRetorno.aniosRetornoGrm").value(16.38))
                .andExpect(jsonPath("$.metricasRetorno.aniosPaybackNeto").value(19.27))
                .andExpect(jsonPath("$.metricasRetorno.clasificacionBenchmark").value("PROMEDIO"));
    }

    @Test
    @DisplayName("POST /api/v1/rentabilidad/simular debe responder 200 OK con métricas calculadas en memoria")
    void shouldReturn200OkForSimulation() throws Exception {
        String simularPayload = """
                {
                    "valorVentaUsd": 285000.00,
                    "alquilerMensualPen": 5437.50,
                    "oerRatio": 0.2000,
                    "tipoCambioUsado": 3.7500,
                    "distritoNombre": "Miraflores"
                }
                """;

        mockMvc.perform(post("/api/v1/rentabilidad/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simularPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cascadaFinanciera.oerPorcentaje").value(20.00))
                .andExpect(jsonPath("$.cascadaFinanciera.noiAnualUsd").value(13920.00))
                .andExpect(jsonPath("$.metricasRetorno.capRatePorcentaje").value(4.8842))
                .andExpect(jsonPath("$.metricasRetorno.aniosRetornoGrm").value(16.38))
                .andExpect(jsonPath("$.metricasRetorno.aniosPaybackNeto").value(20.47))
                .andExpect(jsonPath("$.metricasRetorno.clasificacionBenchmark").value("PROMEDIO"));
    }

    @Test
    @DisplayName("POST /api/v1/rentabilidad debe responder 400 Bad Request si el OER supera el 35%")
    void shouldReturn400BadRequestWhenOerExceedsLimit() throws Exception {
        String invalidPayload = """
                {
                    "inmuebleId": "%s",
                    "prediccionVentaId": "%s",
                    "prediccionAlquilerId": "%s",
                    "valorVentaUsd": 285000.00,
                    "alquilerMensualPen": 5437.50,
                    "oerRatio": 0.5000,
                    "tipoCambioUsado": 3.7500,
                    "distritoNombre": "Miraflores",
                    "observaciones": "Invalido"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/rentabilidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.oerRatio").exists());
    }

    @Test
    @DisplayName("GET /api/v1/rentabilidad/{id} debe responder 404 Not Found si no existe")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.handle(new GetProfitabilityByIdQuery(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/rentabilidad/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Cálculo de rentabilidad no encontrado"));
    }
}
