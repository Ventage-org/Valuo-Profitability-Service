package pe.ventage.valuo.profitability.infrastructure.clients.predictions;

/**
 * Excepción lanzada cuando el microservicio msPrediccion se encuentra inaccesible,
 * caído o genera un timeout de respuesta (mapea a HTTP 503 en el advice global).
 */
public class PredictionsServiceUnavailableException extends RuntimeException {

    public PredictionsServiceUnavailableException(Throwable cause) {
        super("El microservicio de predicciones (msPrediccion) no está disponible temporalmente. Intente nuevamente.", cause);
    }

    public PredictionsServiceUnavailableException(String message) {
        super(message);
    }
}
