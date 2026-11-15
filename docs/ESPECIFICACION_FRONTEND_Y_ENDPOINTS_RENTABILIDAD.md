# Especificación Técnica: Endpoints y Diseño de Interfaz Frontend (UI/UX)
## Módulo: `valuo-profitability-service` (Análisis de Rentabilidad Inmobiliaria)

---

### 1. Catálogo Completo de Endpoints del Microservicio

El microservicio expone dos tipos de endpoints según la arquitectura del sistema:
1. **Públicos (North-South):** Expuestos hacia el frontend Angular a través del API Gateway (`http://localhost:8080/api/v1/rentabilidad`).
2. **Internos (East-West):** Expuestos exclusivamente en la red privada para comunicación inter-servicio (`http://valuo-profitability-service:8084/internal/v1/rentabilidad`), consumido por `msReportes`.

```
                  [ Frontend Angular SPA (:4200) ]
                                 │
                                 ▼
                     [ API Gateway (:8080) ]
                                 │
     ┌───────────────────────────┴───────────────────────────┐
     │                                                       │
     ▼                                                       ▼
[ POST /simular ]                                   [ POST / ]
(Sliders reactivos <5ms)                            (Guardado definitivo)
     │                                                       │
     └───────────────────────────┬───────────────────────────┘
                                 │
                                 ▼
              [ valuo-profitability-service (:8084) ]
                                 ▲
                                 │ (Red Interna Privada)
                      [ msReportes (:8085) ]
                     [ GET /internal/v1/... ]
```

---

#### 1.1. Tabla Resumen de Endpoints

| Método | Ruta | Acceso | Propósito | Latencia Típica |
|:---:|---|:---:|---|:---:|
| `POST` | `/api/v1/rentabilidad/simular` | Público (Gateway) | Simulación en tiempo real (sliders OER y Tipo de Cambio) en memoria RAM sin tocar la BD. | `< 5 ms` |
| `POST` | `/api/v1/rentabilidad` | Público (Gateway) | Cálculo definitivo y persistencia transaccional en el esquema PostgreSQL `profitability`. | `< 40 ms` |
| `GET` | `/api/v1/rentabilidad/{id}` | Público (Gateway) | Consulta de un análisis específico por su UUID. | `< 15 ms` |
| `GET` | `/api/v1/rentabilidad/inmueble/{inmuebleId}/ultimo` | Público (Gateway) | Obtiene el último análisis generado para un inmueble (precarga de pantalla). | `< 20 ms` |
| `GET` | `/api/v1/rentabilidad/inmueble/{inmuebleId}` | Público (Gateway) | Historial completo de análisis y escenarios evaluados para un inmueble. | `< 25 ms` |
| `GET` | `/internal/v1/rentabilidad/{id}` | Privado (Directo) | Endpoint inter-servicio consumido por `msReportes` para consolidar el PDF pericial. | `< 10 ms` |

---

#### 1.2. Contratos de Datos (Requests & Responses)

##### A. Simulación Reactiva en Memoria (`POST /api/v1/rentabilidad/simular`)
* **Uso en Frontend:** Se dispara con cada movimiento del slider de OER o cambio del tipo de cambio (con un `debounceTime(150)` en RxJS).
* **Request Body:**
```json
{
  "valorVentaUsd": 150000.00,
  "alquilerMensualPen": 3187.50,
  "oerRatio": 0.1500,
  "tipoCambioUsado": 3.4400,
  "distritoNombre": "MIRAFLORES"
}
```
* **Response Body (`200 OK`):** Retorna `ProfitabilityCalculationResource` con `id: null` y sin persistencia en BD.

##### B. Guardado Definitivo (`POST /api/v1/rentabilidad`)
* **Uso en Frontend:** Se dispara cuando el asesor hace clic en el botón **"Guardar Análisis"** o **"Generar Informe PDF"**.
* **Request Body:**
```json
{
  "inmuebleId": "e7b91d2c-3891-4d1a-8f3e-821901a8c901",
  "prediccionVentaId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "prediccionAlquilerId": "f9e8d7c6-b5a4-3210-fedc-ba9876543210",
  "valorVentaUsd": 150000.00,
  "alquilerMensualPen": 3187.50,
  "oerRatio": 0.1500,
  "tipoCambioUsado": 3.4400,
  "distritoNombre": "MIRAFLORES",
  "observaciones": "Acabados de estreno, vista a parque."
}
```
* **Response Body (`201 Created`):** Retorna el mismo DTO completo pero con su `id` UUID asignado, fecha de creación `createdAt` y cabecera HTTP `Location: /api/v1/rentabilidad/{id}`.

