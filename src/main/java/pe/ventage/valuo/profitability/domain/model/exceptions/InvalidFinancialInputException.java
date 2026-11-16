package pe.ventage.valuo.profitability.domain.model.exceptions;

/**
 * Excepción de dominio lanzada cuando una entrada financiera viola un invariante
 * del modelo matemático de rentabilidad.
 */
public class InvalidFinancialInputException extends RuntimeException {

    public InvalidFinancialInputException(String message) {
        super(message);
    }
}
