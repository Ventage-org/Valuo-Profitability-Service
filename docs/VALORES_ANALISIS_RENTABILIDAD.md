# Guía de valores del Análisis de Rentabilidad (para presentación)

Pantalla: `/rentabilidad/:id` · Servicio: `valuo-profitability-service` · Fuente de datos: `msprediccion` (modelo XGBoost) + benchmark BCRP.

---

## 1. ¿Por qué venta en dólares y alquiler en soles?

Es una **convención del mercado peruano**, y la pantalla la respeta:

| Operación | Moneda habitual en Lima | Cómo se muestra |
| :-- | :-- | :-- |
| Compra-venta de inmuebles | **USD** | `$ xxx USD` (≈ S/. equivalente) |
| Alquiler residencial | **PEN (soles)** | `S/. xxx PEN` (≈ USD/mes) |

Importante para explicarlo bien:
- El modelo de ML predice **ambos en soles nominales**. La conversión a USD se hace con **un único tipo de cambio** (por defecto 3.44, editable), el mismo para venta y alquiler.
- Usar el mismo TC hace que **Cap Rate, Gross Yield y años de retorno no cambien** al mover el TC (son ratios). Solo cambian los montos absolutos en USD.
- Hay un selector `$ USD / S/. Soles` que cambia la moneda de NOI, OpEx y GSI en el desglose.

---

## 2. Datos de entrada

| Valor | Origen | Descripción |
| :-- | :-- | :-- |
| **Valor comercial de venta (V_venta)** | Predicción de venta (`msprediccion`) | Precio de mercado estimado del inmueble. |
| **Alquiler mensual (P_alquiler)** | Predicción de alquiler (`msprediccion`) | Canon mensual estimado. |
| **Tipo de cambio (TC)** | Usuario / default `3.44` | Soles por dólar. Rango del input: 2.5 – 5.5. |
| **OER** (Operating Expense Ratio) | Usuario / default `15%` | % del ingreso bruto que se va en gastos. Rango 0 – 35%. |
| **Distrito** | Ficha del inmueble (`msinmuebles`) | Se usa solo para el benchmark BCRP. |

Presets de OER: 10% bajo · **15% estándar Lima** · 20% conservador · 25% alto. (Criterio del proyecto, no valor oficial.)

---

## 3. Cascada financiera (flujo anual, Unlevered)

Unlevered = se asume compra **al contado**, sin hipoteca.

| Concepto | Fórmula | Qué significa |
| :-- | :-- | :-- |
| **GSI** – Ingreso bruto anual | `P_alquiler × 12` | Lo que se cobraría en un año con 100% de ocupación. |
| **OpEx** – Gastos operativos anuales | `GSI × OER` | Mantenimiento, impuestos, vacancia, administración, seguros. |
| **NOI** – Ingreso operativo neto | `GSI − OpEx` | Lo que queda en mano del propietario. |
| Versión PEN | `monto USD × TC` | Mismo valor en soles. |

---

## 4. Indicadores de retorno

| Indicador | Fórmula | Lectura |
| :-- | :-- | :-- |
| **Cap Rate Unlevered** | `(NOI / V_venta) × 100` | Rentabilidad **neta** anual del inmueble sin deuda. Es la métrica estrella. |
| **Gross Rental Yield** | `(GSI / V_venta) × 100` | Rentabilidad **bruta** anual (antes de gastos). Es la que se compara con el BCRP. |
| **Años de retorno (PER / GRM)** | `V_venta / GSI` | Años de alquiler bruto para igualar el precio. Es el inverso del Gross Yield. |
| **Payback operativo neto** | `V_venta / NOI` | Años reales de recuperación descontando gastos. Siempre ≥ PER. |

Relación útil para la exposición: `PER = 100 / Gross Yield` y `Payback = 100 / Cap Rate`.

**Ejemplo (Miraflores):** Venta S/. 1,068,750 · Alquiler S/. 5,437.50/mes · TC 3.80 · OER 18%
→ GSI USD 17,171 · NOI USD 14,080 · Cap Rate 5.01% · Gross Yield 6.11% · PER 16.4 años · Payback 20.0 años.

---

## 5. Benchmark distrital (semáforo BCRP)

- **Fuente:** indicador **PER = Precio de venta / Alquiler anual** del BCRP (BCRPData, trimestral). Período cargado: **T4-2025**.
- **Cobertura:** solo **12 distritos** (Barranco, Jesús María, La Molina, Lince, Magdalena, Miraflores, Pueblo Libre, San Borja, San Isidro, San Miguel, Surco, Surquillo). Los demás muestran **"Sin dato BCRP"**.
- **Rendimiento bruto de mercado** = `100 / PER_BCRP`.
- **Comparación:** Gross Yield del inmueble vs. rendimiento bruto del distrito (bruto contra bruto, porque el PER del BCRP no descuenta gastos).

#### ¿De dónde sale el "rendimiento bruto de mercado"?

