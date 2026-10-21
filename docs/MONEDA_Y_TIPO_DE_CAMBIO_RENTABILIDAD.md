# Especificación Técnica: Manejo Bimoneda, Tipo de Cambio y Benchmark BCRP
## Microservicio: `valuo-profitability-service` (Módulo de Rentabilidad)

---

### 1. Contexto del Problema y Justificación de Negocio

El sistema **Valuo** integra dos realidades de mercado en el sector inmobiliario peruano:
1. **Motor Predictivo de Machine Learning (`mlService` / `msPrediccion`):**
   Entrenado con estadísticas macroeconómicas y series temporales oficiales del Banco Central de Reserva del Perú (BCRP). Produce las estimaciones de valor en **Soles (PEN)**, específicamente en **precios corrientes de mercado (`precio_soles_nominales`)**.
2. **Evaluación de Inversión y Rentabilidad Inmobiliaria (`valuo-profitability-service`):**
   Los inversionistas, tasadores y agentes inmobiliarios en Lima (especialmente en Lima Top y Lima Moderna) analizan y comparan el valor de adquisición de los inmuebles y sus retornos (Cap Rate, Gross Yield, GRM) referenciados en **Dólares Americanos (USD)**, a la vez que gestionan flujos de alquiler que pueden cobrarse en Soles o Dólares.

---

### 2. Las 3 Reglas de Oro del Tipo de Cambio

Para garantizar la coherencia matemática, financiera y la trazabilidad del análisis, se implementan de forma estricta las siguientes reglas:

| # | Regla | Razón Técnica / Matemática |
| :--- | :--- | :--- |
| **1** | **Mismo Tipo de Cambio para Venta y Alquiler** | El Cap Rate y el Gross Rental Yield son ratios. Si la venta y el alquiler se convirtieran con tipos de cambio distintos, se alteraría de forma artificial la rentabilidad de la propiedad por efecto de arbitraje cambiario ficticio. Al usar el mismo $TC$, el Cap Rate es **matemáticamente invariante**: $\frac{\text{NOI}_{\text{PEN}} / TC}{\text{Venta}_{\text{PEN}} / TC} = \frac{\text{NOI}_{\text{PEN}}}{\text{Venta}_{\text{PEN}}}$. |
| **2** | **Persistencia Inmutable del TC Utilizado** | Cada análisis ejecutado almacena en la tabla `profitability.profitability_calculations` la columna `exchange_rate_used DECIMAL(6,4)`. Esto garantiza auditoría histórica: cuando el agente descargue el reporte PDF semanas después, el documento reflejará con exactitud bajo qué tipo de cambio se evaluó la operación. |
| **3** | **Uso Exclusivo de Soles Nominales** | Se toma el valor `precio_soles_nominales` de la predicción y **no** el constante deflactado, ya que el flujo de caja simula transacciones a precios corrientes actuales de mercado. |

---

### 3. Modelo de Dominio y Conversión de Monedas

El agregado raíz `ProfitabilityCalculation` implementa una estrategia de resolución bimoneda automática:

```
[ Predicción Venta / Alquiler ] ──> (Soles Nominales PEN)
                                          │
                                          ▼  (TC unificado: default 3.7500 o ingresado por usuario)
                                    [ Conversión ]
                                    Venta USD    = Venta PEN / TC
                                    Alquiler USD = Alquiler PEN / TC
                                          │
                                          ▼
                             [ Cascada Operativa Bimoneda ]
                             • GSI (USD y PEN)
                             • OpEx (USD y PEN vía OER 0% - 35%)
                             • NOI (USD y PEN)
                                          │
                                          ▼
                               [ Indicadores de Retorno ]
                               • Cap Rate (%) = (NOI_USD / Venta_USD) * 100
                               • Gross Yield (%) = (GSI_USD / Venta_USD) * 100
                               • GRM (Años) = Venta_USD / GSI_USD
                               • Semáforo Benchmark BCRP (Bajo, Promedio, Atractivo)
```

#### Valor por Defecto:
* `DEFAULT_EXCHANGE_RATE = 3.7500`

---

### 4. Simulación Dinámica y Análisis de Sensibilidad

La interfaz de usuario en Angular dispone de dos controles interactivos en tiempo real:

1. **Slider de OER (Ratio de Gastos Operativos):**
   * Rango: `0.0000` (0%) a `0.3500` (35%).
   * Modifica los gastos operativos anuales (`OpEx`), alterando directamente el `NOI` y el `Cap Rate`.
