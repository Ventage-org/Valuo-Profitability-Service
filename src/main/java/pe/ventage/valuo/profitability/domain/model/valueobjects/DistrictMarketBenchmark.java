package pe.ventage.valuo.profitability.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Referencia de mercado por distrito basada en el indicador PER (Precio de Venta / Alquiler Anual)
 * publicado por el BCRP (BCRPData, serie trimestral "Indicador Precio de Venta/Alquiler Anual - PER").
 *
 * Tabla estática: el dato es trimestral, se actualiza manualmente (ver {@link #PERIODO}).
 * El BCRP solo publica 12 distritos de Lima; el resto no tiene referencia (Optional vacío).
 * Series: PD41940PQ (Barranco) ... PD41951PQ (Surquillo), PD41952PQ (Promedio).
 */
public final class DistrictMarketBenchmark {

    public static final String FUENTE = "BCRP - Indicador Precio de Venta/Alquiler Anual (PER)";
    public static final String PERIODO = "T4-2025";

    private static final Map<String, BigDecimal> PER_BY_DISTRICT = Map.ofEntries(
            Map.entry("barranco", new BigDecimal("15.3048")),
            Map.entry("jesus maria", new BigDecimal("16.6538")),
            Map.entry("la molina", new BigDecimal("15.4008")),
            Map.entry("lince", new BigDecimal("14.9658")),
            Map.entry("magdalena", new BigDecimal("16.2307")),
            Map.entry("miraflores", new BigDecimal("17.5000")),
            Map.entry("pueblo libre", new BigDecimal("14.8656")),
            Map.entry("san borja", new BigDecimal("18.1336")),
            Map.entry("san isidro", new BigDecimal("15.9490")),
            Map.entry("san miguel", new BigDecimal("16.8863")),
            Map.entry("surco", new BigDecimal("19.8189")),
            Map.entry("surquillo", new BigDecimal("14.4120"))
    );

    private static final Map<String, String> ALIASES = Map.of(
            "magdalena del mar", "magdalena",
            "santiago de surco", "surco"
    );

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Referencia de mercado de un distrito.
     *
     * @param perYears            PER en años (precio / alquiler anual)
     * @param grossYieldPercentage rendimiento bruto de mercado en % (100 / PER)
     */
    public record MarketReference(BigDecimal perYears, BigDecimal grossYieldPercentage) {}

    private DistrictMarketBenchmark() {}

    public static Optional<MarketReference> findByDistrict(String districtName) {
        if (districtName == null || districtName.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(districtName);
        key = ALIASES.getOrDefault(key, key);
        BigDecimal per = PER_BY_DISTRICT.get(key);
        if (per == null) {
            return Optional.empty();
        }
        BigDecimal grossYield = HUNDRED.divide(per, 4, RoundingMode.HALF_UP);
        return Optional.of(new MarketReference(per, grossYield));
    }

    private static String normalize(String value) {
        String noAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