**No es un dato publicado por el BCRP: es un valor derivado.** El único dato oficial es el **PER** (años). El porcentaje se calcula en [`DistrictMarketBenchmark`](../src/main/java/pe/ventage/valuo/profitability/domain/model/valueobjects/DistrictMarketBenchmark.java) con:

```
Rendimiento bruto de mercado (%) = 100 / PER_BCRP
```

**Por qué es matemáticamente válido:** el PER del BCRP es `Precio de venta / Alquiler anual`. El rendimiento bruto es su inverso: `(Alquiler anual / Precio) × 100`. Por tanto, `100 / PER` es exactamente el rendimiento bruto implícito en el PER.

| Concepto | Del inmueble (predicho por el modelo) | Del mercado (BCRP) |
| :-- | :-- | :-- |
| **PER (años)** | `Venta / GSI` | Dato oficial BCRP por distrito |
| **Rendimiento bruto (%)** | `(GSI / Venta) × 100` | `100 / PER_BCRP` (derivado) |

Ejemplo (Miraflores): PER 17.50 → `100 / 17.50 = 5.71%`. Si un departamento en Miraflores rinde 6.4% bruto, equivale a un PER de 15.6 años y queda un +12% sobre el mercado → `ATRACTIVO`.

**Implicaciones para la exposición:**
1. El dato oficial es el PER en años; el porcentaje es una conversión, no una cifra aparte del BCRP.
2. Se compara **bruto contra bruto** porque el PER no descuenta gastos operativos. Por eso el semáforo usa Gross Yield y no Cap Rate.
3. Los PER son una tabla estática del trimestre T4-2025, cargada a mano (sin llamadas al BCRP en ejecución).
4. El PER del BCRP se construye con precios de **oferta** de avisos, no con transacciones cerradas. Presentarlo como *referencia de mercado*, no como rentabilidad real garantizada.
5. La banda de ±10% del semáforo es criterio del proyecto, no oficial.

| Semáforo | Condición | Mensaje |
| :-- | :-- | :-- |
| 🟢 **ATRACTIVO** | Gross Yield > 110% del mercado | Sobre mercado (+x%) |
| 🟡 **PROMEDIO** | Entre 90% y 110% | En mercado |
| 🔴 **BAJO** | < 90% | Bajo mercado (−x%) |
| ⚪ **SIN_REFERENCIA** | Distrito sin serie BCRP | Sin dato BCRP |

La banda de ±10% es **criterio del proyecto**, no un valor oficial del BCRP.
Mover OER o TC **no cambia el semáforo** (solo depende de precio, alquiler y distrito).

Tabla de referencia:

| Distrito | PER (años) | Rend. bruto mercado |
| :-- | :-- | :-- |
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

---

## 6. Qué se ve en cada bloque de la pantalla

| Bloque | Valores mostrados |
| :-- | :-- |
| **4 tarjetas superiores** | Cap Rate · Gross Yield · NOI anual · Años de retorno (PER) |
| **Entradas ML y sensibilidad** | Valor de venta (USD ≈ PEN) · Alquiler mensual (PEN ≈ USD) · TC · slider OER con OpEx anual |
| **Desglose en cascada** | GSI · OpEx · NOI · V_venta (en USD o PEN según selector) |
| **Benchmark distrital** | Etiqueta del semáforo · diferencia % vs. distrito · Gross Yield del inmueble vs. BCRP · PER de ambos |
| **Informe ejecutivo (A4)** | Resumen de todo lo anterior + Payback operativo, para el PDF |

---

## 7. Persistencia y trazabilidad

- **Guardar análisis** (`POST /api/v1/rentabilidad`) almacena en el esquema `profitability`: GSI, OpEx, NOI (USD y PEN), Cap Rate, Gross Yield, GRM, Payback, categoría de benchmark, OER y **TC usado** (`exchange_rate_used`), más los IDs lógicos de inmueble y predicciones.
- Simular con sliders (`POST /rentabilidad/simular`) es **en memoria**, sin tocar la base de datos.
- Los IDs entre servicios son UUID lógicos, sin llaves foráneas entre esquemas.

---

## 8. Limitaciones a mencionar con honestidad

1. Cap Rate **unlevered**: no considera hipoteca ni apalancamiento.
2. El OER es un **supuesto** del usuario, no un dato predicho.
3. El benchmark BCRP cubre **12 de 22 distritos** y se actualiza manualmente por trimestre.
4. Las predicciones tienen error (MAPE del modelo de venta/alquiler); los indicadores heredan esa incertidumbre.
5. Solo aplica a **departamentos** (el modelo no se entrenó con casas).
6. No incluye vacancia explícita, impuestos específicos ni apreciación del inmueble en el tiempo.

---

## 9. Guion corto sugerido (30 segundos)

> "Tomamos el precio de venta y el alquiler que predice nuestro modelo. El alquiler anual es el ingreso bruto; le restamos un ratio de gastos para obtener el NOI. El Cap Rate es el NOI sobre el precio: la rentabilidad neta sin deuda. Además comparamos el rendimiento bruto contra el PER oficial del BCRP del distrito y lo mostramos como semáforo. La venta se muestra en dólares y el alquiler en soles porque así opera el mercado peruano."