---

### 2. Especificación de Componentes de la Interfaz Frontend (Angular)

La vista de **Análisis de Rentabilidad** está diseñada como un dashboard interactivo de una sola página (SPA), estructurado en 5 zonas visuales claras:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ [ZONA 1] Encabezado Contextual del Inmueble (Dirección, Distrito, Tags)         │
├───────────────────────────────────────┬─────────────────────────────────────────┤
│ [ZONA 2] Panel de Controles & Sliders │ [ZONA 3] Cards de Métricas Principales  │
│ • Tipo de Cambio (Input / Switch)     │ ┌───────────────┐ ┌───────────────────┐ │
│ • Slider OER (0% a 35%)               │ │ Cap Rate      │ │ Gross Yield       │ │
│ • Presets rápidos (10%, 15%, 25%)     │ │ 5.78%         │ │ 6.80%             │ │
│ • Resumen valores base (Venta/Alq)    │ └───────────────┘ └───────────────────┘ │
│                                       │ ┌───────────────┐ ┌───────────────────┐ │
│                                       │ │ Payback Neto  │ │ Años Retorno GRM  │ │
│                                       │ │ 17.30 años    │ │ 14.71 años        │ │
│                                       │ └───────────────┘ └───────────────────┘ │
├───────────────────────────────────────┴─────────────────────────────────────────┤
│ [ZONA 4] Semáforo de Mercado & Benchmark Oficial BCRP                           │
│ • Badge de Estado ("Sobre mercado" / "En mercado" / "Bajo mercado")             │
│ • Gráfico comparativo: Retorno Bruto Inmueble vs Promedio BCRP del Distrito     │
├─────────────────────────────────────────────────────────────────────────────────┤
│ [ZONA 5] Cascada Operativa Bimoneda (Tabla Desglosada GSI -> OpEx -> NOI)       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ [BARRA INFERIOR] Botones de Acción ("Guardar", "Exportar PDF") & Aviso Legal    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2.1. ZONA 1: Encabezado Contextual del Inmueble
Muestra la información de referencia traída desde `msInmuebles` para que el asesor sepa exactamente qué propiedad está analizando.
* **Elementos Visuales:**
  * **Título:** `"Evaluación de Rentabilidad Financiera"`.
  * **Breadcrumb:** `Cartera de Inmuebles > Depto. Av. Larco 450 > Rentabilidad`.
  * **Badges informativos:**
    * Distrito: `Miraflores` (Pill azul oscuro).
    * Tipo de inmueble: `Departamento Residencial` (Pill gris).
    * Superficie: `85.00 m²` | Dormitorios: `3` | Antigüedad: `2 años`.

---

#### 2.2. ZONA 2: Panel de Parámetros y Sliders de Sensibilidad (Inputs)

Este panel permite al asesor ajustar las variables operativas en vivo:

1. **Control de Tipo de Cambio ($TC$):**
   * **Tipo de componente:** Input numérico con botón de restauración a valor oficial.
   * **Label:** `"Tipo de Cambio Aplicado (USD/PEN)"`.
   * **Valor inicial por defecto:** `3.4400`.
   * **Ayuda contextual:** *"Se aplica el mismo TC a venta y alquiler para evitar distorsiones cambiarias."*
   * **Comportamiento:** Al modificarlo, recalcula inmediatamente la simulación.

2. **Slider Interactivo de Gastos Operativos (OER - Operating Expense Ratio):**
   * **Tipo de componente:** Slider de rango continuo (`mat-slider` / HTML5 range).
   * **Rango:** `0.00%` a `35.00%` (Paso: `0.5%`).
   * **Valor inicial por defecto:** `15.00%` (Línea base recomendada para Lima).
   * **Display numérico:** Badge dinámico sobre el slider que muestra el porcentaje en tiempo real (ej. `15.0%`).
   * **Botones de Presets Rápidos (Quick Selectors):**
     * `[Bajo: 10%]` (Inmueble nuevo, vacancia mínima, sin comisión de administración).
     * `[Estándar: 15%]` (Recomendado mercado Lima: incluye 1 mes de vacancia cada 2 años + mantenimiento).
     * `[Conservador: 20%]` (Mayor previsión de vacancia y reparaciones periódicas).
     * `[Alto: 25%]` (Edificios antiguos o con cuotas de mantenimiento extraordinarias elevadas).

