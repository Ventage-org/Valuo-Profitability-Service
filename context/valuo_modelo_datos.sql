-- ============================================================================
-- VALUO - MODELO DE BASE DE DATOS (MER)
-- Sistema de Valoración Inmobiliaria con Machine Learning
-- ============================================================================
-- Motor: PostgreSQL 16
-- Patrón: Database-per-Service (un esquema por microservicio)
--
-- IMPORTANTE - Principio de Microservicios:
--   Las referencias entre esquemas (ej: agente_id en schema_inmuebles) son
--   IDs lógicos, NO foreign keys reales. Cada microservicio es dueño exclusivo
--   de su esquema y no accede directamente a tablas de otro servicio.
--   La integridad referencial entre servicios se garantiza a nivel de
--   aplicación, no a nivel de base de datos.
-- ============================================================================


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 1: USUARIOS Y AUTENTICACIÓN (msUsuarios)                      ║
-- ║  Contenedor: dbUsuarios                                                ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_usuarios;
SET search_path TO schema_usuarios;

-- Enum para roles del sistema
CREATE TYPE schema_usuarios.rol_usuario AS ENUM ('AGENTE', 'ADMINISTRADOR');

CREATE TABLE schema_usuarios.usuarios (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255)    NOT NULL UNIQUE,
    password_hash       VARCHAR(255)    NOT NULL,          -- BCrypt hash
    nombre              VARCHAR(100)    NOT NULL,
    apellido            VARCHAR(100)    NOT NULL,
    telefono            VARCHAR(20),
    rol                 schema_usuarios.rol_usuario NOT NULL DEFAULT 'AGENTE',
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_usuarios.usuarios IS
    'Tabla principal del MS Usuarios. Almacena credenciales y perfil de agentes inmobiliarios y administradores del sistema.';
COMMENT ON COLUMN schema_usuarios.usuarios.password_hash IS
    'Hash BCrypt de la contraseña. Nunca se almacena en texto plano.';
COMMENT ON COLUMN schema_usuarios.usuarios.rol IS
    'AGENTE: usuario operativo que registra inmuebles y solicita predicciones. ADMINISTRADOR: usuario técnico que supervisa el modelo ML.';

-- Índice para login por email
CREATE INDEX idx_usuarios_email ON schema_usuarios.usuarios (email);
-- Índice para filtros por rol (ej: listar solo administradores)
CREATE INDEX idx_usuarios_rol ON schema_usuarios.usuarios (rol);


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 2: GESTIÓN DE INMUEBLES (msInmuebles)                         ║
-- ║  Contenedor: dbInmuebles                                               ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_inmuebles;
SET search_path TO schema_inmuebles;

-- Enum para el estado comercial del inmueble
CREATE TYPE schema_inmuebles.estado_comercial AS ENUM (
    'DISPONIBLE',
    'VENDIDO',
    'ALQUILADO',
    'INACTIVO'
);

-- Enum para el tipo de inmueble
CREATE TYPE schema_inmuebles.tipo_inmueble AS ENUM (
    'DEPARTAMENTO',
    'CASA'
);

-- Catálogo de distritos de Lima Metropolitana
CREATE TABLE schema_inmuebles.distritos (
    id                  SERIAL          PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL UNIQUE,
    provincia           VARCHAR(100)    NOT NULL DEFAULT 'Lima',
    activo              BOOLEAN         NOT NULL DEFAULT TRUE
);