2. **Slider / Input de Tipo de Cambio (TC):**
   * Rango sugerido: `3.5000` a `4.0000` (default `3.7500`).
   * Al mover el TC, el servicio re-escala instantáneamente los flujos nominales en dólares:
     $$\text{Venta USD} = \frac{\text{Venta PEN}}{TC_{\text{nuevo}}}$$
     $$\text{Alquiler USD} = \frac{\text{Alquiler PEN}}{TC_{\text{nuevo}}}$$
     $$\text{NOI USD} = \frac{\text{NOI PEN}}{TC_{\text{nuevo}}}$$
   * El porcentaje del **Cap Rate** se mantiene firme y sólido, mientras el inversionista visualiza en vivo cuánto dinero líquido neto en USD generará su propiedad ante posibles fluctuaciones del dólar.

---

### 5. Semáforo Financiero: Benchmark BCRP por Distrito

#### A. Fuente y alcance
La referencia de mercado es el indicador **PER (Precio de Venta / Alquiler Anual)** de BCRPData, serie trimestral "Indicador Precio de Venta/Alquiler Anual - PER" (códigos `PD41940PQ` a `PD41952PQ`). Período cargado: **T4-2025**.

* El BCRP publica el PER de solo **12 distritos**: Barranco, Jesús María, La Molina, Lince, Magdalena, Miraflores, Pueblo Libre, San Borja, San Isidro, San Miguel, Surco y Surquillo.
* Los otros 10 distritos del modelo (Ate Vitarte, Bellavista, Breña, Carabayllo, Cercado de Lima, Chorrillos, Comas, La Perla, La Victoria, Los Olivos) **no tienen serie** en el BCRP.

#### B. Implementación: tabla estática
Los valores se guardan en [`DistrictMarketBenchmark`](../src/main/java/pe/ventage/valuo/profitability/domain/model/valueobjects/DistrictMarketBenchmark.java) como una tabla fija. **No hay llamadas al BCRP en tiempo de ejecución.** El dato es trimestral, por lo que se actualiza a mano una vez por trimestre (constante `PERIODO`).

| Distrito | PER (años) | Rendimiento bruto de mercado (100 / PER) |
| :--- | :--- | :--- |
| Barranco | 15.30 | 6.53% |
| Jesús María | 16.65 | 6.00% |
| La Molina | 15.40 | 6.49% |
| Lince | 14.97 | 6.68% |
| Magdalena | 16.23 | 6.16% |
| Miraflores | 17.50 | 5.71% |
| Pueblo Libre | 14.87 | 6.73% |
| San Borja | 18.13 | 5.51% |
| San Isidro | 15.95 | 6.27% |
| San Miguel | 16.89 | 5.92% |
| Surco | 19.82 | 5.05% |
| Surquillo | 14.41 | 6.94% |

Los nombres se normalizan (tildes, mayúsculas) y se aceptan los alias `Santiago de Surco` y `Magdalena del Mar`.

#### C. Regla de clasificación (`BenchmarkCategory.classify`)
Se compara el **Gross Yield del inmueble** contra el rendimiento bruto de mercado de su distrito. Se compara bruto contra bruto porque el PER del BCRP es bruto (no descuenta gastos operativos).

| Categoría | Condición | Significado |
| :--- | :--- | :--- |
| `BAJO` | Gross Yield < 90% del mercado | Rinde menos que el promedio del distrito |
| `PROMEDIO` | Entre 90% y 110% del mercado | Alineado con el distrito |
| `ATRACTIVO` | Gross Yield > 110% del mercado | Rinde más que el promedio del distrito |
| `SIN_REFERENCIA` | Distrito sin serie BCRP o no informado | No hay benchmark comparable; se muestran igual Cap Rate y Gross Yield |

La tolerancia de ±10% es un criterio del proyecto, no un valor oficial del BCRP.

#### D. Comportamiento en `/simular`
El Gross Yield depende solo del precio de venta y del alquiler. **El OER y el tipo de cambio no lo modifican**, por lo que mover esos sliders cambia el Cap Rate y el NOI pero **no** el semáforo. El semáforo solo cambia si cambian el precio, el alquiler o el distrito. Para obtener benchmark, `distritoNombre` debe enviarse en el request de `/simular` y de `/rentabilidad`.

Campos de respuesta: `clasificacionBenchmark` (`BAJO`, `PROMEDIO`, `ATRACTIVO`, `SIN_REFERENCIA`), `etiquetaBenchmark` y `descripcionBenchmark`.

