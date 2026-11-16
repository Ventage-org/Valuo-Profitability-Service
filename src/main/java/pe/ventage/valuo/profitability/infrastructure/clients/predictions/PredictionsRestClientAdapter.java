package pe.ventage.valuo.profitability.infrastructure.clients.predictions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import pe.ventage.valuo.profitability.application.internal.outboundservices.ExternalPredictionsService;
import pe.ventage.valuo.profitability.application.internal.outboundservices.PredictionSnapshot;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador REST que implementa el puerto ExternalPredictionsService.
 * Consume la API interna de msPrediccion (/internal/v1/predicciones/{id}) de forma síncrona con timeouts.
 */
@Component
public class PredictionsRestClientAdapter implements ExternalPredictionsService {

    private static final Logger log = LoggerFactory.getLogger(PredictionsRestClientAdapter.class);

    private final RestClient client;

    public PredictionsRestClientAdapter(PredictionsClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));

        this.client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<PredictionSnapshot> findPredictionById(UUID predictionId) {
        if (predictionId == null) {
            return Optional.empty();
        }

        try {
            log.debug("Llamando a msPrediccion para obtener predicción: {}", predictionId);
            PredictionApiResponse response = client.get()
                    .uri("/internal/v1/predicciones/{id}", predictionId)
                    .retrieve()
                    .body(PredictionApiResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(new PredictionSnapshot(
                    response.id(),
                    response.tipoPrediccion(),
                    response.valorEstimadoUsd(),
                    response.valorEstimadoPen(),
                    response.tipoCambioUsado(),
                    response.distritoNombre()
            ));

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Predicción remota con ID {} no encontrada (404)", predictionId);
            return Optional.empty();
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Fallo al comunicarse con msPrediccion para ID {}: {}", predictionId, e.getMessage());
            throw new PredictionsServiceUnavailableException(e);
        }
    }
}
