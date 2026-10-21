package pe.ventage.valuo.profitability.infrastructure.clients.predictions;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para la conexión REST hacia msPrediccion.
 */
@Configuration
@ConfigurationProperties(prefix = "services.predictions")
public class PredictionsClientProperties {

    private String baseUrl = "http://localhost:8083";
    private int connectTimeoutSeconds = 3;
    private int readTimeoutSeconds = 5;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
