package pe.ventage.valuo.profitability.shared.interfaces.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;
import pe.ventage.valuo.profitability.domain.model.exceptions.ProfitabilityCalculationNotFoundException;
import pe.ventage.valuo.profitability.infrastructure.clients.predictions.PredictionsServiceUnavailableException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones REST utilizando el estándar RFC 7807 (ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProfitabilityCalculationNotFoundException.class)
    public ProblemDetail handleNotFound(ProfitabilityCalculationNotFoundException e) {
        log.warn("Recurso no encontrado: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Cálculo de rentabilidad no encontrado");
        problem.setType(URI.create("https://api.valuo.pe/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidFinancialInputException.class)
    public ProblemDetail handleInvalidInput(InvalidFinancialInputException e) {
        log.warn("Violación de invariante financiero: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Parámetros financieros inválidos");
        problem.setType(URI.create("https://api.valuo.pe/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Error de validación en request: {}", errors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Los datos enviados contienen errores de validación");
        problem.setTitle("Error de validación de campos");
        problem.setType(URI.create("https://api.valuo.pe/errors/validation"));
        problem.setProperty("errores", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PredictionsServiceUnavailableException.class)
    public ProblemDetail handleServiceUnavailable(PredictionsServiceUnavailableException e) {
        log.error("Fallo de comunicación con microservicio remoto: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setTitle("Servicio de predicciones no disponible");
        problem.setType(URI.create("https://api.valuo.pe/errors/service-unavailable"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException e) {
        log.debug("Recurso estático o ruta no encontrada: {}", e.getResourcePath());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "El recurso solicitado no fue encontrado: " + e.getResourcePath());
        problem.setTitle("Recurso no encontrado");
        problem.setType(URI.create("https://api.valuo.pe/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception e) {
        log.error("Error no controlado en el servidor: ", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado. Contacte a soporte.");
        problem.setTitle("Error interno del servidor");
        problem.setType(URI.create("https://api.valuo.pe/errors/internal"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