### 6. Flujo de Integración Inter-Servicios: Origen del Distrito y Doble Predicción Cruzada (Just-In-Time)

Para que el análisis de rentabilidad funcione sin fricción y con total integridad, el microservicio requiere 3 datos clave originados en otros servicios del ecosistema:
1. `distritoNombre`: Necesario para comparar contra el benchmark PER del BCRP de la zona.
2. `prediccionVentaId`: Precio estimado de adquisición comercial ($V_{\text{venta}}$).
3. `prediccionAlquilerId`: Canon mensual de arrendamiento proyectado ($P_{\text{alquiler}}$).

#### A. Obtención Automática del Distrito (`msInmuebles` - Puerto 8082)
* **El usuario nunca digita el distrito a mano** en el formulario de rentabilidad.
* El frontend (Angular) parte del `inmuebleId` (UUID) seleccionado por el agente inmobiliario.
* Se consulta síncronamente al microservicio de inmuebles:
  ```http
  GET http://localhost:8082/api/v1/inmuebles/{inmuebleId}
  ```
* En la respuesta (`200 OK`), se extrae el campo `distritoNombre` (ej. `"Miraflores"`), el cual viaja directamente en las peticiones hacia `valuo-profitability-service`.

#### B. Mecanismo de Predicción Cruzada en Tiempo Real (Just-In-Time Cross-Prediction)
* **El Dilema:** Al registrar un inmueble en la plataforma, el agente usualmente solo genera una predicción inicial (por ejemplo, solo predice el precio de venta si su intención primaria era vender). Sin embargo, la fórmula de rentabilidad financiera requiere obligatoriamente **ambos precios simultáneos** (Venta y Alquiler).
* **Solución Implementada (Opción A):**
  1. Antes de abrir la vista de rentabilidad, el sistema verifica en `msPrediccion` (`GET /api/v1/predicciones/inmueble/{inmuebleId}`) si ya existen ambas predicciones vigentes.
  2. Si falta alguna (por ejemplo, falta la predicción de alquiler):
     - El frontend o el flujo automático toma las características físicas del inmueble ya obtenidas de `msInmuebles` (m², habitaciones, baños, antigüedad, etc.).
     - Despacha inmediatamente la predicción faltante a `msPrediccion`:
       ```http
       POST http://localhost:8083/api/v1/predicciones/estimar
       ```
     - La inferencia estadística con Machine Learning (Python FastAPI + XGBoost en `mlService`) toma únicamente **~17 milisegundos**.
  3. Una vez que se tienen los dos IDs con sus respectivos valores en **Soles Nominales** (`precio_soles_nominales`):
     - `prediccionVentaId` $\rightarrow$ `valorVentaPen`
     - `prediccionAlquilerId` $\rightarrow$ `alquilerMensualPen`
  4. Se inicializa el cálculo en `valuo-profitability-service`:
     - Modo interactivo: `POST /api/v1/rentabilidad/simular` (en memoria con slider de OER y TC).
     - Modo definitivo: `POST /api/v1/rentabilidad` (persistencia formal en PostgreSQL).

#### C. Diagrama de Secuencia de la Integración

```
[ Angular SPA ]                [ msInmuebles:8082 ]            [ msPrediccion:8083 ]         [ msRentabilidad:8084 ]
       │                                │                                │                               │
       │ 1. GET /inmuebles/{id}         │                                │                               │
       ├───────────────────────────────>│                                │                               │
       │    Retorna distritoNombre      │                                │                               │
       │<───────────────────────────────┤                                │                               │
       │                                                                 │                               │
       │ 2. ¿Tiene predicciones de Venta y Alquiler?                    │                               │
       │    Si falta alguna (ej. Alquiler):                              │                               │
       │    POST /predicciones/estimar (reutiliza m², baños, etc.)       │                               │
       ├────────────────────────────────────────────────────────────────>│                               │
       │    Inferencia ML (~17ms) retorna prediccionAlquilerId y PEN     │                               │
       │<────────────────────────────────────────────────────────────────┤                               │
       │                                                                                                 │
       │ 3. Simulación interactiva (Sliders OER y TC)                                                   │
       │    POST /api/v1/rentabilidad/simular                                                            │
       │    (Payload con valorVentaPen, alquilerMensualPen, distritoNombre, oerRatio, TC)               │
       ├────────────────────────────────────────────────────────────────────────────────────────────────>│
       │    Retorna Cascada (NOI, GSI) y Semáforo BCRP según el distrito (<5ms)                         │
       │<────────────────────────────────────────────────────────────────────────────────────────────────┤
       │                                                                                                 │
       │ 4. Guardado definitivo del Análisis                                                            │
       │    POST /api/v1/rentabilidad                                                                    │
       ├────────────────────────────────────────────────────────────────────────────────────────────────>│
       │    Persiste en schema 'profitability' y retorna ID para Reportes PDF                           │
       │<────────────────────────────────────────────────────────────────────────────────────────────────┤
```