3. **Cajas Informativas de Valores Base (Insumos de Machine Learning):**
   * **Valor Venta Estimado:** Mostrar en Dólares y Soles (ej. `$150,000 USD` / `S/. 562,500 PEN`).
   * **Alquiler Mensual Estimado:** Mostrar en Soles y Dólares (ej. `S/. 3,187.50 PEN` / `$850.00 USD`).
   * Tag de procedencia: *"Inferencia ML XGBoost (msPrediccion)"*.

---

#### 2.3. ZONA 3: Cards de Métricas Clave (KPI Cards)

Cuatro tarjetas con diseño limpio, números grandes y jerarquía tipográfica:

| Tarjeta | Campo JSON | Formato en UI | Descripción / Tooltip para el Asesor |
|---|---|---|---|
| **1. Cap Rate Unlevered** | `metricasRetorno.capRatePorcentaje` | **`5.78%`** (Verde / Resaltado) | **Tasa de Capitalización No Apalancada.** Rendimiento operativo neto anual respecto al valor total del activo, asumiendo compra al contado. |
| **2. Gross Rental Yield** | `metricasRetorno.grossYieldPorcentaje` | **`6.80%`** | **Rendimiento Bruto Anual.** Relación entre los ingresos brutos anuales y el precio de compra, antes de gastos operativos. |
| **3. Payback Operativo Neto** | `metricasRetorno.aniosPaybackNeto` | **`17.30 años`** | **Tiempo de Retorno Real.** Años necesarios para recuperar la inversión considerando el flujo de caja neto en mano (descontando OpEx). |
| **4. Años Retorno Bruto (PER)** | `metricasRetorno.aniosRetornoGrm` | **`14.71 años`** | **Multiplicador de Renta Bruta.** Ratio idéntico al PER publicado por el BCRP. Permite la comparación estandarizada entre distritos. |

---

#### 2.4. ZONA 4: Semáforo y Benchmark Oficial BCRP

##### A. Justificación y Evolución del Semáforo
En el diseño inicial del sistema, se evaluó un semáforo genérico con umbrales fijos para toda la ciudad (ej. menor a 4.5% bajo, 4.5% a 6.5% promedio, mayor a 6.5% alto). Sin embargo, este enfoque fue descartado y evolucionado porque penalizaba distritos consolidados como San Isidro o Miraflores (donde la rentabilidad histórica normal ronda el 5.0% - 5.7% debido a la alta plusvalía y menor riesgo) y premiaba de forma descontextualizada a distritos periféricos de mayor riesgo. 

El semáforo actual se basa en un **Benchmark Distrital Oficial** que contrasta el **Gross Rental Yield del inmueble** contra el rendimiento promedio derivado del ratio **PER trimestral del BCRP (T4-2025)** del distrito donde se ubica la propiedad, aplicando una **banda de tolerancia relativa del $\pm 10\%$**:

$$\text{Rango "En mercado" (PROMEDIO)} = \left[ \text{GrossYield}_{\text{distrito}} \times 0.90 \;,\; \text{GrossYield}_{\text{distrito}} \times 1.10 \right]$$

##### B. Especificación Visual de los 4 Estados del Semáforo

| Estado (`clasificacionBenchmark`) | Condición Matemática | Clases UI (Tailwind / CSS) | Icono | Mensaje Dinámico en Frontend |
|:---:|---|---|:---:|---|
| **`ATRACTIVO`** <br> *(Sobre mercado)* | $\text{GrossYield} > 1.10 \times \text{BCRP}$ | `bg-emerald-50 text-emerald-800 border-emerald-300 ring-emerald-500` | `TrendingUp` (Verde esmeralda) | *"Rendimiento superior en más de 10% al promedio distrital BCRP. Excelente oportunidad de inversión."* |
| **`PROMEDIO`** <br> *(En mercado)* | $0.90 \times \text{BCRP} \le \text{GrossYield} \le 1.10 \times \text{BCRP}$ | `bg-blue-50 text-blue-800 border-blue-300 ring-blue-500` | `Scale` / `CheckCircle` (Azul corporativo) | *"Rendimiento alineado con el promedio BCRP del distrito (±10%). Valores en equilibrio de mercado."* |
| **`BAJO`** <br> *(Bajo mercado)* | $\text{GrossYield} < 0.90 \times \text{BCRP}$ | `bg-amber-50 text-amber-800 border-amber-300 ring-amber-500` | `AlertTriangle` (Ámbar / Naranja) | *"Rendimiento más de 10% por debajo del promedio distrital BCRP. Evaluar ajuste en precio de adquisición o canon."* |
| **`SIN_REFERENCIA`** <br> *(Sin dato BCRP)* | Distrito sin serie histórica (10 distritos) | `bg-slate-100 text-slate-700 border-slate-300 ring-slate-400` | `HelpCircle` / `Info` (Gris neutro) | *"El BCRP no publica serie histórica de ratio PER para este distrito. Se muestran los indicadores sin contraste distrital."* |