-- Tabla principal de inmuebles
-- Tabla principal de inmuebles
CREATE TABLE schema_inmuebles.inmuebles (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    agente_id               UUID            NOT NULL,           -- Ref lógica → schema_usuarios.usuarios.id
    distrito_id             INTEGER         NOT NULL REFERENCES schema_inmuebles.distritos(id),
    titulo                  VARCHAR(255)    NOT NULL,
    descripcion             TEXT,
    tipo_inmueble           schema_inmuebles.tipo_inmueble NOT NULL DEFAULT 'DEPARTAMENTO',
    superficie_total_m2     DECIMAL(10,2)   NOT NULL CHECK (superficie_total_m2 > 0),
    superficie_techada_m2   DECIMAL(10,2)   CHECK (superficie_techada_m2 > 0),
    habitaciones            SMALLINT        NOT NULL CHECK (habitaciones >= 0),
    banos                   DECIMAL(3,1)    NOT NULL CHECK (banos > 0), -- Permite 1.0, 1.5, 2.0, 2.5
    estacionamientos        SMALLINT        NOT NULL DEFAULT 0 CHECK (estacionamientos >= 0),
    piso                    SMALLINT,                           -- NULL si es casa
    antiguedad_anios        SMALLINT        CHECK (antiguedad_anios >= 0),
    es_estreno              BOOLEAN         NOT NULL DEFAULT FALSE,
    vista_exterior          BOOLEAN         NOT NULL DEFAULT TRUE, -- Requerido por ML M3
    estado_comercial        VARCHAR(20)     NOT NULL DEFAULT 'DISPONIBLE',
    moneda                  VARCHAR(3)      NOT NULL DEFAULT 'USD',
    precio_publicado        DECIMAL(12,2)   CHECK (precio_publicado > 0),
    tipo_cambio_ref         DECIMAL(6,4),                       -- TC vigente al registrar/actualizar
    precio_pen_equiv        DECIMAL(12,2),                      -- Precio normalizado en Soles
    latitud                 DECIMAL(10,8),                      -- Coordenadas GPS para distancias espaciales OSM
    longitud                DECIMAL(11,8),
    direccion               VARCHAR(500),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMPTZ                         -- Soft delete
);

COMMENT ON TABLE schema_inmuebles.inmuebles IS
    'Tabla principal del MS Inmuebles. Almacena propiedades residenciales registradas por agentes para comercialización y valoración con ML M3.';
COMMENT ON COLUMN schema_inmuebles.inmuebles.agente_id IS
    'ID lógico del agente propietario del registro. Referencia a schema_usuarios.usuarios.id. Sin FK física (Database-per-Service).';
COMMENT ON COLUMN schema_inmuebles.inmuebles.superficie_total_m2 IS
    'Superficie total en metros cuadrados. Predictor numérico de mayor peso en el modelo XGBoost.';
COMMENT ON COLUMN schema_inmuebles.inmuebles.vista_exterior IS
    'TRUE si tiene vista a calle o parque, FALSE si es interior o pozo de luz. Variable requerida por el modelo XGBoost M3.';

