**UPC**

**TALLER DE PROYECTOS I**

**SOLUCIÓN WEB BASADA EN EL ALGORITMO XGBOOST PARA LA VALORACIÓN Y** **EL** **ANÁLISIS DE RENTABILIDAD DE DEPARTAMENTOS RESIDENCIALES EN AGENCIAS INDEPENDIENTES DE LIMA METROPOLITANA**  **** **PROJECT CHARTER**

**VERSIÓN** **1.2**

Preparado por:

| Jefe de Proyecto: | Joaquin Cortez |
| :--- | :--- |
| Especialidad: | Ingeniería de Software |
| Empresa/Grupo de Investigación: | Ventage |
Historial de Revisiones

| **Versión** | **Fecha** | **Autor** | **Descripción** |
| :---: | :---: | :---: | :---: |
| 1.0 | 02/09/2026 | Carlos Pingus | Redacción de la primera versión del Project Charter |
| 1.1 | 09/09/2026 | Joaquin Cortez | Redacción de la segunda versión del Project Charter |
| 1.2 | 23/09/2026 | Joaquin Cortez | Redacción de la tercera versión del Project Charter |
|  |  |  |  |
# Índice de Contenidos
1. Resumen ejecutivo

2. Marco Teórico

3. Posicionamiento

  - 3.1. Motivación

  - 3.2. Planteamiento del problema

  - 3.3. Objetivos

    - 3.3.1. Objetivo General

    - 3.3.2. Objetivos Específicos

  - 3.4. Indicadores de Éxito

  - 3.5. Alcance del Proyecto

  - 3.6. Suposiciones y Restricciones

  - 3.7. Contribución e Impacto del Proyecto de Investigación

1. Organización del Proyecto

  - 4.1. Responsable del Proyecto

  - 4.2. Stakeholders, Beneficiarios o Usuarios

  - 4.3. Recursos requeridos

  - 4.4. Fases e hitos del proyecto

  - 4.5. Enfoques del trabajo

  - 4.6. Riesgos y Mitigación

1. Glosario

2. Siglario

3. Bibliografía

4. Aprobación

5. Anexos

# 1. Resumen ejecutivo
El presente proyecto de investigación propone el desarrollo de un sistema web basado en algoritmos de Machine Learning que permita a las agencias inmobiliarias independientes de Lima Metropolitana estimar el valor comercial de inmuebles residenciales y calcular indicadores de rentabilidad de forma objetiva, reproducible y explicable.

El sector de intermediación inmobiliaria independiente en Lima Metropolitana enfrenta una problemática crítica caracterizada por la alta subjetividad y variabilidad en la tasación comercial. El mercado peruano ha sido clasificado como de baja transparencia de datos (JLL & LaSalle Investment Management, 2024), una condición agravada por la ausencia de un asiento cartográfico catastral único que permita vincular con precisión la ubicación física y el valor económico de las propiedades (Limachi, 2023). Esta carencia estructural se encuentra reconocida incluso en el propio marco normativo del sector: el Reglamento Nacional de Tasaciones del Perú (Resolución Ministerial N° 172-2016-VIVIENDA, modificada por la Resolución Ministerial N° 124-2020-VIVIENDA) delega en el perito la recopilación informal de muestras de mercado para la tasación comercial, mientras que el Reglamento del Registro de Peritos Valuadores (Resolución SBS N° 03225-2021) certifica la idoneidad profesional de los tasadores sin estandarizar las fuentes de datos que estos emplean. En este entorno de alta heterogeneidad distrital y variabilidad de criterios humanos (Tapia et al., 2025), las tasaciones empíricas resultantes divergen sistemáticamente de la realidad transaccional, prolongando la latencia en las negociaciones y mermando la tasa de cierre comercial de estas organizaciones.

La solución propuesta consiste en una plataforma web que integra dos modelos predictivos entrenados con el algoritmo XGBoost —uno para precio de venta y otro para alquiler mensual—, un componente de explicabilidad basado en valores SHAP y un módulo de análisis financiero que calcula la Tasa de Capitalización (Cap Rate). El sistema se construirá sobre una arquitectura de tres capas con Angular en el frontend, Spring Boot en el backend y PostgreSQL, incorporando prácticas de MLOps mediante MLflow y Docker.

El proyecto se ejecutará durante el ciclo académico 2026-2, entre el 24 de agosto y el 12 de diciembre de 2026, bajo el marco de trabajo Scrum con un Sprint 0 de preparación técnica y tres sprints de desarrollo. La gestión del ciclo de vida del modelo predictivo seguirá la metodología CRISP-DM. El caso de estudio es MAK Inmobiliaria, empresa peruana del sector con más de cuatro décadas de trayectoria, cuyo Departamento Comercial y de Gestión de Inmuebles servirá como entorno de validación funcional y de usabilidad.

Los principales entregables comprenden el presente Project Charter, el documento de arquitectura del sistema y el repositorio de datos consolidado de naturaleza espacio-temporal. Este repositorio integra de manera estructurada los registros transaccionales históricos del Banco Central de Reserva del Perú (BCRP) para el periodo 2016–2025, correspondientes al mercado de departamentos residenciales, detallando variables estructurales físicas del predio (superficie, habitaciones, baños, garajes, nivel de piso, vista exterior y antigüedad), variables macroeconómicas de control temporal (año, trimestre, tipo de cambio nominal, IPC y el cálculo del precio en soles constantes para neutralizar el sesgo inflacionario) y el precio por metro cuadrado en dólares como variable objetivo a predecir. Asimismo, este repositorio consolida variables socioeconómicas y demográficas calculadas a partir de la Encuesta Nacional de Hogares (ENAHO) y proyecciones de población del Instituto Nacional de Estadística e Informática (INEI), métricas de criminalidad distrital y variables geoespaciales de accesibilidad e infraestructura de entorno urbano derivadas mediante OpenStreetMap. Los entregables técnicos finales se completan con los modelos predictivos de venta y alquiler entrenados, optimizados y versionados, el sistema web funcional y la Memoria de Tesis. La contribución esperada es una reducción medible del error porcentual de valoración respecto de los métodos empíricos actualmente empleados, junto con una base metodológica replicable para mercados con infraestructura de datos limitada. 

# 2. Marco Teórico
## Valoración inmobiliaria y modelos automatizados
La valoración inmobiliaria consiste en la estimación del valor de mercado de un activo a partir de sus atributos físicos, su localización y las condiciones económicas vigentes. Los enfoques tradicionales comprenden el método comparativo de mercado, el enfoque de costos y la capitalización de rentas, todos ellos dependientes en distinto grado del criterio del tasador. El estándar internacional que rige esta práctica es el International Valuation Standards (International Valuation Standards Council, 2025), mientras que en el ámbito nacional el ejercicio del corretaje está regulado por la Ley N.° 29080 (Congreso de la República, 2007).

Frente a las limitaciones de los métodos manuales han surgido los Modelos de Valoración Automatizada (AVM), sistemas que estiman el valor de un inmueble mediante procedimientos estadísticos o algorítmicos sobre bases de datos de transacciones. El Jaouhari et al. (2024) documentan en su revisión sistemática que los AVM han evolucionado desde la regresión hedónica clásica hacia arquitecturas basadas en aprendizaje automático, con mejoras sustantivas en precisión cuando el mercado presenta relaciones no lineales entre las variables predictoras y el precio.
## Aprendizaje automático supervisado y XGBoost
El aprendizaje automático supervisado permite construir funciones de predicción a partir de ejemplos etiquetados. Dentro de esta familia, los métodos de ensamble basados en árboles de decisión han mostrado un desempeño consistentemente superior en problemas de datos tabulares. XGBoost, implementación optimizada del gradient boosting, construye árboles de forma secuencial de modo que cada nuevo árbol minimiza los errores residuales del anterior mediante descenso de gradiente, destacando por su velocidad, eficiencia y capacidad de escalar con grandes volúmenes de datos (Ali et al., 2023).