---

### 7. Principio Arquitectónico de Microservicios (Database-per-Service)

Siguiendo el principio estricto de microservicios establecido para el sistema Valuo:

1. **Aislamiento de Esquemas:**
   * Cada microservicio es dueño absoluto de su propio esquema en PostgreSQL (`db_operacional_valuo`):
     - `profitability` $\rightarrow$ Microservicio de Rentabilidad (`valuo-profitability-service`).
     - `monitoring` $\rightarrow$ Microservicio de Monitoreo (`valuo-monitoring-service`).
     - `schema_usuarios` $\rightarrow$ Microservicio de Usuarios e Identidad (`msUsuarios`).
     - `schema_inmuebles` $\rightarrow$ Microservicio de Cartera de Inmuebles (`msInmuebles`).
     - `schema_predicciones` $\rightarrow$ Microservicio de Predicción (`msPrediccion`).
2. **Sin Llaves Foráneas Físicas entre Microservicios:**
   * Las columnas `agent_id`, `property_id`, `sale_prediction_id` y `rental_prediction_id` en la tabla `profitability.profitability_calculations` son **identificadores lógicos (UUIDs)**, **NO Foreign Keys reales de base de datos**.
   * Ningún microservicio realiza *JOINs* ni lee tablas de otros esquemas directamente.
   * La integridad referencial y sincronización de datos se gestiona exclusivamente a **nivel de aplicación** mediante contratos REST síncronos con timeouts definidos y objetos desacoplados (ACL / Value Objects).

---

### 8. Contrato de Endpoints REST

#### A. Creación y Persistencia del Análisis (`POST /api/v1/rentabilidad`)
El canon mensual de alquiler se recibe exclusivamente en Soles (`alquilerMensualPen`), y la venta soporta Soles o Dólares (`valorVentaPen` o `valorVentaUsd`).

```json
{
  "inmuebleId": "e3a1b2c4-8888-4444-9999-111122223333",
  "prediccionVentaId": "a1111111-2222-3333-4444-555555555555",
  "prediccionAlquilerId": "b1111111-2222-3333-4444-555555555555",
  "valorVentaPen": 1068750.00,
  "alquilerMensualPen": 5437.50,
  "oerRatio": 0.1500,
  "tipoCambioUsado": 3.7500,
  "distritoNombre": "Miraflores",
  "observaciones": "Simulación con predicciones nativas en Soles"
}
```

#### B. Simulación en Vivo en Memoria (`POST /api/v1/rentabilidad/simular`)
Ejecuta el cálculo en memoria en **< 5 milisegundos** sin tocar la base de datos:

```json
{
  "valorVentaPen": 1068750.00,
  "alquilerMensualPen": 5437.50,
  "oerRatio": 0.1800,
  "tipoCambioUsado": 3.8000,
  "distritoNombre": "Miraflores"
}
```

**Respuesta Exitosa (`200 OK`):**
```json
{
  "valoresBase": {
    "valorVentaUsd": 281250.00,
    "alquilerMensualUsd": 1430.92,
    "tipoCambioUsado": 3.8000,
    "valorVentaPen": 1068750.00,
    "alquilerMensualPen": 5437.50
  },
  "cascadaFinanciera": {
    "ingresoBrutoAnualUsd": 17171.04,
    "oerPorcentaje": 18.00,
    "gastosOperativosAnualesUsd": 3090.79,
    "noiAnualUsd": 14080.25,
    "ingresoBrutoAnualPen": 65250.00,
    "noiAnualPen": 53505.00
  },
  "metricasRetorno": {
    "capRatePorcentaje": 5.0063,
    "grossYieldPorcentaje": 6.1053,
    "aniosRetornoGrm": 16.38,
    "aniosPaybackNeto": 19.98,
    "clasificacionBenchmark": "PROMEDIO",
    "etiquetaBenchmark": "En mercado",
    "descripcionBenchmark": "Rendimiento bruto alineado con el promedio BCRP del distrito (±10%)."
  }
}
```
