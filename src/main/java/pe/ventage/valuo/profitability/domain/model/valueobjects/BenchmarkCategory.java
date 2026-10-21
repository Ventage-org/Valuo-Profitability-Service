package pe.ventage.valuo.profitability.domain.model.valueobjects;

import pe.ventage.valuo.profitability.domain.model.exceptions.InvalidFinancialInputException;

import java.math.BigDecimal;

/**
 * Clasificación del Gross Yield del inmueble frente al rendimiento bruto de mercado de su distrito,
 * derivado del indicador PER del BCRP (ver {@link DistrictMarketBenchmark}).
 * Si el distrito no tiene serie publicada por el BCRP, la categoría es SIN_REFERENCIA.
 */
public enum BenchmarkCategory {
    BAJO("Bajo mercado", "Rendimiento bruto por debajo del promedio BCRP del distrito (más de 10% menor)."),
    PROMEDIO("En mercado", "Rendimiento bruto alineado con el promedio BCRP del distrito (±10%)."),
    ATRACTIVO("Sobre mercado", "Rendimiento bruto por encima del promedio BCRP del distrito (más de 10% mayor)."),
    SIN_REFERENCIA("Sin referencia BCRP", "El BCRP no publica el indicador PER para este distrito; no hay benchmark comparable.");

    /** Tolerancia respecto al rendimiento de mercado para considerarlo "en mercado". */
    private static final BigDecimal TOLERANCE_LOW = new BigDecimal("0.90");
    private static final BigDecimal TOLERANCE_HIGH = new BigDecimal("1.10");

    private final String etiqueta;
    private final String descripcion;

    BenchmarkCategory(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Clasifica el Gross Yield del inmueble contra el rendimiento bruto de mercado del distrito.
     *
     * @param grossYieldPercentage Gross Yield del inmueble en % (ej: 6.1053)
     * @param districtName         Distrito del inmueble (puede ser nulo o sin serie BCRP)
     * @return Categoría correspondiente; SIN_REFERENCIA si el distrito no tiene dato BCRP
     */
    public static BenchmarkCategory classify(BigDecimal grossYieldPercentage, String districtName) {
        if (grossYieldPercentage == null) {
            throw new InvalidFinancialInputException("El Gross Yield no puede ser nulo para clasificar el benchmark");
        }
        return DistrictMarketBenchmark.findByDistrict(districtName)
                .map(ref -> {
                    BigDecimal lower = ref.grossYieldPercentage().multiply(TOLERANCE_LOW);
                    BigDecimal upper = ref.grossYieldPercentage().multiply(TOLERANCE_HIGH);
                    if (grossYieldPercentage.compareTo(lower) < 0) {
                        return BAJO;
                    } else if (grossYieldPercentage.compareTo(upper) > 0) {
                        return ATRACTIVO;
                    }
                    return PROMEDIO;
                })
                .orElse(SIN_REFERENCIA);
    }
}