La evidencia empírica respalda esta elección para el dominio inmobiliario. Hassoon et al. (2026) reportan que XGBoost superó a redes neuronales artificiales y a regresión por vectores de soporte en un problema de predicción con muestra reducida y relaciones no lineales. Noh y Park (2026) documentan que los modelos basados en árboles superaron a la regresión por mínimos cuadrados ordinarios en tres mercados urbanos distintos, con mejoras de R² entre 0.10 y 0.35. En el contexto peruano, Oporto D’Ugard et al. (2025) obtuvieron con XGBoost un MAPE de 17.89% empleando características físicas y variables geográficas de inmuebles de una entidad financiera nacional, resultado que constituye el punto de comparación local más cercano para este proyecto.
## Explicabilidad mediante valores SHAP
La adopción de modelos de alta capacidad predictiva introduce el problema de la opacidad: un asesor inmobiliario debe poder sustentar ante su cliente por qué el sistema propone un valor determinado. Los valores SHAP (SHapley Additive exPlanations) cuantifican la contribución marginal de cada variable a una predicción individual, permitiendo traducir el resultado del modelo en argumentos comprensibles (Ponce-Bobadilla et al., 2024).

Kucklick y Müller (2023) analizan explícitamente el compromiso entre precisión e interpretabilidad en tasación inmobiliaria y sostienen que la explicabilidad no es un complemento opcional sino un requisito funcional en dominios de alto impacto financiero. Kee y Ho (2025) demuestran la aplicabilidad conjunta de XGBoost y valores de Shapley en predicción de precios, y Tanashkin et al. (2026) refuerzan que en contextos donde la valoración debe sustentarse ante terceros la capacidad de explicación resulta determinante.
## Variables predictoras y entorno urbano
La literatura converge en que las características físicas del inmueble no bastan para explicar el precio. Liu et al. (2024) organizan las variables del entorno construido en seis dimensiones —densidad, diversidad, despliegue ambiental, diseño interior, proximidad al destino y distancia al tránsito— y muestran que la distancia al transporte y el diseño interior concentran el mayor peso predictivo. Rey-Blanco et al. (2024) construyen índices de accesibilidad peatonal y vehicular e incorporan la dimensión espacial a modelos de regresión y Random Forest. Genc y Colak (2026) analizan la heterogeneidad espacial de los precios mediante sistemas de información geográfica y regresión ponderada geográficamente. Este cuerpo de evidencia fundamenta la decisión de enriquecer el dataset del proyecto con variables geoespaciales, socioeconómicas y de seguridad ciudadana.
## Análisis de rentabilidad: Cap Rate
La predicción de un precio adquiere valor comercial cuando se traduce en indicadores de decisión. La Tasa de Capitalización (Cap Rate) relaciona el ingreso operativo neto con el valor del activo, constituyendo el indicador estándar de la industria para comparar la rentabilidad esperada entre distintas propiedades cuando no se dispone de información sobre las condiciones específicas de financiamiento del comprador; Manganelli et al. (2024) analizan su vínculo con los factores de riesgo del mercado residencial. Patrocínio et al. (2024) advierten que no existe una única formulación matemática para los indicadores de retorno de inversión inmobiliaria, y que la elección del método de cálculo puede conducir a conclusiones distintas sobre un mismo activo, lo que obliga a documentar explícitamente la fórmula y los supuestos adoptados. Bajo este criterio, el presente proyecto calcula el Cap Rate en su forma unlevered, es decir, asumiendo la ausencia de financiamiento hipotecario, dado que no se dispone de datos granulares sobre las condiciones de crédito de cada comprador. Esta aproximación permite al asesor contrastar de forma consistente la rentabilidad relativa entre distintas propiedades, si bien no representa el retorno neto que obtendría un comprador que financie la operación mediante crédito hipotecario, distinción que el sistema comunica explícitamente al usuario junto con el resultado del indicador.

## Métricas de evaluación y validación
La evaluación del desempeño predictivo se sustenta en el error absoluto medio (MAE), la raíz del error cuadrático medio (RMSE), el error porcentual absoluto medio (MAPE) y el coeficiente de determinación (R²). Hoxha y Hoxha (2026) argumentan que ninguna métrica aislada es suficiente y que la combinación de indicadores de escala y de proporción explicada permite detectar sobreajuste. En cuanto a la validación, Deng y Zhang (2026) y Çılgın y Gökçen (2025) establecen la validación cruzada de k pliegues como estándar metodológico, mientras que Noh y Park (2026) advierten sobre la filtración de información espacial cuando las particiones se realizan de forma aleatoria sin criterio territorial, y Mathotaarachchi et al. (2024) documentan caídas de precisión al evaluar modelos sobre conjuntos externos.
## Arquitectura de software y operación del modelo
La utilidad práctica de un modelo predictivo depende de su integración en una plataforma accesible. Maia y David (2024) demuestran la viabilidad de desplegar modelos de tasación dentro de aplicaciones web accesibles a usuarios sin formación técnica. Rodrigues et al. (2025) proponen una arquitectura distribuida de MLOps orientada al despliegue y actualización continua de modelos, y Acharya et al. (2026) desarrollan un marco automatizado de MLOps para escenarios predictivos industriales. Estos aportes fundamentan la incorporación de MLflow para el versionado de experimentos y de Docker para la contenedorización del sistema.

La selección concreta de tecnologías se sustenta en el benchmarking realizado como parte de la investigación previa: XGBoost como motor predictivo, Spring Boot como framework de backend por su seguridad integrada y su ecosistema de microservicios, y PostgreSQL como gestor de base de datos por su conectividad directa y eficiencia tanto con Spring Data JPA/Hibernate en el backend como con las librerías de manipulación de datos en Python.
## Metodologías de gestión
El ciclo de vida del modelo predictivo se gestionará mediante CRISP-DM, metodología estándar para proyectos de minería de datos estructurada en seis fases iterativas: comprensión del negocio, comprensión de los datos, preparación, modelado, evaluación y despliegue (IBM, 2021). La gestión del proyecto se realizará con Scrum, marco ágil basado en iteraciones cortas con roles, artefactos y ceremonias definidos (European Scrum Org., 2024).

# 3. Posicionamiento
## 3.1. Motivación
El mercado inmobiliario peruano opera con un nivel de transparencia informacional que restringe el descubrimiento eficiente de precios. El *Global Real Estate* *Transparency* *Index* *2024* sitúa al Perú en la categoría de mercado "Semi-Transparente" debido a las limitaciones en la infraestructura de datos públicos (JLL & LaSalle Investment Management, 2024), lo cual se traduce en una distorsión sistemática en el descubrimiento eficiente de precios de mercado. A diferencia de mercados consolidados donde existe un sistema centralizado de listados múltiples (MLS) que permite el intercambio de información transaccional bajo estándares unificados (National Association of Realtors, s. f.), el sector en Lima Metropolitana se encuentra fragmentado debido a la falta de un repositorio integrado de tasación y venta. Al respecto, Limachi (2023) enfatiza que en el país no se cuenta con un asiento cartográfico catastral único que permita identificar con precisión la ubicación física y el valor económico de las propiedades, lo cual perpetúa una desconexión entre los precios de oferta publicados y los precios reales de cierre de las transacciones, afectando la seguridad jurídica y dificultando el tráfico comercial directo. Esta problemática es respaldada cuantitativamente en el ámbito local por un estudio de campo en el distrito de Jesús María, que representa el segmento residencial de Lima Centro, donde se demuestra que el 73.86% de los usuarios percibe una falta recurrente de información importante en las plataformas inmobiliarias y un 47.97% experimenta dificultades significativas para acceder a datos relevantes del mercado (Mérida & Rodríguez, 2023).

Esta carencia estructural no es solo una percepción del mercado: se encuentra reconocida en el propio marco normativo peruano. El Reglamento Nacional de Tasaciones del Perú (aprobado por Resolución Ministerial N° 172-2016-VIVIENDA y modificado por Resolución Ministerial N° 124-2020-VIVIENDA, Ministerio de Vivienda, Construcción y Saneamiento) distingue entre la tasación reglamentaria, basada en valores arancelarios oficiales, y la tasación comercial, en la cual delega expresamente en el perito la recopilación de muestras de mercado a partir de fuentes informales como avisos publicitarios y transacciones referenciales. De manera similar, el Reglamento del Registro de Peritos Valuadores (aprobado por Resolución SBS N° 03225-2021, Superintendencia de Banca, Seguros y AFP) certifica la idoneidad profesional de quienes ejercen la tasación para el sistema financiero, pero no estandariza ni centraliza las fuentes de información que estos utilizan para sustentar sus valores. En conjunto, ambas normas evidencian que el Estado peruano regula quién puede tasar, mas no con qué datos se tasa, dejando en manos del propio profesional la tarea de construir su propio marco de referencia de mercado. Como consecuencia, los agentes se ven obligados a recurrir a múltiples herramientas e instituciones de consulta de forma aislada e independiente para intentar reconstruir la información comercial de un predio (Mérida & Rodríguez, 2023).

