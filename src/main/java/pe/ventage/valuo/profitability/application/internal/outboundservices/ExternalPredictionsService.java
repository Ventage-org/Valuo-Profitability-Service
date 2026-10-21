package pe.ventage.valuo.profitability.application.internal.outboundservices;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (Anti-Corruption Layer) hacia el microservicio remoto de predicciones (msPrediccion).
 * La implementación vive en infrastructure/clients/predictions.
 */
public interface ExternalPredictionsService {

    /**
     * Consulta el snapshot de una predicción por su ID único.
     *
     * @param predictionId Identificador UUID de la predicción en msPrediccion
     * @return Snapshot en modelo propio de rentabilidad
     */
    Optional<PredictionSnapshot> findPredictionById(UUID predictionId);
}