> [!NOTE]
> **Comportamiento en vivo de los Sliders:**
> Mover los sliders de OER (Gastos Operativos) o Tipo de Cambio **modifica el Cap Rate y el Payback Neto**, pero **NO altera el estado del semáforo**. El semáforo evalúa la rentabilidad bruta intrínseca del activo frente al distrito, por lo que solo varía si se modifica el precio de venta, el canon de alquiler o el distrito.

##### C. Componente Gráfico Comparativo (Visual Gauge / Dual Bar)
Debajo del badge del semáforo, el frontend renderiza una comparativa visual horizontal:
1. **Barra 1 (Inmueble evaluado):** Gross Yield en % y PER en años (ej. `6.80%` | `14.71 años`).
2. **Barra 2 (Promedio del distrito):** Gross Yield distrital BCRP y PER distrital (ej. Miraflores: `5.71%` | `17.50 años`).
3. **Delta Diferencial:** Badge con el diferencial porcentual relativo (ej. `+19.1% vs promedio distrital`).
4. **Nota Metodológica al pie:** *"Fuente: Banco Central de Reserva del Perú (BCRPData) - Serie Trimestral PER T4-2025."*

##### D. Ejemplo de Plantilla Angular (HTML / Tailwind)
```html
<div class="rounded-xl border p-5 shadow-sm" [ngClass]="{
  'bg-emerald-50/50 border-emerald-200': res.metricasRetorno.clasificacionBenchmark === 'ATRACTIVO',
  'bg-blue-50/50 border-blue-200': res.metricasRetorno.clasificacionBenchmark === 'PROMEDIO',
  'bg-amber-50/50 border-amber-200': res.metricasRetorno.clasificacionBenchmark === 'BAJO',
  'bg-slate-50 border-slate-200': res.metricasRetorno.clasificacionBenchmark === 'SIN_REFERENCIA'
}">
  <div class="flex items-center justify-between">
    <div class="flex items-center gap-2.5">
      <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider"
            [ngClass]="getBadgeClasses(res.metricasRetorno.clasificacionBenchmark)">
        <lucide-icon [name]="getBenchmarkIcon(res.metricasRetorno.clasificacionBenchmark)" class="w-4 h-4"></lucide-icon>
        {{ res.metricasRetorno.etiquetaBenchmark }}
      </span>
      <span class="text-sm font-medium text-slate-600">Benchmark Distrital: {{ res.valoresBase.distritoNombre }}</span>
    </div>
    <span class="text-xs text-slate-400 font-mono">BCRP T4-2025</span>
  </div>

  <p class="mt-3 text-sm text-slate-700 leading-relaxed">
    {{ res.metricasRetorno.descripcionBenchmark }}
  </p>

  <!-- Barra comparativa visual si el distrito tiene dato BCRP -->
  <div *ngIf="res.metricasRetorno.clasificacionBenchmark !== 'SIN_REFERENCIA'" class="mt-4 pt-4 border-t border-slate-200/60">
    <div class="grid grid-cols-2 gap-4 text-xs font-medium">
      <div>
        <span class="text-slate-500">Rendimiento Inmueble (Gross Yield):</span>
        <span class="block text-base font-bold text-slate-900">{{ res.metricasRetorno.grossYieldPorcentaje }}% (PER: {{ res.metricasRetorno.aniosRetornoGrm }} a.)</span>
      </div>
      <div>
        <span class="text-slate-500">Promedio Oficial Distrito (BCRP):</span>
        <span class="block text-base font-bold text-slate-700">{{ getDistrictGrossYield(res.valoresBase.distritoNombre) }}% (PER: {{ getDistrictPer(res.valoresBase.distritoNombre) }} a.)</span>
      </div>
    </div>
  </div>
</div>
```