Ante esta carencia estructural, las agencias independientes se ven obligadas a recurrir a precios publicados en portales digitales. Sin embargo, como señala el Banco Central de Reserva del Perú (BCRP, 2026), estos datos, recolectados de plataformas comerciales como Urbania, son precios listados u ofertados, por lo que no reflejan necesariamente los valores reales a los que finalmente se ejecutan las transacciones de venta o alquiler en el mercado. Esta desconexión estructural introduce un alto margen de incertidumbre y subjetividad en el proceso de tasación. Dicha opacidad informacional se extiende de forma crítica al segmento de arrendamiento, en el cual el mercado carece de tasas de capitalización (Cap Rates) de referencia, historiales operativos y comparables estandarizados. Esta ausencia de benchmarks cuantitativos genera un cuello de botella financiero directo: la banca comercial local se niega a financiar proyectos o activos cuyos modelos no se pueden respaldar empíricamente con antecedentes de mercado. Como resultado, la incertidumbre frena la asignación de capital e incrementa los tiempos de estabilización y absorción de los proyectos hasta por tres años, un plazo considerablemente más extenso que en mercados maduros debido a la falta de referencias operativas objetivas. (Cisman, 2026).

Este problema se agrava por la brecha tecnológica entre las grandes corporaciones y las pequeñas empresas. En América Latina, las micro y pequeñas empresas representan la gran mayoría de los negocios, pero operan con niveles de digitalización muy bajos (OECD/CAF/SELA, 2024), un escenario de fragmentación digital que se replica en el segmento de agencias inmobiliarias independientes en el Perú, las cuales dependen principalmente de búsquedas manuales en portales web y del registro de información en hojas de cálculo descentralizadas.

A esta limitación tecnológica se suma la dependencia del juicio subjetivo del tasador. Tapia et al. (2025) documentan que la diversidad de criterios entre profesionales genera desviaciones significativas en las estimaciones de un mismo activo —una consecuencia directa, no una causa independiente, de la ausencia de un sistema de datos que ofrezca referencias objetivas de mercado—. El resultado es un proceso que castiga la precisión en favor de la intuición, con consecuencias directas sobre la duración del ciclo comercial y sobre la confianza del cliente. De hecho, la evidencia de campo confirma que el 33.74% de los usuarios identifica la falta de estadística de mercado como uno de los principales defectos de las plataformas existentes, mientras que un 48.78% señala la falta de información detallada como la principal traba para tomar decisiones confiables (Mérida & Rodríguez, 2023).

Existe, en consecuencia, una necesidad no atendida: dotar a las agencias inmobiliarias independientes de una herramienta analítica que unifique criterios de tasación, integre variables que hoy no se consideran y traduzca la predicción en indicadores de decisión comercial, sin exigir de ellas una infraestructura de datos o una capacidad técnica de la que carecen.

## 3.2. Planteamiento del problema
Para identificar de manera sistemática el origen y las consecuencias de las limitaciones de valoración en el mercado local, se realizó un diagnóstico causal estructurado mediante la técnica del árbol de problemas. Este análisis permite delimitar el problema técnico central del proyecto frente a las deficiencias del entorno y sus efectos comerciales, asegurando que la propuesta tecnológica responda a necesidades reales del sector. A continuación, se presenta la representación gráfica de esta estructura causal: 

Figura 2. Árbol de problemas de la valoración inmobiliaria y del análisis de rentabilidad. Nota: Adaptado de la sistematización causal del Trabajo de Investigación, integrando los sustentos institucionales de JLL (2024), BCRP (2026), Tapia et al. (2025), Limachi (2023), OECD/CAF/SELA (2024), el Ministerio de Vivienda, Construcción y Saneamiento (2020) y la Superintendencia de Banca, Seguros y AFP (2021). 

El análisis del árbol de problemas revela una relación de causa-efecto que se origina en la fragmentación de la infraestructura de datos urbanos y el rezago tecnológico de las agencias independientes. Las causas raíz (raíces del árbol) evidencian que la falta de un catastro integrado y la dependencia de precios de portales de oferta alimentan directamente el problema central de imprecisión y subjetividad en la tasación, un vacío que se encuentra reconocido incluso en el propio marco normativo del sector: tanto el Reglamento Nacional de Tasaciones (Ministerio de Vivienda, Construcción y Saneamiento, 2020) como el Reglamento del Registro de Peritos Valuadores (Superintendencia de Banca, Seguros y AFP, 2021) regulan quién puede tasar, pero no estandarizan con qué datos se tasa. Como consecuencia (ramas del árbol), este vacío técnico obliga incluso a profesionales certificados a sustentar sus evaluaciones financieras en estimaciones no estandarizadas, lo cual deriva en decisiones comerciales con alto margen de error y en pérdidas de rentabilidad y riesgos patrimoniales reales para sus clientes, no por falta de idoneidad del tasador, sino por la ausencia de un sistema de referencia objetivo que respalde su criterio. Finalmente, este desorden sistemático impacta al sector al manifestarse en una baja y semi transparencia informacional junto a severas limitaciones de datos en mercados emergentes, lo cual profundiza la insatisfacción del usuario por la falta de estadísticas, eleva los riesgos patrimoniales por estimaciones imprecisas y deriva en la paralización del capital de inversión y del financiamiento bancario.



| Problema | Causas |
| :--- | :--- |
| Baja precisión y alta subjetividad en la valoración de departamentos del segmento residencial, así como en el análisis de rentabilidad inmobiliaria para agencias independientes en Lima Metropolitana.  | **Baja transparencia y asimetría de información:**  El mercado limeño carece de transparencia de datos estructurada, lo que dificulta el acceso a registros transaccionales confiables (JLL, 2024).  |
|  | **Brecha entre precios de oferta y precios de cierre:**  Las tasaciones se basan en precios listados en portales que no reflejan los valores reales de las transacciones ejecutadas (BCRP, 2026).  |
|  | **Subjetividad por dependencia del juicio experto:**La falta de métodos cuantitativos estandarizados genera que la valoración dependa de la variabilidad del criterio humano (Tapia et al., 2025).  |
|  | **Ausencia de un catastro urbano unificado:** El país no cuenta con un asiento cartográfico único que integre con precisión la ubicación física y el valor económico de las propiedades (Limachi, 2023).  |
|  | **Infraestructura de datos fragmentada y brecha digital:** El segmento de agencias independientes opera con nula digitalización frente a las grandes corporaciones (OECD/CAF/SELA, 2024).  |
|  | **Falta de tasas de capitalización de referencia y comparables:** ****Evidencia cómo la carencia de Cap Rates referenciales frena el financiamiento bancario e incrementa la incertidumbre en los precios de alquiler e inversión (Cisman, 2026). |
|  | **Alta opacidad y desatención de datos en el mercado:** ****Respalda con estadísticas cuantitativas la alta percepción de opacidad y la demanda no atendida de datos e historial de precios por parte de los usuarios (Mérida & Rodríguez, 2023). |
|  | **Vacío normativo en la recopilación de datos transaccionales:** El Reglamento Nacional de Tasaciones (R.M. N° 172-2016-VIVIENDA, modificado por R.M. N° 124-2020-VIVIENDA) delega en el propio perito la recopilación de muestras de mercado a partir de fuentes informales, sin que el Estado provea un sistema oficial de datos de transacciones inmobiliarias (Ministerio de Vivienda, Construcción y Saneamiento, 2020). |
|  | **Certificación profesional sin estandarización de datos:** El Reglamento del Registro de Peritos Valuadores (Resolución SBS N° 03225-2021) certifica la idoneidad técnica del perito valuador, pero no estandariza ni centraliza las fuentes de información que este utiliza para sustentar sus tasaciones (Superintendencia de Banca, Seguros y AFP, 2021). |
## 3.3. Objetivos
### 3.3.1. Objetivo General
**OG:** Implementar un sistema web basado en algoritmos de Machine Learning que permita la valoración predictiva de departamentos residenciales y el análisis de indicadores de rentabilidad (Cap Rate) para agencias inmobiliarias independientes en Lima Metropolitana.
### 3.3.2. Objetivos Específicos
**OE1:** Analizar los modelos de aprendizaje automático y las variables del entorno urbano de Lima Metropolitana, determinando el conjunto de características óptimas para el entrenamiento de los algoritmos predictivos. 

**OE2:** Diseñar la arquitectura del sistema web, el esquema de la base de datos y el modelo predictivo, definiendo la estructura de procesamiento de información y los componentes de la interfaz de usuario. 

