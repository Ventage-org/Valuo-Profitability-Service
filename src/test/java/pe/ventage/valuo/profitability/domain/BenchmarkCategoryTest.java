package pe.ventage.valuo.profitability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.ventage.valuo.profitability.domain.model.valueobjects.BenchmarkCategory;
import pe.ventage.valuo.profitability.domain.model.valueobjects.DistrictMarketBenchmark;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de Dominio: Benchmark BCRP por distrito (PER)")
class BenchmarkCategoryTest {

    @Test
    @DisplayName("Miraflores PER 17.5 -> rendimiento bruto de mercado 5.7143%")
    void shouldDeriveMarketYieldFromPer() {
        var ref = DistrictMarketBenchmark.findByDistrict("Miraflores").orElseThrow();
        assertEquals(new BigDecimal("5.7143"), ref.grossYieldPercentage());
    }

    @Test
    @DisplayName("Debe resolver nombres con tilde, mayúsculas y alias de distrito")
    void shouldNormalizeDistrictNames() {
        assertTrue(DistrictMarketBenchmark.findByDistrict("JESÚS MARÍA").isPresent());
        assertTrue(DistrictMarketBenchmark.findByDistrict("Santiago de Surco").isPresent());
        assertTrue(DistrictMarketBenchmark.findByDistrict("Magdalena del Mar").isPresent());
    }

    @Test
    @DisplayName("Debe clasificar según la tolerancia de ±10% frente al mercado")
    void shouldClassifyAgainstMarketYield() {
        // Mercado Miraflores = 5.7143% -> rango PROMEDIO [5.1429, 6.2857]
        assertEquals(BenchmarkCategory.BAJO, BenchmarkCategory.classify(new BigDecimal("5.0000"), "Miraflores"));
        assertEquals(BenchmarkCategory.PROMEDIO, BenchmarkCategory.classify(new BigDecimal("6.1053"), "Miraflores"));
        assertEquals(BenchmarkCategory.ATRACTIVO, BenchmarkCategory.classify(new BigDecimal("7.0000"), "Miraflores"));
    }

    @Test
    @DisplayName("Distrito sin serie BCRP o nulo debe devolver SIN_REFERENCIA")
    void shouldReturnSinReferenciaWhenDistrictHasNoBcrpData() {
        assertEquals(BenchmarkCategory.SIN_REFERENCIA, BenchmarkCategory.classify(new BigDecimal("8.0000"), "Comas"));
        assertEquals(BenchmarkCategory.SIN_REFERENCIA, BenchmarkCategory.classify(new BigDecimal("8.0000"), null));
    }
}