---

#### 2.5. ZONA 5: Cascada Operativa Bimoneda (Tabla Financiera)

Tabla interactiva que transparenta el cálculo matemático hacia el cliente:

| Concepto Operativo | Fórmula | Columna USD | Columna PEN |
|---|---|:---:|:---:|
| **(+) Ingreso Bruto Anual (GSI)** | Canon mensual $\times 12$ | `$10,200.00` | `S/. 38,250.00` |
| **(-) Gastos Operativos (OpEx)** | $\text{GSI} \times 15.0\%$ | `-$1,530.00` | `-S/. 5,737.50` |
| **(=) Ingreso Operativo Neto (NOI)** | $\text{GSI} - \text{OpEx}$ | **`$8,670.00`** | **`S/. 32,512.50`** |
| **Valor Comercial del Activo** | Base de tasación | `$150,000.00` | `S/. 562,500.00` |

---

#### 2.6. BARRA INFERIOR: Acciones y Aviso Legal

1. **Campo de Observaciones:**
   * Textarea opcional: *"Notas o supuestos del asesor (ej. remodelación prevista, negociación de canon)"*.
2. **Botones de Acción:**
   * **`[Restablecer]` (Secundario / Outline):** Devuelve el slider OER a 15.0% y el TC a 3.4400.
   * **`[Guardar Análisis]` (Primario / Sólido Azul):**
     * Llama a `POST /api/v1/rentabilidad`.
     * Despliega toast de confirmación: *"Análisis de rentabilidad guardado con éxito"*.
     * Actualiza el historial de evaluaciones del inmueble.
   * **`[Descargar Informe PDF]` (Botón Icono / Verde o Rojo PDF):**
     * Habilitado tras guardar. Invoca a `msReportes` pasando el `id` para descargar la ficha ejecutiva pericial.
3. **Aviso Legal Normativo (Footer Disclaimer):**
   * Párrafo en texto gris pequeño (11px):
   > *"Aviso legal: Este indicador refleja el rendimiento para una compra al contado (unlevered) y constituye una referencia comparativa. No contempla el financiamiento hipotecario del comprador ni sustituye una tasación reglamentaria según el Reglamento Nacional de Tasaciones del Perú."*

---

### 3. Flujo Reactivo de Datos en Angular (Arquitectura Frontend)

```typescript
// Ejemplo de servicio Angular: ProfitabilityService
@Injectable({ providedIn: 'root' })
export class ProfitabilityService {
  private readonly gatewayUrl = 'http://localhost:8080/api/v1/rentabilidad';

  constructor(private http: HttpClient) {}

  // 1. Simulación reactiva para sliders (<5ms)
  simulate(payload: SimulateSensitivityResource): Observable<ProfitabilityCalculationResource> {
    return this.http.post<ProfitabilityCalculationResource>(`${this.gatewayUrl}/simular`, payload);
  }

  // 2. Persistencia definitiva
  save(payload: CalculateProfitabilityResource): Observable<ProfitabilityCalculationResource> {
    return this.http.post<ProfitabilityCalculationResource>(this.gatewayUrl, payload);
  }

  // 3. Consulta de histórico o precarga
  getLatestByInmueble(inmuebleId: string): Observable<ProfitabilityCalculationResource> {
    return this.http.get<ProfitabilityCalculationResource>(`${this.gatewayUrl}/inmueble/${inmuebleId}/ultimo`);
  }
}
```

```typescript
// En el componente Angular: manejo con RxJS debounce
this.oerControl.valueChanges.pipe(
  debounceTime(150),
  distinctUntilChanged(),
  switchMap(oer => this.profitabilityService.simulate({
    valorVentaUsd: this.inmueble.precioVentaUsd,
    alquilerMensualPen: this.inmueble.alquilerPen,
    oerRatio: oer / 100,
    tipoCambioUsado: this.tcControl.value,
    distritoNombre: this.inmueble.distrito
  }))
).subscribe(res => {
  this.metrics = res.metricasRetorno;
  this.cascade = res.cascadaFinanciera;
});
```