**OE3:** Desarrollar el sistema web integrando el algoritmo predictivo XGBoost, el módulo de cálculo financiero de rentabilidad (Cap Rate) y la interfaz gráfica para la visualización y justificación de resultados. 

**OE4:** Validar el error estadístico del modelo predictivo y la usabilidad de la plataforma web con agentes inmobiliarios independientes, evaluando el nivel de precisión alcanzado frente al método tradicional. 
## 3.4. Indicadores de Éxito


| Objetivo Específico | Indicador de Éxito  |
| :--- | :--- |
| **OE1 — I1** | Aprobación del informe de análisis de modelos y selección de variables, que especifique la estructura del dataset consolidado para el periodo 2016–2025.  |
| **OE2 — I1** | Aprobación del documento de arquitectura del sistema por la asesora, que incluya los diagramas lógicos, físicos, el modelo entidad-relación y el pipeline de Machine Learning.  |
| **OE3 — I1** | Aprobación del acta de conformidad de la implementación del sistema web, que valide la integración de la interfaz, el backend y el servicio de Machine Learning.  |
| **OE4 — I1** | Aprobación del informe de validación del modelo XGBoost, con MAPE ≤ 15% en el conjunto de prueba (70/30), umbral alineado al estándar IAAO (COD ≤ 15%) y contrastado como referencia local frente al 17.89% de Oporto D'Ugard et al. (2025) en Lima Metropolitana. |
| **OE4 — I2** | Aprobación del informe de usabilidad y del acta de conformidad aplicados a agentes inmobiliarios independientes y al equipo comercial del caso de estudio con MAK Inmobiliaria, alcanzando un nivel de satisfacción percibida SUS ≥ 75. |
## 3.5. Alcance del Proyecto
### Alcance funcional
1. Registro y gestión de información de inmuebles residenciales para valoración.

2. Integración y procesamiento de variables predictoras del mercado inmobiliario.

3. Predicción del valor comercial del inmueble mediante modelo XGBoost.

4.      Predicción del valor de alquiler mensual mediante un segundo modelo XGBoost.

5. Visualización de resultados de valoración con soporte explicativo de variables relevantes mediante SHAP.

6. Cálculo de indicadores de rentabilidad Cap Rate.

7. Generación de reportes de valoración y rentabilidad para uso comercial.

8. Pruebas funcionales y de usabilidad con usuarios del caso de estudio (MAK Inmobiliaria).

9. Monitoreo del desempeño del modelo predictivo y gestión del proceso de actualización mediante reentrenamiento periódico. 
### Alcance técnico
1. Desarrollo del frontend web para la interacción de los usuarios.

2. Desarrollo del backend con servicios API para la lógica de negocio y la predicción.

3. Diseño de la base de datos para la persistencia de inmuebles, valoraciones y resultados.

4. Pipeline de preprocesamiento, entrenamiento, evaluación y consumo del modelo de Machine Learning.

5. Versionado de componentes y despliegue en entorno de pruebas.
### Fuera del alcance
1. Tasación con validez legal, catastral o pericial. El sistema produce estimaciones de referencia comercial y no sustituye a un informe de valorización emitido por un perito acreditado.

2. Casas unifamiliares, quintas, casas de campo/playa, terrenos sin edificar, así como inmuebles comerciales, industriales y rurales. El sistema predictivo se circunscribe exclusivamente a departamentos del segmento residencial urbano.

3. Ámbitos geográficos distintos de Lima Metropolitana.

4. Integración con los sistemas heredados o el CRM propietario de MAK Inmobiliaria. La solución se plantea como plataforma independiente e interoperable.

5. Intermediación de transacciones, gestión de pagos o firma electrónica de contratos.

6. Despliegue en un entorno productivo con acuerdos de nivel de servicio. La entrega se realiza sobre un entorno de pruebas.

7. Análisis basado en imágenes del inmueble o en modelos tridimensionales.

8. Modelamiento financiero avanzado de rentabilidad. El cálculo del Cap Rate se realiza bajo el supuesto de ausencia de apalancamiento financiero (unlevered), utilizando un ratio de gastos operativos (OER) estándar y parametrizable. No se incorpora financiamiento hipotecario, tasa de vacancia real por inmueble, apreciación de capital a largo plazo, ni datos granulares de gasto operativo real por predio. No se implementa un indicador de ROI separado, dado que bajo estos supuestos converge matemáticamente con el Cap Rate.
## 3.6. Suposiciones y Restricciones
### Suposiciones


| Suposiciones | Descripción |
| :--- | :--- |
| Vigencia del dataset obtenido | Se asume que el conjunto de datos del BCRP correspondiente al periodo 2016–2025, ya obtenido por el equipo, mantiene una estructura estable y resulta suficiente para el entrenamiento y la evaluación de los modelos predictivos. |
| Disponibilidad de fuentes complementarias | Se asume que las fuentes de enriquecimiento (INEI, indicadores de seguridad ciudadana, OpenStreetMap y Google Maps API) permanecerán accesibles durante la ejecución del proyecto. |
| Participación de MAK Inmobiliaria | Se asume que MAK Inmobiliaria facilitará el acceso a usuarios para la realización de entrevistas, validaciones y pruebas de usabilidad. |
| Disponibilidad del equipo | Se asume que los integrantes del proyecto dedicarán el tiempo planificado para cumplir con los sprints y entregables definidos. |
| Estabilidad tecnológica | Se asume que las tecnologías seleccionadas (Angular, Spring Boot, PostgreSQL, XGBoost, Docker y MLflow) serán compatibles entre sí y no presentarán limitaciones que impidan el desarrollo del sistema. |
| Continuidad del acompañamiento académico | Se asume que la asesoría especializada y metodológica se mantendrá disponible durante todo el ciclo 2026-2. |
### Restricciones


| Restricciones | Descripción |
| :--- | :--- |
| Tiempo del proyecto | El desarrollo deberá completarse dentro del ciclo académico 2026-2 establecido por la UPC, comprendido entre el 24 de agosto y el 12 de diciembre de 2026. |
| Recursos económicos | El proyecto utilizará únicamente herramientas gratuitas, académicas o de código abierto, sin adquirir licencias comerciales que excedan el presupuesto asignado por el equipo. |
| Cobertura y naturaleza del dataset | El modelo será entrenado y validado a partir de los registros históricos transaccionales del BCRP (2016–2025), los cuales recopilan operaciones de compraventa correspondientes al mercado de departamentos residenciales en Lima Metropolitana. |
| Segmento de mercado | El alcance del modelo predictivo se limita exclusivamente a departamentos del segmento residencial urbano en Lima Metropolitana. Se excluyen expresamente casas unifamiliares, quintas, terrenos e inmuebles comerciales o rurales debido a la naturaleza de la base transaccional de referencia del BCRP. |
| Dependencia de la calidad de datos | La precisión del modelo dependerá de la calidad, granularidad y actualización de la información disponible en el dataset utilizado. |
| Protección de datos personales | El tratamiento de cualquier información proveniente de MAK Inmobiliaria deberá ajustarse a la Ley N.° 29733, Ley de Protección de Datos Personales (Congreso de la República, 2011). |
| Términos de uso de fuentes externas | El uso de datos se limitará estrictamente a fuentes públicas oficiales y de código abierto (BCRP, INEI, OpenStreetMap), respetando sus licencias de datos abiertos y términos de consulta de las API empleadas, evitando el uso de técnicas de extracción automatizada en plataformas comerciales privadas. |
## 3.7. Contribución e Impacto del Proyecto de Investigación
### Contribución tecnológica
     El proyecto aporta un sistema de valoración automatizada adaptado a un mercado con baja transparencia informacional, que combina tres elementos que en la literatura revisada aparecen habitualmente por separado: un modelo predictivo basado en técnicas de boosting de árboles de decisión (XGBoost), una capa de explicabilidad orientada al usuario no técnico, y un módulo financiero que traduce la predicción en indicadores de decisión comercial. Los trabajos precedentes en el contexto peruano se han concentrado en la valorización con fines bancarios o en la predicción económica agregada, sin abordar la operación comercial de la agencia independiente. Para efectos de este proyecto, se entrenan dos modelos XGBoost independientes entre sí —uno sobre el mercado de venta y otro sobre el mercado de alquiler—, cuyas salidas se combinan externamente mediante un módulo de cálculo financiero para estimar el Ingreso Operativo Neto (NOI), aplicando un ratio de gastos operativos (OER) estándar y parametrizable, dado que no se dispone de datos granulares de gasto real por predio ni de registros pareados de venta-alquiler para un mismo inmueble. El Cap Rate se calcula como el cociente entre el Ingreso Operativo Neto (NOI) anual estimado y el valor comercial predicho del inmueble, bajo el supuesto de ausencia de apalancamiento financiero (unlevered). Se declara como limitación explícita que el indicador no incorpora financiamiento hipotecario, tasa de vacancia real ni apreciación de capital a largo plazo.