-- Índices optimizados para filtros de agentes y búsquedas
CREATE INDEX idx_inmuebles_busqueda ON schema_inmuebles.inmuebles (distrito_id, estado_comercial, precio_pen_equiv) WHERE deleted_at IS NULL;
CREATE INDEX idx_inmuebles_agente ON schema_inmuebles.inmuebles (agente_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inmuebles_estado ON schema_inmuebles.inmuebles (estado_comercial);

-- Inserción del catálogo de distritos de Lima Metropolitana
INSERT INTO schema_inmuebles.distritos (nombre) VALUES
    ('Ate'), ('Barranco'), ('Breña'), ('Carabayllo'), ('Chaclacayo'),
    ('Chorrillos'), ('Cieneguilla'), ('Comas'), ('El Agustino'),
    ('Independencia'), ('Jesús María'), ('La Molina'), ('La Victoria'),
    ('Lima'), ('Lince'), ('Los Olivos'), ('Lurigancho-Chosica'),
    ('Lurín'), ('Magdalena del Mar'), ('Miraflores'), ('Pachacámac'),
    ('Pucusana'), ('Pueblo Libre'), ('Puente Piedra'), ('Punta Hermosa'),
    ('Punta Negra'), ('Rímac'), ('San Bartolo'), ('San Borja'),
    ('San Isidro'), ('San Juan de Lurigancho'), ('San Juan de Miraflores'),
    ('San Luis'), ('San Martín de Porres'), ('San Miguel'),
    ('Santa Anita'), ('Santa María del Mar'), ('Santa Rosa'),
    ('Santiago de Surco'), ('Surquillo'), ('Villa El Salvador'),
    ('Villa María del Triunfo'), ('Callao');


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 3: PREDICCIONES ML (msPrediccion)                             ║
-- ║  Contenedor: dbPredicciones                                            ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_predicciones;
SET search_path TO schema_predicciones;

CREATE TABLE schema_predicciones.predicciones (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    agente_id               UUID            NOT NULL,           -- Ref lógica → schema_usuarios.usuarios.id
    inmueble_id             UUID,                               -- Ref lógica → schema_inmuebles.inmuebles.id (NULL si cotización rápida)
    tipo_prediccion         VARCHAR(20)     NOT NULL DEFAULT 'VENTA', -- 'VENTA' (modelo M3) o 'ALQUILER'

    -- Snapshot de features de entrada enviados al modelo
    distrito_nombre         VARCHAR(100)    NOT NULL,
    superficie_m2           DECIMAL(10,2)   NOT NULL,
    habitaciones            SMALLINT        NOT NULL,
    banos                   SMALLINT        NOT NULL,
    estacionamientos        SMALLINT        NOT NULL DEFAULT 0,
    piso                    SMALLINT,
    antiguedad_anios        SMALLINT,
    vista_exterior          BOOLEAN         NOT NULL DEFAULT TRUE, -- Snapshot orientación
    features_contexto       JSONB,                              -- Snapshot variables de entorno (NSE, delincuencia, distancias OSM)

    -- Resultado monetario dual e intervalos de negociación
    valor_estimado_pen      DECIMAL(12,2)   NOT NULL,           -- Salida nativa en Soles tras IPC
    valor_estimado_usd      DECIMAL(12,2)   NOT NULL,           -- Monto convertido a Dólares
    rango_min_usd           DECIMAL(12,2)   NOT NULL,           -- Límite inferior según MAPE
    rango_max_usd           DECIMAL(12,2)   NOT NULL,           -- Límite superior según MAPE

    -- Factores macroeconómicos y metadatos de reproducibilidad
    factor_ipc_usado        DECIMAL(8,4)    NOT NULL DEFAULT 1.6918, -- Factor IPC base (Q1-2026)
    tipo_cambio_usado       DECIMAL(6,4)    NOT NULL,           -- TC USD/PEN en momento de inferencia
    version_modelo          VARCHAR(50)     NOT NULL,           -- Versión registrada en MLflow (ej: "v3.0.0-ampliado")
    mape_referencial        DECIMAL(5,2)    NOT NULL DEFAULT 14.88, -- Error porcentual histórico de soporte
    shap_base_value         DECIMAL(12,2)   NOT NULL,           -- Base value para gráfico Waterfall
    shap_values             JSONB           NOT NULL,           -- Contribuciones SHAP por variable
    tiempo_inferencia_ms    INTEGER,                            -- Latencia del servicio ML en ms
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_predicciones.predicciones IS
    'Tabla principal del MS Predicción. Almacena el historial inmutable de tasaciones con snapshot de features, bimoneda, factores IPC/TC y explicabilidad SHAP para Waterfall.';
COMMENT ON COLUMN schema_predicciones.predicciones.shap_base_value IS
    'Valor esperado base E[f(x)] del modelo XGBoost requerido para renderizar la gráfica Waterfall.';
COMMENT ON COLUMN schema_predicciones.predicciones.shap_values IS
    'Contribuciones marginales SHAP por feature en formato JSONB indexable.';

-- Índices de consulta rápida
CREATE INDEX idx_predicciones_agente ON schema_predicciones.predicciones (agente_id, created_at DESC);
CREATE INDEX idx_predicciones_inmueble ON schema_predicciones.predicciones (inmueble_id, created_at DESC) WHERE inmueble_id IS NOT NULL;
CREATE INDEX idx_predicciones_tipo ON schema_predicciones.predicciones (tipo_prediccion);
CREATE INDEX idx_predicciones_shap ON schema_predicciones.predicciones USING GIN (shap_values);


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 4: ANÁLISIS DE RENTABILIDAD (msRentabilidad)                  ║
-- ║  Contenedor: dbRentabilidad                                            ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_rentabilidad;
SET search_path TO schema_rentabilidad;

CREATE TABLE schema_rentabilidad.calculos_rentabilidad (
    id                              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    agente_id                       UUID            NOT NULL,           -- Ref lógica → schema_usuarios.usuarios.id
    inmueble_id                     UUID,                               -- Ref lógica → schema_inmuebles.inmuebles.id
    prediccion_venta_id             UUID            NOT NULL,           -- Ref lógica → predicciones (XGBoost Venta)
    prediccion_alquiler_id          UUID            NOT NULL,           -- Ref lógica → predicciones (XGBoost Alquiler)

    -- Snapshots de valores predichos por los dos modelos XGBoost
    valor_venta_usd                 DECIMAL(12,2)   NOT NULL CHECK (valor_venta_usd > 0),
    alquiler_mensual_usd            DECIMAL(12,2)   NOT NULL CHECK (alquiler_mensual_usd > 0),

    -- Cálculos operativos intermedios
    ingreso_bruto_anual_usd         DECIMAL(12,2)   NOT NULL,
    oer_ratio                       DECIMAL(5,4)    NOT NULL DEFAULT 0.2000 CHECK (oer_ratio >= 0 AND oer_ratio < 1),
    gastos_operativos_anuales_usd   DECIMAL(12,2)   NOT NULL,
    noi_anual_usd                   DECIMAL(12,2)   NOT NULL,

    -- Métricas financieras finales (convergentes bajo supuestos unlevered)
    cap_rate_porcentaje             DECIMAL(6,4)    NOT NULL,
    roi_unlevered_porcentaje        DECIMAL(6,4)    NOT NULL,

    tipo_cambio_usado               DECIMAL(6,4)    NOT NULL,
    observaciones_supuestos         TEXT,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_rentabilidad.calculos_rentabilidad IS
    'Tabla principal del MS Rentabilidad. Almacena las simulaciones financieras combinando los modelos XGBoost de Venta y Alquiler mediante NOI, OER estándar y Cap Rate/ROI unlevered.';
COMMENT ON COLUMN schema_rentabilidad.calculos_rentabilidad.oer_ratio IS
    'Operating Expense Ratio: porcentaje estándar parametrizable asignado a gastos operativos (por defecto 0.20 = 20%).';
COMMENT ON COLUMN schema_rentabilidad.calculos_rentabilidad.noi_anual_usd IS
    'Net Operating Income anual: Ingreso Bruto Anual × (1 - OER).';

-- Índices de consulta rápida
CREATE INDEX idx_rentabilidad_agente ON schema_rentabilidad.calculos_rentabilidad (agente_id, created_at DESC);
CREATE INDEX idx_rentabilidad_inmueble ON schema_rentabilidad.calculos_rentabilidad (inmueble_id, created_at DESC) WHERE inmueble_id IS NOT NULL;


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 5: GENERACIÓN DE REPORTES (msReportes)                        ║
-- ║  Contenedor: dbReportes                                                ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_reportes;
SET search_path TO schema_reportes;

-- Enum para el estado del reporte
CREATE TYPE schema_reportes.estado_reporte AS ENUM (
    'GENERANDO',
    'COMPLETADO',
    'ERROR'
);

CREATE TABLE schema_reportes.reportes (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    agente_id               UUID            NOT NULL,           -- Ref lógica → usuarios
    inmueble_id             UUID            NOT NULL,           -- Ref lógica → inmuebles
    prediccion_venta_id     UUID,                               -- Ref lógica → predicciones (VENTA)
    prediccion_alquiler_id  UUID,                               -- Ref lógica → predicciones (ALQUILER)
    calculo_rentabilidad_id UUID,                               -- Ref lógica → calculos_rentabilidad

    -- Metadatos del PDF
    nombre_archivo          VARCHAR(255)    NOT NULL,
    ruta_almacenamiento     VARCHAR(500)    NOT NULL,           -- Ruta en el filesystem o bucket
    tamano_bytes            BIGINT,
    estado                  schema_reportes.estado_reporte NOT NULL DEFAULT 'GENERANDO',

    -- Datos incluidos en el reporte (snapshot para regeneración)
    datos_snapshot          JSONB,                              -- Copia consolidada del contenido del reporte

    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_reportes.reportes IS
    'Tabla de metadatos de reportes PDF generados. Registra qué agente lo generó, qué inmueble/predicción/rentabilidad incluye, y la ruta del archivo para descarga.';

-- Tabla de auditoría de descargas
CREATE TABLE schema_reportes.reportes_descargas (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    reporte_id          UUID            NOT NULL REFERENCES schema_reportes.reportes(id) ON DELETE CASCADE,
    descargado_por      UUID            NOT NULL,               -- Ref lógica → usuarios
    ip_cliente          VARCHAR(45),                             -- IPv4 o IPv6
    user_agent          VARCHAR(500),
    downloaded_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_reportes.reportes_descargas IS
    'Log de auditoría de descargas de reportes PDF. Registra quién descargó, cuándo y desde qué IP/dispositivo.';

-- Índices
CREATE INDEX idx_reportes_agente ON schema_reportes.reportes (agente_id);
CREATE INDEX idx_reportes_inmueble ON schema_reportes.reportes (inmueble_id);
CREATE INDEX idx_reportes_estado ON schema_reportes.reportes (estado);
CREATE INDEX idx_reportes_descargas_reporte ON schema_reportes.reportes_descargas (reporte_id);


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 6: AUDITORÍA DE MONITOREO MLOps (msMonitoreo)                 ║
-- ║  Nota: Las métricas del modelo (MAE, RMSE, MAPE) se almacenan          ║
-- ║  en MLflow Tracking Server. Este esquema solo persiste las              ║
-- ║  decisiones administrativas de aprobación/reversión de modelos.         ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_monitoreo;
SET search_path TO schema_monitoreo;

-- Enum para la acción del administrador
CREATE TYPE schema_monitoreo.accion_modelo AS ENUM (
    'APROBADO',
    'REVERTIDO'
);

CREATE TABLE schema_monitoreo.auditoria_modelos (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id                UUID            NOT NULL,           -- Ref lógica → schema_usuarios.usuarios.id (rol ADMINISTRADOR)
    version_modelo          VARCHAR(50)     NOT NULL,           -- Versión del modelo candidato evaluado (ej: "v2.1.0")
    accion                  schema_monitoreo.accion_modelo NOT NULL,

    -- Métricas comparadas (snapshot de MLflow al momento de la decisión)
    metricas_modelo_vigente JSONB           NOT NULL,           -- {"mae": 12500, "rmse": 18200, "mape": 14.2}
    metricas_modelo_candidato JSONB         NOT NULL,           -- {"mae": 11800, "rmse": 17500, "mape": 13.1}

    motivo                  TEXT,                               -- Justificación opcional del administrador
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_monitoreo.auditoria_modelos IS
    'Log de auditoría del MS Monitoreo. Registra cada decisión de aprobación o reversión de modelos ML por parte del administrador. Las métricas son un snapshot de MLflow al momento de la decisión.';
COMMENT ON COLUMN schema_monitoreo.auditoria_modelos.metricas_modelo_vigente IS
    'Snapshot JSON de las métricas del modelo actualmente en producción al momento de la evaluación. Formato: {"mae": float, "rmse": float, "mape": float}.';
COMMENT ON COLUMN schema_monitoreo.auditoria_modelos.metricas_modelo_candidato IS
    'Snapshot JSON de las métricas del modelo candidato propuesto. El administrador compara ambas para tomar la decisión.';

-- Índices
CREATE INDEX idx_auditoria_admin ON schema_monitoreo.auditoria_modelos (admin_id);
CREATE INDEX idx_auditoria_version ON schema_monitoreo.auditoria_modelos (version_modelo);
CREATE INDEX idx_auditoria_fecha ON schema_monitoreo.auditoria_modelos (created_at DESC);


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  ESQUEMA 7: DATASET MAESTRO DE ML (Feature Store Distrital / Pipeline) ║
-- ║  Contenedor: dbDataset                                                 ║
-- ║                                                                        ║
-- ║  NOTA: Esquema analítico (Feature Store / Data Mart).                   ║
-- ║  Alimentado por el Pipeline de Datos (BCRP, ENAHO, PNP, OSM).          ║
-- ║  Leído por el Pipeline de Reentrenamiento del mlService.               ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

CREATE SCHEMA IF NOT EXISTS schema_dataset;
SET search_path TO schema_dataset;

-- 7.1. Tabla dimensional de métricas distritales (Feature Store Distrital)
CREATE TABLE schema_dataset.metricas_distritales (
    id                              SERIAL          PRIMARY KEY,
    distrito_nombre                 VARCHAR(100)    NOT NULL UNIQUE, -- Clave natural distrital (ej: "Miraflores", "Surco")
    distrito_encoded                DECIMAL(12,2)   NOT NULL,       -- Target Encoding bayesiano distrital

    -- Estratos socioeconómicos NSE (ENAHO / INEI)
    pct_nse_a                       DECIMAL(5,2)    NOT NULL CHECK (pct_nse_a >= 0),
    pct_nse_b                       DECIMAL(5,2)    NOT NULL CHECK (pct_nse_b >= 0),
    pct_nse_c                       DECIMAL(5,2)    NOT NULL CHECK (pct_nse_c >= 0),
    pct_nse_d                       DECIMAL(5,2)    NOT NULL CHECK (pct_nse_d >= 0),
    pct_nse_e                       DECIMAL(5,2)    NOT NULL CHECK (pct_nse_e >= 0),

    -- Indicadores de seguridad ciudadana (PNP denuncias / 10,000 hab.)
    tasa_robo                       DECIMAL(8,4)    NOT NULL CHECK (tasa_robo >= 0),
    tasa_hurto                      DECIMAL(8,4)    NOT NULL CHECK (tasa_hurto >= 0),

    -- Demografía y territorio (INEI)
    poblacion_proyectada            INTEGER         NOT NULL CHECK (poblacion_proyectada > 0),
    area_distrito_km2               DECIMAL(10,4)   NOT NULL CHECK (area_distrito_km2 > 0),
    densidad_hab_km2                DECIMAL(10,2)   NOT NULL CHECK (densidad_hab_km2 > 0),

    -- Distancias espaciales geodésicas en km (OSM Overpass API)
    distancia_centro_km             DECIMAL(8,4)    NOT NULL CHECK (distancia_centro_km >= 0),
    dist_colegio_km                 DECIMAL(8,4)    NOT NULL CHECK (dist_colegio_km >= 0),
    dist_hospital_km                DECIMAL(8,4)    NOT NULL CHECK (dist_hospital_km >= 0),
    dist_estacion_transporte_km     DECIMAL(8,4)    NOT NULL CHECK (dist_estacion_transporte_km >= 0),
    dist_centro_comercial_km        DECIMAL(8,4)    NOT NULL CHECK (dist_centro_comercial_km >= 0),
    dist_parque_km                  DECIMAL(8,4)    NOT NULL CHECK (dist_parque_km >= 0),
    dist_universidad_km             DECIMAL(8,4)    NOT NULL CHECK (dist_universidad_km >= 0),

    actualizado_en                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_dataset.metricas_distritales IS
    'Feature Store distrital que consolida NSE (INEI), seguridad ciudadana (PNP) y distancias espaciales (OSM) para alimentar inferencias ML en tiempo real.';

CREATE UNIQUE INDEX idx_metricas_distrito ON schema_dataset.metricas_distritales (distrito_nombre);


-- 7.2. Tabla maestra del dataset de entrenamiento (Inmuebles históricos normalizados)
CREATE TABLE schema_dataset.dataset_inmuebles (
    id                              BIGSERIAL       PRIMARY KEY,
    fuente                          VARCHAR(50)     NOT NULL,       -- 'BCRP', 'CONVENIO_AGENCIAS', 'PORTAL_INMOBILIARIO', 'MANUAL'
    id_anuncio_fuente               VARCHAR(100),
    fecha_publicacion               DATE            NOT NULL,
    anio                            SMALLINT        NOT NULL,       -- Feature ML
    trimestre                       SMALLINT        NOT NULL,       -- Feature ML
    tipo_operacion                  VARCHAR(20)     NOT NULL DEFAULT 'VENTA', -- 'VENTA' o 'ALQUILER'
    distrito_nombre                 VARCHAR(100)    NOT NULL REFERENCES schema_dataset.metricas_distritales(distrito_nombre), -- Clave de enlace hacia el Feature Store

    -- Atributos físicos del departamento (Features ML)
    superficie_m2                   DECIMAL(10,2)   NOT NULL CHECK (superficie_m2 > 0),
    habitaciones                    SMALLINT        NOT NULL CHECK (habitaciones >= 0),
    banos                           SMALLINT        NOT NULL CHECK (banos >= 0),
    estacionamientos                SMALLINT        NOT NULL DEFAULT 0,
    piso                            SMALLINT        NOT NULL DEFAULT 1,
    antiguedad_anios                SMALLINT        NOT NULL DEFAULT 0,
    vista_exterior                  BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Variables financieras originales y macroeconómicas (BCRP)
    precio_original                 DECIMAL(12,2)   NOT NULL,
    moneda_original                 VARCHAR(3)      NOT NULL DEFAULT 'USD',
    tipo_cambio                     DECIMAL(6,4)    NOT NULL,
    precio_soles_nominal            DECIMAL(12,2)   NOT NULL,
    ipc                             DECIMAL(8,4)    NOT NULL,
    precio_soles_const              DECIMAL(12,2)   NOT NULL,       -- TARGET DEL MODELO (Soles constantes Base 2009=100)

    -- Flags de calidad y particionamiento ML
    es_duplicado                    BOOLEAN         NOT NULL DEFAULT FALSE,
    es_outlier                      BOOLEAN         NOT NULL DEFAULT FALSE,
    incluido_en_entrenamiento       BOOLEAN         NOT NULL DEFAULT TRUE,
    split_dataset                   VARCHAR(10)     NOT NULL DEFAULT 'TRAIN', -- 'TRAIN' (2016-2023) o 'TEST' (2024-2025)

    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE schema_dataset.dataset_inmuebles IS
    'Data Mart analítico normalizado (3FN). Almacena ofertas históricas del BCRP (2016-2025), atributos físicos y target deflactado por IPC. Las variables del entorno urbano se obtienen mediante JOIN con metricas_distritales.';

CREATE INDEX idx_dataset_entrenamiento ON schema_dataset.dataset_inmuebles (tipo_operacion, split_dataset) WHERE incluido_en_entrenamiento = TRUE AND es_outlier = FALSE;
CREATE INDEX idx_dataset_distrito_periodo ON schema_dataset.dataset_inmuebles (distrito_nombre, anio, trimestre);

-- Vista analítica para entrenamiento de Machine Learning (Une atributos de inmueble con métricas distritales)
CREATE OR REPLACE VIEW schema_dataset.vw_dataset_entrenamiento AS
SELECT 
    i.id,
    i.fuente,
    i.fecha_publicacion,
    i.anio,
    i.trimestre,
    i.tipo_operacion,
    i.distrito_nombre,
    i.superficie_m2,
    i.habitaciones,
    i.banos,
    i.estacionamientos,
    i.piso,
    i.antiguedad_anios,
    i.vista_exterior,
    i.precio_soles_const,
    i.split_dataset,
    m.distrito_encoded,
    m.pct_nse_a,
    m.pct_nse_b,
    m.pct_nse_c,
    m.pct_nse_d,
    m.pct_nse_e,
    m.tasa_robo,
    m.tasa_hurto,
    m.poblacion_proyectada,
    m.area_distrito_km2,
    m.densidad_hab_km2,
    m.distancia_centro_km,
    m.dist_colegio_km,
    m.dist_hospital_km,
    m.dist_estacion_transporte_km,
    m.dist_centro_comercial_km,
    m.dist_parque_km,
    m.dist_universidad_km
FROM schema_dataset.dataset_inmuebles i
INNER JOIN schema_dataset.metricas_distritales m 
    ON i.distrito_nombre = m.distrito_nombre
WHERE i.incluido_en_entrenamiento = TRUE AND i.es_outlier = FALSE;

COMMENT ON VIEW schema_dataset.vw_dataset_entrenamiento IS
    'Vista que materializa el cruce de datos entre el inmueble y el Feature Store distrital para consumo directo por XGBoost en Python.';


-- 7.3. Log operativo y de auditoría del Pipeline de Datos y MLOps
CREATE TABLE schema_dataset.pipeline_ejecuciones (
    id                              BIGSERIAL       PRIMARY KEY,
    tipo_ejecucion                  VARCHAR(50)     NOT NULL,       -- 'INGESTA_BCRP', 'CARGA_AGENCIAS', 'ENRIQUECIMIENTO_DISTRITAL', 'ACTUALIZACION_OSM', 'REENTRENAMIENTO_ML'
    ejecutado_por                   VARCHAR(50)     NOT NULL DEFAULT 'CRON_AUTOMATICO', -- 'CRON_AUTOMATICO', 'ADMIN_MANUAL', 'PIPELINE_CI_CD'
    iniciado_en                     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalizado_en                   TIMESTAMPTZ,
    duracion_segundos               INTEGER,
    registros_procesados            INTEGER         NOT NULL DEFAULT 0,
    registros_nuevos                INTEGER         NOT NULL DEFAULT 0,
    registros_duplicados            INTEGER         NOT NULL DEFAULT 0,
    registros_outliers              INTEGER         NOT NULL DEFAULT 0,
    errores                         INTEGER         NOT NULL DEFAULT 0,
    metricas_salida                 JSONB,                          -- Snapshot de métricas si fue reentrenamiento (MAPE, R2, etc.)
    observaciones                   TEXT,
    estado                          VARCHAR(20)     NOT NULL DEFAULT 'EN_PROCESO' -- 'EN_PROCESO', 'COMPLETADO', 'ADVERTENCIA', 'FALLIDO'
);

COMMENT ON TABLE schema_dataset.pipeline_ejecuciones IS
    'Log operativo y de auditoría MLOps para corridas de ingesta de datos, enriquecimiento distrital y reentrenamientos de modelos.';

CREATE INDEX idx_pipeline_tipo_fecha ON schema_dataset.pipeline_ejecuciones (tipo_ejecucion, iniciado_en DESC);
CREATE INDEX idx_pipeline_fallos ON schema_dataset.pipeline_ejecuciones (estado) WHERE estado = 'FALLIDO';


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  RESTAURAR search_path                                                 ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

RESET search_path;