### Beneficios cuantitativos esperados


| Indicador | Línea base | Meta |
| :--- | :--- | :--- |
| MAPE del modelo de valoración sobre el conjunto de prueba | Sin línea base local estrictamente comparable; referencia contextual: 17.89% (Oporto D'Ugard et al., 2025) | ≤ 15 (alineado al estándar IAAO, COD ≤ 15%) |
| Índice de usabilidad SUS de la interfaz web | No disponible | ≥ 75 |
| Tiempo total de ciclo para la obtención de una propuesta de valor comercial y análisis de rentabilidad.  | 180 minutos (3 horas) por informe, basado en el flujo manual que involucra la búsqueda en múltiples portales, tabulación de comparables en Excel y cálculo financiero empírico.  | ≤ 5 minutos para el flujo completo en la plataforma web (desde el ingreso de atributos del inmueble, visualización explicativa con SHAP, hasta la descarga del reporte financiero en PDF). |
| Cobertura temporal del dataset de entrenamiento | Sin base histórica consolidada en la agencia | Periodo 2016–2025 |
### Beneficios cualitativos
Para el asesor inmobiliario, la posibilidad de sustentar una propuesta de valor con evidencia cuantitativa y con la contribución explícita de cada variable, en lugar de apoyarse únicamente en su experiencia. Para el gerente comercial, la trazabilidad y el control de calidad sobre las estimaciones emitidas por su equipo. Para el propietario, una referencia de precio verificable que reduce la fricción en la negociación.
### Alineamiento con los Objetivos de Desarrollo Sostenible
El proyecto se alinea directamente con el ODS 9 (Industria, Innovación e Infraestructura), específicamente con la meta de aumentar la investigación científica y mejorar la capacidad tecnológica de los sectores industriales. El sector inmobiliario de Lima Metropolitana opera bajo un marcado déficit de infraestructura de datos debido a la fragmentación de la información catastral y comercial. En este contexto, el proyecto contribuye de forma directa a mitigar esta carencia mediante el diseño e implementación de una solución tecnológica innovadora: un sistema web modular basado en Machine Learning que centraliza, procesa y expone analíticamente variables físicas, macroeconómicas y geoespaciales para la toma de decisiones. Así, la plataforma dota a las micro y pequeñas agencias independientes de una capacidad analítica avanzada, promoviendo la adopción de tecnología y la innovación en un sector tradicionalmente rezagado.

# 4. Organización del Proyecto
## 4.1. Responsable del Proyecto


| Responsable | Roles | Responsabilidades |
| :--- | :--- | :--- |
| Cortez Quezada, Joaquín Antonio | Jefe de Proyecto / Development Team | Planificación y seguimiento del proyecto, gestión de entregables, coordinación con la asesora, desarrollo del backend y del pipeline de Machine Learning. |
| Pingus Rodríguez, Carlos Daniel | Scrum Master / Development Team | Facilitación de las ceremonias Scrum, eliminación de impedimentos, aseguramiento del cumplimiento del proceso ágil, desarrollo del frontend y de los módulos de explicabilidad y rentabilidad. |
| Félix Corrales, Rosa Andrea | Asesora Especializada | Orientación técnica sobre la solución propuesta, revisión de la arquitectura y del modelo predictivo, y validación de los entregables técnicos del proyecto. |
| Herrera Trujillo, Emilio Antonio | Asesor Metodológico | Guía en la aplicación de la metodología de investigación y evaluación de los avances expuestos en las semanas 8 y 16. |
| Representante de MAK Inmobiliaria | Product Owner | Priorización del Product Backlog, validación de las historias de usuario y aceptación funcional de los incrementos entregados en cada sprint. |
## 4.2. Stakeholders, Beneficiarios o Usuarios
### Stakeholders


| Stakeholders | Necesidades | Entregables |
| :--- | :--- | :--- |
| Asesora especializada (Rosa Andrea Félix Corrales) | Solidez técnica de la solución, coherencia entre la arquitectura propuesta y el problema planteado | Project Charter, documento de arquitectura, informe de validación del modelo, Memoria de Tesis |
| Asesor metodológico | Rigor académico, coherencia metodológica y cumplimiento de objetivos | Avances expuestos en las semanas 8 y 16, Memoria de Tesis |
| Comité evaluador UPC | Evidencia técnica y científica del proyecto | Sustentaciones, prototipo funcional, resultados de validación |
| MAK Inmobiliaria (dirección y gerencia) | Mejorar la precisión de las tasaciones y disponer de soporte cuantitativo para la decisión comercial | Sistema web funcional, reporte de resultados, acta de conformidad |
| Grupo de investigación Ventage | Producción de un resultado de investigación publicable y replicable | Artículo de investigación, dataset documentado, código fuente versionado |
| Equipo del proyecto (Jefe de Proyecto y Scrum Master) | Requisitos claros, arquitectura estable y control del alcance | Product Backlog, cronograma, reportes de avance por sprint |
### Beneficiarios


| Beneficiarios | Necesidades | Entregables |
| :--- | :--- | :--- |
| Agencias inmobiliarias independientes de Lima Metropolitana | Acceso a herramientas analíticas equiparables a las de las grandes corporaciones | Plataforma web replicable, procedimiento metodológico documentado |
| Propietarios que solicitan una valoración | Referencia de precio verificable y comprensible para negociar en condiciones informadas | Reporte de valoración con explicación de variables |
| Comunidad académica y profesional del sector | Evidencia empírica sobre el desempeño de modelos de ensamble en un mercado con baja transparencia | Artículo de investigación y resultados de validación |
### Usuarios


| Usuarios | Necesidades | Entregables |
| :--- | :--- | :--- |
| Agente Inmobiliario Independiente  | Tasación rápida, consistente y explicable, con argumentos presentables al cliente  | Módulo de valoración, explicación SHAP, reporte en PDF  |
| Agencia Inmobiliaria MYPE  | Tasación rápida, consistente y explicable, con argumentos presentables al cliente; gestión de su cartera de inmuebles publicados  | Módulo de valoración, explicación SHAP, reporte en PDF, gestión de inmuebles  |
| Administrador del sistema | Supervisión del estado del modelo y control del proceso de reentrenamiento  | Panel de monitoreo, funciones de aprobación y reversión de reentrenamiento  |
## 4.3. Recursos requeridos
### Recursos de hardware
1. Dos estaciones de trabajo portátiles con procesador de gama media o superior, 16 GB de memoria RAM y almacenamiento en estado sólido, destinadas al desarrollo y a las pruebas locales.

2. Conectividad estable a internet para el acceso a servicios en la nube, repositorios y fuentes de datos externas.

3. Entorno de cómputo acelerado en la nube para el entrenamiento de los modelos, con recurso a los laboratorios de la universidad como alternativa si la capacidad instalada resulta insuficiente.
### Recursos de software base
1. **Frontend:** Angular y su cadena de herramientas asociada.

2. **Backend:** Java con Spring Boot; entorno de desarrollo integrado IntelliJ IDEA.

3. **Base de datos:** PostgreSQL.

4. **Machine Learning:** Python, XGBoost, scikit-learn, pandas y la librería SHAP.

5. **Operación de modelos:** MLflow para el versionado y seguimiento de experimentos; Docker para la contenedorización.

6. **Servicios externos:** Google Maps API para la geocodificación; OpenStreetMap para el cálculo de distancias.

7. **Control de versiones y colaboración:** Git y GitHub.

8. **Diseño de interfaz:** herramienta de prototipado para wireframes y mockups.

9. **Gestión ágil:** tablero Scrum para el seguimiento del Product Backlog y de los sprints.
### Recursos materiales y de gestión
1. Plantillas institucionales UPC para la documentación y la sustentación.

2. Software de gestión bibliográfica y de verificación de similitud.

3. Espacios de reunión para las sesiones de trabajo con MAK Inmobiliaria y para las pruebas de usabilidad.
## 4.4. Fases e hitos del proyecto
El proyecto se desarrolla durante el ciclo académico 2026-2, comprendido entre el 24 de agosto y el 12 de diciembre de 2026. Las semanas 8 y 16 no contemplan sesión de clase, ya que están destinadas a la exposición de avances ante el asesor metodológico.

El trabajo se organiza en cuatro fases. La fase de **Inicio** consolida el sustento del problema y el Project Charter. La fase de **Planificación** define el Product Backlog y el plan de gestión. La fase de **Ejecución** desarrolla el sistema mediante sprints iterativos bajo Scrum, precedidos de un Sprint 0 de preparación técnica. La fase de **Cierre** ejecuta las pruebas finales, consolida la Memoria de Tesis y realiza la sustentación.

| Hito del proyecto | Fecha Estimada | Entregables incluidos | Prioridad |
| :--- | :--- | :--- | :--- |
| **Hito 1: Inicio del proyecto** (Semana 2) | 02/09/2026 | • Project Charter v1.0• Alcance, suposiciones y restricciones• Identificación de stakeholders• Memoria de Tesis: avance de los puntos 1 y 2 | Alta |
| **Hito 2: Cierre del Sprint 0** (Semana 5) | 26/09/2026 | • Arquitectura lógica, física e integrada validada• Modelo entidad-relación de la base de datos• Ambientes de desarrollo y pruebas configurados• Dataset BCRP 2016–2025 preprocesado• Wireframes y mockups de la interfaz• Product Backlog priorizado con sprints definidos | Alta |
| **Hito 3: Exposición de avance parcial ante el asesor metodológico** (Semana 8) | 17/10/2026 | • Cierre del Sprint 1• Pipeline de datos implementado• Modelos de venta y alquiler entrenados y evaluados (MAE, RMSE, MAPE)• Módulo de gestión de usuarios y autenticación• Memoria de Tesis: avance hasta el punto 3 | Alta |
| **Hito 4: Cierre del Sprint 2** (Semana 12) | 14/11/2026 | • Módulo de gestión de inmuebles (CRUD)• Funcionalidad de predicción de valor operativa• Explicabilidad del modelo mediante SHAP• Prototipo funcional del frontend Angular integrado con el backend Spring Boot | Alta |
| **Hito 5: Exposición final ante el asesor metodológico** (Semana 16) | 12/12/2026 | • Cierre del Sprint 3• Módulo de análisis de rentabilidad (Cap Rate)• Generación de reportes en PDF• Enriquecimiento geoespacial del dataset• Actualización de datos mediante la consolidación de nuevos registros históricos del BCRP y reentrenamiento del modelo • Resultados de las pruebas de usabilidad (SUS)• Acta de conformidad de MAK Inmobiliaria• Memoria de Tesis consolidada y artículo de investigación | Alta |
### Organización de los sprints
- **Sprint 0 — Preparación técnica.** Definición de la arquitectura del sistema, creación de la base de datos y configuración de los ambientes de desarrollo y pruebas.

- **Sprint 1 — Datos y modelos.** Desarrollo del pipeline de datos y entrenamiento de los modelos predictivos de venta y alquiler con XGBoost, junto con el módulo de gestión de usuarios y autenticación.

- **Sprint 2 — Valoración y** **explicabilidad.** Desarrollo del módulo de gestión de inmuebles y de las funcionalidades de predicción de valor y explicabilidad mediante SHAP.

- **Sprint 3 — Rentabilidad y actualización.** Desarrollo del módulo de análisis de rentabilidad, generación de reportes, enriquecimiento geoespacial del dataset a partir de fuentes de datos abiertos y actualización del modelo predictivo mediante la ingesta de nuevos consolidados históricos del BCRP y el reentrenamiento supervisado.
## 4.5. Enfoques del trabajo
### Gestión del proyecto: Scrum
Se empleará el marco de trabajo ágil Scrum para la gestión de tareas, el control de avances y las entregas iterativas mediante sprints, asegurando la adaptación continua a los requerimientos del caso de estudio y la entrega progresiva de valor a los stakeholders. Se aplicarán las ceremonias de Sprint Planning, Daily Scrum, Sprint Review y Retrospectiva, y se mantendrán como artefactos el Product Backlog, el Sprint Backlog y el incremento (European Scrum Org., 2024).
### Ciclo de vida del modelo predictivo: CRISP-DM
Para el desarrollo del modelo de Machine Learning se aplicará la metodología Cross-Industry Standard Process for Data Mining, que guiará las seis fases del proceso (IBM, 2021):

1. **Comprensión del negocio:** caracterización del proceso de tasación comercial en agencias independientes de Lima.

2. **Comprensión de los datos:** exploración del dataset del BCRP correspondiente al periodo 2016–2025 y de las fuentes complementarias de enriquecimiento.

3. **Preparación de los datos:** limpieza, tratamiento de valores atípicos, normalización e ingeniería de características.

4. **Modelado:** entrenamiento del algoritmo XGBoost para los modelos de venta y de alquiler, con ajuste de hiperparámetros.

5. **Evaluación:** cálculo de MAE, RMSE, MAPE y R² sobre el conjunto de prueba, con validación cruzada y comparación frente al valor de referencia local.

6. **Despliegue:** publicación del modelo como servicio predictivo consumible por el backend.
### Desarrollo y operación: MLOps
Para garantizar la integración entre el frontend, el backend y el modelo de Machine Learning se adoptarán prácticas de MLOps mediante MLflow para el versionado y el seguimiento de experimentos, Docker para la contenedorización del sistema y control de versiones del pipeline de datos. Asimismo, se implementará el monitoreo del desempeño del modelo para detectar degradación de la precisión ante cambios en la dinámica del mercado inmobiliario limeño (Rodrigues et al., 2025).
### Técnicas de investigación
Revisión sistemática de literatura sobre modelos de valoración automatizada, entrevistas semiestructuradas con asesores de MAK Inmobiliaria para el levantamiento del proceso actual, modelado del proceso mediante notación BPMN, análisis causal con árbol de problemas y diagrama de Ishikawa, y evaluación de usabilidad mediante el cuestionario System Usability Scale.
## 4.6. Riesgos y Mitigación


| # | Riesgo | Probabilidad | Impacto | Estrategia de mitigación |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Retraso en las actividades del cronograma debido a la necesidad de realizar iteraciones adicionales de entrenamiento si el modelo de Machine Learning no alcanza la precisión esperada (MAPE objetivo). | Media | Alto | **Mitigar.** Realizar ingeniería de características, comparar algoritmos adicionales y ajustar hiperparámetros mediante validación cruzada. Reservar holgura en el Sprint 1 para una segunda iteración de entrenamiento. |
| 2 | Insuficiencia de la granularidad o de la cobertura del dataset del BCRP 2016–2025 para el nivel de detalle que requiere la valoración por inmueble individual. | Media | Alto | **Mitigar.** Delimitar el alcance exclusivamente a departamentos residenciales para reducir el sesgo por heterogeneidad. Asimismo, enriquecer el dataset mediante la integración de fuentes abiertas oficiales (INEI, seguridad ciudadana, OpenStreetMap) y datos históricos de referencia, documentando explícitamente la unidad de análisis en el informe de selección del dataset. |
| 3 | Incremento del esfuerzo (mayor inversión de horas-hombre) por la necesidad de reprogramar sustancialmente la interfaz web y los gráficos de resultados (SHAP y rentabilidad) si los agentes independientes consideran que la plataforma es compleja o poco intuitiva. | Media | Medio | **Evitar.** Diseñar wireframes y prototipos interactivos desde el Sprint 0 y validarlos visualmente con asesores del sector antes de escribir el código del frontend. |
| 4 | Retraso en las actividades del cronograma debido a problemas de compatibilidad e integración entre el frontend, el backend y la API del modelo de Machine Learning. | Baja | Alto | **Mitigar.** Implementar integración continua, realizar pruebas de integración periódicas desde el Sprint 1 y documentar los contratos de las API. |
| 5 | Retraso en las actividades del cronograma por la indisponibilidad de los agentes de MAK Inmobiliaria para participar en las sesiones de pruebas de usabilidad en las fechas acordadas. | Baja | Medio | **Mitigar.** Coordinar las sesiones con anticipación y contar con usuarios alternativos (agentes independientes externos) para la validación. |
| 6 | Indisponibilidad temporal o cambios en la estructura de descarga de las bases de datos públicas oficiales (BCRP o INEI). | Media | Medio | **Mitigar.** Mantener repositorios locales respaldados y versionados de las series históricas descargadas (2016–2025) y utilizar conectores estándar para la lectura de capas geoespaciales desde OpenStreetMap. |
| 7 | Degradación del desempeño del modelo tras el reentrenamiento con datos actualizados. | Baja | Medio | **Mitigar**. Establecer un umbral de aceptación que impida promover como modelo activo en el entorno de pruebas a cualquier modelo candidato cuyas métricas de rendimiento (MAPE, RMSE, MAE) empeoren respecto del modelo actualmente desplegado en dicho entorno.  |
# 5. Glosario


| Término | Descripción |
| :--- | :--- |
| **Valoración inmobiliaria** | Proceso de estimación del valor de mercado de un bien inmueble a partir de sus características físicas, su localización y las condiciones económicas vigentes. |
| **Tasación comercial** | Estimación del precio de salida al mercado de un inmueble con fines de intermediación, distinta de la tasación pericial con validez legal. |
| **Modelo de Valoración Automatizada** | Sistema que estima el valor de un inmueble mediante procedimientos estadísticos o algorítmicos aplicados sobre bases de datos de transacciones. |
| **Machine** **Learning** | Rama de la inteligencia artificial que permite a los sistemas aprender patrones a partir de datos sin ser programados explícitamente para cada caso. |
| **Aprendizaje supervisado** | Modalidad de aprendizaje automático en la que el modelo se entrena con ejemplos que incluyen la respuesta correcta. |
| **XGBoost** | Implementación optimizada del algoritmo de gradient boosting que construye árboles de decisión de forma secuencial, minimizando en cada iteración los errores residuales del árbol precedente. |
| **SHAP** | Método de explicabilidad que cuantifica la contribución de cada variable a una predicción individual. |
| **Cap** **Rate** | Tasa de capitalización; relación entre el ingreso operativo neto anual y el valor del activo inmobiliario. |
| **Ingreso Operativo Neto (NOI)** | Ingreso generado por un inmueble en alquiler después de descontar los gastos operativos estimados, sin considerar financiamiento hipotecario ni impuestos sobre la renta. |
| **Ratio de Gastos Operativos (OER)** | Porcentaje estándar y parametrizable del ingreso bruto de alquiler que se descuenta para estimar los gastos operativos de un inmueble, ante la ausencia de datos granulares de gasto real por predio. |
| **Unlevered** **(sin apalancamiento)** | Supuesto financiero bajo el cual un indicador de rentabilidad se calcula asumiendo que la propiedad se adquiere al contado, sin financiamiento hipotecario. |
| **Modelo hedónico** | Modelo econométrico que descompone el precio de un bien en el valor implícito de cada uno de sus atributos. |
| **Variable predictora** | Atributo empleado como entrada del modelo para estimar la variable objetivo. |
| **MAPE** | Error porcentual absoluto medio; métrica que expresa el error de predicción como porcentaje del valor real. |
| **Sobreajuste** | Fenómeno en el que el modelo aprende en exceso los datos de entrenamiento y pierde capacidad de generalizar a datos nuevos. |
| **Validación cruzada** | Técnica que divide el conjunto de datos en particiones sucesivas de entrenamiento y prueba para estimar el desempeño del modelo de forma robusta. |
| **Dataset** | Conjunto estructurado de datos utilizado para entrenar y evaluar modelos. |
| **Pipeline de datos** | Secuencia automatizada de pasos de extracción, limpieza, transformación y carga de información. |
| **CRISP-DM** | Metodología para proyectos de minería de datos y aprendizaje automático estructurada en seis fases iterativas. |
| **Scrum** | Marco ágil de gestión de proyectos basado en iteraciones cortas denominadas sprints. |
| **Sprint** | Iteración de duración fija dentro de Scrum al término de la cual se entrega un incremento de producto. |
| **Product** **Backlog** | Lista priorizada de funcionalidades y requisitos pendientes del producto. |
| **MLOps** | Conjunto de prácticas para operacionalizar modelos de aprendizaje automático en producción, con monitoreo y mantenimiento continuos. |
| **API REST** | Interfaz de servicios web que permite la comunicación estandarizada entre sistemas. |
| **Frontend** | Capa de interfaz visual con la que interactúa el usuario. |
| **Backend** | Capa lógica que procesa las reglas de negocio, los datos y los servicios del sistema. |
| **Caso de estudio** | Contexto organizacional real empleado para validar la solución propuesta. |
| **System** **Usability** **Scale** | Cuestionario estandarizado de diez ítems que produce un índice de usabilidad percibida en una escala de 0 a 100. |
# 6. Siglario


| Sigla | Descripción |
| :--- | :--- |
| **API** | Application Programming Interface (Interfaz de Programación de Aplicaciones) |
| **AVM** | Automated Valuation Model (Modelo de Valoración Automatizada) |
| **BCRP** | Banco Central de Reserva del Perú |
| **BPMN** | Business Process Model and Notation |
| **CRISP-DM** | Cross-Industry Standard Process for Data Mining |
| **CRUD** | Create, Read, Update, Delete |
| **IA** | Inteligencia Artificial |
| **IAAO** | International Association of Assessing Officers |
| **INEI** | Instituto Nacional de Estadística e Informática |
| **ISO** | International Organization for Standardization |
| **IVSC** | International Valuation Standards Council |
| **JLL** | Jones Lang LaSalle |
| **MAE** | Mean Absolute Error (Error Absoluto Medio) |
| **MAPE** | Mean Absolute Percentage Error (Error Porcentual Absoluto Medio) |
| **ML** | Machine Learning |
| **MLOps** | Machine Learning Operations |
| **MLS** | Multiple Listing Service |
| **MIPYME****NOI** | Micro, Pequeña y Mediana EmpresaNet Operating Income (Ingreso Operativo Neto) |
| **NSE** | Nivel Socioeconómico |
| **ODS****OER** | Objetivos de Desarrollo SostenibleOperating Expense Ratio (Ratio de Gastos Operativos) |
| **OSM** | OpenStreetMap |
| **PDF** | Portable Document Format |
| **RMSE** | Root Mean Square Error (Raíz del Error Cuadrático Medio) |
| **R²** | Coeficiente de Determinación |
| **SHAP** | SHapley Additive exPlanations |
| **SUS** | System Usability Scale |
| **TH** | Historia Técnica |
| **UPC** | Universidad Peruana de Ciencias Aplicadas |
| **US** | Historia de Usuario |
# 7. Bibliografía
Acharya, V., Saketh, V. N., Zaki, A., Gowda, M. M., Jain, N., & Honnavalli, P. B. (2026). A serverless automated MLOps framework for scalable industrial predictive maintenance. *Discover Internet of Things, 6*(1). https://doi.org/10.1007/s43926-026-00291-0

Ali, Z. A., Abduljabbar, Z. H., Tahir, H. A., Sallow, A. B., & Almufti, S. M. (2023). eXtreme gradient boosting algorithm with machine learning: A review. *Academic Journal of Nawroz University, 12*(2), 320–334. https://doi.org/10.25007/ajnu.v12n2a1612

Çılgın, C., & Gökçen, H. (2025). *Hybrid model architecture for real estate market value prediction.*

Cisman, H. (2026). La tesis de multifamily comienza a nacer en Lima. GRI Hub Noticias. https://www.gribclub.com/

Congreso de la República. (2007). *Ley* *N.°* *29080. Ley de Creación del Registro del Agente Inmobiliario del Ministerio de Vivienda, Construcción y Saneamiento.* https://www.leyes.congreso.gob.pe/Documentos/Leyes/29080.pdf

Congreso de la República. (2011). *Ley* *N.°* *29733. Ley de Protección de Datos Personales.* https://www.gob.pe/institucion/congreso-de-la-republica/normas-legales/243470-29733

Congreso de la República. (2023). *Ley* *N.°* *31814. Ley que promueve el uso de la inteligencia artificial en favor del desarrollo económico y social del país.* https://www.gob.pe/institucion/congreso-de-la-republica/normas-legales/4565760-31814

Deng, L., & Zhang, X. (2026). Effects of housing price determinants in major international financial centers. *Cities, 175.* https://doi.org/10.1016/j.cities.2026.107142

El Jaouhari, A., Samadhiya, A., Kumar, A., Šešplaukis, A., & Raslanas, S. (2024). Mapping the landscape: A systematic literature review on automated valuation models and strategic applications in real estate. *International Journal of Strategic Property Management, 28*(5), 286–301. https://doi.org/10.3846/ijspm.2024.22251

European Scrum Org. (2024). *Guía metodologías Agile 2024* (v1.0). European Scrum Org.

Genc, N., & Colak, H. E. (2026). Identifying influencers of housing prices: spatial heterogeneity and pattern analysis using machine learning, spatial regression, and GIS. *Journal of Housing and the Built Environment.* https://doi.org/10.1007/s10901-025-10244-8

Hassoon, A., Ghazali, F. E. M., & Khaleel, T. A. (2026). Application of XGBoost and artificial neural networks in predicting housing project productivity. *Discover Artificial Intelligence.* https://doi.org/10.1007/s44163-025-00658-2

Hoxha, V., & Hoxha, D. (2026). Predicting apartment prices in Prishtina: insights from gradient boosting modeling. *Journal of Financial Management of Property and Construction, 31*(1), 49–63. https://doi.org/10.1108/JFMPC-03-2024-0017

IBM. (2021). *Conceptos básicos de ayuda de CRISP-DM.* IBM SPSS Modeler SaaS. https://www.ibm.com/docs/es/spss-modeler/saas?topic=dm-crisp-help-overview

Instituto Nacional de Estadística e Informática. (2024). *Perú: XII. Déficit habitacional.* https://www.inei.gob.pe/media/MenuRecursivo/publicaciones_digitales/Est/Lib2027/cap12.pdf

International Valuation Standards Council. (2025). *International Valuation Standards effective 31 January 2025.* IVSC.

JLL & LaSalle Investment Management. (2024). *Global Real Estate Transparency Index 2024.* https://www.jll.com/en-us/insights/global-real-estate-transparency-index

Kee, T., & Ho, W. K. O. (2025). eXplainable machine learning for real estate: XGBoost and Shapley values in price prediction. *Civil Engineering Journal, 11*(5), 2116–2133. https://doi.org/10.28991/CEJ-2025-011-05-022

Kucklick, J. P., & Müller, O. (2023). Tackling the accuracy-interpretability trade-off: Interpretable deep learning models for satellite image-based real estate appraisal. *ACM* *Transactions* *on* *Management* *Information* *Systems.*

Limachi, M. (2023). Creación institucional del catastro urbano y rural para conocer la realidad física de inmuebles o predios en el territorio peruano. https://repositorio.unjfsc.edu.pe/handle/20.500.14067/7696  **

Liu, X., Qiao, R., Wu, Z., Yang, T., Zhang, X., Zhang, X., & Zhu, Z. (2024). Unveiling the spatially varied nonlinear effects of urban built environment on housing prices using an interpretable ensemble learning model. *Applied Geography, 173.* https://doi.org/10.1016/J.APGEOG.2024.103458

Maia, W. F., & David, S. A. (2024). Machine learning applied to estate pricing for residential rentals in dynamic urban markets — The case of São Paulo city. *Engineering Analysis with Boundary Elements, 169.* https://doi.org/10.1016/J.ENGANABOUND.2024.105988

Manganelli, B., Anelli, D., Tajani, F., & Morano, P. (2024). Capitalization rate and real estate risk factors: An analysis of the relationships for the residential market in the city of Rome (Italy). *Real Estate Management and Valuation, 32*, 101–115. https://doi.org/10.2478/remav-2024-0028

Merida, J., Rodriguez, C. (2023). El efecto de la transparencia de la información en la experiencia del cliente para la adquisición de inmuebles en el distrito de Jesús María, 2023 [Tesis de maestría, Universidad del Pacífico]. Repositorio Institucional de la Universidad del Pacífico.

Mathotaarachchi, K. V., Hasan, R., & Mahmood, S. (2024). Advanced machine learning techniques for predictive modeling of property prices. *Information, 15*(6). https://doi.org/10.3390/info15060295

Ministerio de Vivienda, Construcción y Saneamiento. (2016). *Reglamento Nacional de Tasaciones del Perú* (Resolución Ministerial N.° 172-2016-VIVIENDA), modificado por la Resolución Ministerial N.° 124-2020-VIVIENDA (2020).

National Association of Realtors. (s. f.). *Multiple Listing Service (MLS): What is it.* NAR. https://www.nar.realtor/mls-online-listings/multiple-listing-service-mls-what-is-it

Noh, Y., & Park, C. Y. (2026). Explainable machine learning for urban housing prices: Policy relevant diagnostics across three cities. *Cities, 176.* https://doi.org/10.1016/j.cities.2026.107266

OECD/CAF/SELA. (2024). *SME Policy Index: Latin America and the Caribbean 2024: Towards an inclusive, resilient, and sustainable recovery.* OECD Publishing. https://doi.org/10.1787/ba028c1d-en

Oporto D’Ugard, C. S., Canlla Linares, J. A., & Ñique Chacón, C. I. (2025). *Valorización de activos inmobiliarios en una entidad financiera peruana: modelo predictivo* *XGBoost* *usando las características de los inmuebles y variables geográficas.* Universidad Peruana de Ciencias Aplicadas.

Patrocínio, T., Madaleno, M., & Nogueira, M. C. (2024). Does the way variables are calculated change the conclusions to be drawn? A study applied to the ratio ROI (Return on Investment). *Journal of Risk and Financial Management, 17*(7), 266. https://doi.org/10.3390/jrfm17070266

Ponce-Bobadilla, A. V., Schmitt, V., Maier, C. S., Mensing, S., & Stodtmann, S. (2024). Practical guide to SHAP analysis: Explaining supervised machine learning model predictions in drug development. *Clinical and Translational Science, 17*, e70056. https://doi.org/10.1111/cts.70056

Rey-Blanco, D., Zofío, J. L., & González-Arias, J. (2024). Improving hedonic housing price models by integrating optimal accessibility indices into regression and random forest analyses. *Expert Systems with Applications, 235.* https://doi.org/10.1016/J.ESWA.2023.121059

Rodrigues, M. G., Viegas, E. K., Santin, A. O., & Enembreck, F. (2025). A MLOps architecture for near real-time distributed stream learning operation deployment. *Journal of Network and Computer Applications, 238.* https://doi.org/10.1016/J.JNCA.2025.104169

Salunke, S. V., & Ouda, A. (2024). A performance benchmark for the PostgreSQL and MySQL databases. *Future Internet, 16*(10), 382. https://doi.org/10.3390/fi16100382

Superintendencia de Banca, Seguros y Administradoras Privadas de Fondos de Pensiones. (2021). *Reglamento del Registro de Peritos Valuadores que prestan servicio a las empresas del sistema financiero y otros supervisados* (Resolución SBS N.° 03225-2021).

Tanashkin, A. S., Tanashkina, I. G., & Maksimchuik, A. S. (2026). Modern approaches to building interpretable models of the property market using machine learning on the base of mass cadastral valuation. *Land Use Policy, 165*, 107970. https://doi.org/10.1016/J.LANDUSEPOL.2026.107970

Tapia, J., Chavez-Garzon, N., Pezoa, R., Suarez-Aldunate, P., & Pilleux, M. (2025). Comparing automated valuation models for real estate assessment in the Santiago Metropolitan Region: A study on machine learning algorithms and hedonic pricing with spatial adjustments. *PLoS* *ONE, 20*(3). https://doi.org/10.1371/journal.pone.0318701

# 8. Aprobación


| Nombre | Cargo | Firma | Fecha |
| :--- | :--- | :--- | :--- |
| Félix Corrales, Rosa Andrea | Asesora Especializada |  |  |
| Herrera Trujillo, Emilio Antonio | Asesor Metodológico |  |  |
| Cortez Quezada, Joaquín Antonio | Jefe de Proyecto |  |  |
| Pingus Rodríguez, Carlos Daniel | Scrum Master |  |  |
# 9. Anexos


| Anexo | Descripción | Estado |
| :--- | :--- | :--- |
| Anexo 01 | Product Backlog completo: épicas, historias de usuario e historias técnicas con sus criterios de aceptación | Elaborado |
| Anexo 02 | Diagrama BPMN del proceso actual de tasación y captación de inmuebles (As-Is) en MAK Inmobiliaria | Elaborado |
| Anexo 03 | Árbol de problemas de la valoración inmobiliaria y del análisis de rentabilidad | Elaborado |
| Anexo 04 | Informe de Benchmarking de algoritmos de Machine Learning, frameworks de backend y bases de datos | Elaborado |
| Anexo 05 | Diagramas de arquitectura lógica, física e integrada del sistema | Elaborado |
| Anexo 06 | Informe de selección y preprocesamiento del dataset BCRP 2016–2025 | Pendiente (Sprint 0) |
| Anexo 07 | Wireframes y mockups de la interfaz web | Pendiente (Sprint 0) |
| Anexo 08 | Cuestionario System Usability Scale aplicado a los agentes inmobiliarios | Pendiente (Sprint 3) |

