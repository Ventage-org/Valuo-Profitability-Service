# Especificación Técnica de Desarrollo: Módulos Core (IAM, Inmuebles y Predicción)
## Sistema Valuo — Estimación de Mercado Inmobiliario y Rentabilidad (Lima Metropolitana)

* **Propósito del Documento:** Guía técnica detallada, diseño de componentes frontend, contratos de endpoints REST y lógica de negocio con Spring Data JPA para los módulos 1 (IAM), 2 (Inmuebles) y 3 (Predicción).
* **Destinatario:** Desarrollador responsable de la implementación de los módulos base.

---

## ⚠️ 1. Regla de Oro y Terminología Legal / Técnica

> [!IMPORTANT]
> **NO UTILIZAR EL TÉRMINO "TASACIÓN" NI "INMUEBLE TASADO" EN NINGUNA PANTALLA, CÓDIGO O BASE DE DATOS.**
> * **Razón Legal y Regulatoria en el Perú:** Una "Tasación" formal es un acto pericial normado por el Reglamento Nacional de Tasaciones del Perú (Resolución Ministerial N° 172-2016-VIVIENDA) y exige la firma de un perito colegiado habilitado en el Cuerpo Técnico de Tasaciones del Perú (CTP) o en el REPEV de la SBS.
> * **Término Oficial del Sistema:** **"Estimación de Precio de Mercado"**, **"Valor Estimado Referencial"** o **"Proyección Predictiva con Machine Learning"**.
> * **En la Interfaz de Usuario:** Utilizar etiquetas como: *"Precio Estimado Sugerido"*, *"Valor Referencial de Mercado"*, *"Historial de Estimaciones"*, *"Nueva Estimación"*.

---

## 🏛️ 2. Arquitectura de Componentes y Flujo de Comunicación

La solución se estructura en una aplicación cliente **Angular (SPA)** que se comunica vía HTTP/JSON con los microservicios backend Spring Boot (Java 17 con Spring Data JPA) y el motor de inferencia analítica FastAPI (Python 3.11):

```
+-----------------------------------------------------------------------------------------------------------------+
| CAPA FRONTEND: ANGULAR SPA                                                                                      |
|                                                                                                                 |
|   [Módulo IAM]                         [Módulo Inmuebles]                    [Módulo Predicción]                 |
|   - LoginComponent                     - DashboardComponent                  - NuevaEstimacionComponent         |
|   - RegisterComponent                  - CarteraComponent                    - ResultadoEstimacionComponent     |
|   - ProfileComponent                   - InmuebleDetalleComponent            - ShapChartComponent               |
|                                        - CambiarEstadoModalComponent                                            |
+-----------------------------------------------------------------------------------------------------------------+
         |                                        |                                        |
         | HTTP (JWT)                             | HTTP (JWT)                             | HTTP (JWT)
         v                                        v                                        v
+------------------------+              +------------------------+              +-------------------------+
| msUsuarios (Port 8081) |              | msInmuebles (Port 8082)|              | msPrediccion (Port 8083)|
| - AuthController       |              | - InmuebleController   |              | - PrediccionController  |
| - UsuarioController    |              | - DashboardController  |              | - CatalogoController    |
+------------------------+              +------------------------+              +-------------------------+
         |                                        |                                        |
         | Spring Data JPA                        | Spring Data JPA                        | HTTP Interno (<50ms)
         | schema_usuarios                        | schema_inmuebles                       v
         +----------------------------------------+--------------------->       +-------------------------+
                                                                                | mlService (Port 8000)   |
                                                                                | FastAPI / Uvicorn       |
                                                                                | XGBoost + TreeExplainer |
                                                                                +-------------------------+
```

---

## 👤 3. MÓDULO 1: Gestión de Usuarios e Identidad (IAM)

Este módulo gestiona la autenticación, control de acceso basado en roles (RBAC) y los perfiles de los agentes inmobiliarios.

```
MÓDULO 1: IAM
├── Vistas Frontend
│   ├── LoginComponent (/login)
│   ├── RegisterComponent (/register)
│   └── ProfileComponent (/profile)
└── Backend (msUsuarios: 8081)
    ├── AuthController (/api/v1/auth)
    └── UsuarioController (/api/v1/usuarios)
```

### 3.1. Vista 1: Registro de Agente Inmobiliario (`/register`)

#### A. Componente Frontend: `RegisterComponent`
* **Formulario Reactivo (`FormGroup`):**
  * `nombre` y `apellido`: Text inputs requeridos.
  * `email`: Input requerido con validación de patrón de correo.
  * `telefono`: Input requerido con máscara de 9 dígitos para celular peruano (`^9[0-9]{8}$`).
  * `password`: Input con validación de complejidad (mínimo 8 caracteres, al menos 1 número y 1 mayúscula).
  * `ruc`: **Input OPCIONAL**. *Regla de negocio:* No todos los agentes tienen RUC al momento de registrarse. Si se escribe, se valida que contenga **exactamente 11 dígitos numéricos** (iniciando con 10 o 20). Si se deja vacío, viaja como `null`.
  * `nombreAgencia`: Input opcional para el nombre comercial o franquicia (ej. "Remax Diamond", "Independiente").
  * `distritoEspecializacionId`: Desplegable `<select>` cargado dinámicamente desde el catálogo de distritos.

#### B. Endpoints y Lógica de Negocio

##### 1. Carga del Catálogo de Distritos (al inicializar la vista `ngOnInit`)
* **Endpoint:** `GET /api/v1/distritos`
* **Lógica Backend JPA:**
  * El servicio invoca `distritoRepository.findByActivoTrueOrderByNombreAsc()`.
  * Retorna la lista de entidades activas mapeadas a DTOs livianos.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  [
    { "id": 16, "nombre": "Miraflores", "zonaUrbana": "Lima Top" },
    { "id": 19, "nombre": "San Isidro", "zonaUrbana": "Lima Top" },
    { "id": 9, "nombre": "Jesús María", "zonaUrbana": "Lima Moderna" }
  ]
  ```
* **Binding Frontend:** Se itera en el template HTML:
  ```html
  <select formControlName="distritoEspecializacionId" class="form-select">
    <option [ngValue]="null">Seleccione su distrito de especialización</option>
    <option *ngFor="let d of distritos" [value]="d.id">{{ d.nombre }} ({{ d.zonaUrbana }})</option>
  </select>
  ```

##### 2. Envío del Formulario de Registro
* **Endpoint:** `POST /api/v1/auth/register`
* **Headers:** `Content-Type: application/json`
* **Request Body:**
  ```json
  {
    "nombre": "Carlos",
    "apellido": "Mendoza",
    "email": "cmendoza@agenciavaluo.pe",
    "telefono": "987654321",
    "password": "Password123!",
    "ruc": "10456789012",
    "nombreAgencia": "Mendoza Bienes Raíces",
    "distritoEspecializacionId": 16
  }
  ```
* **Lógica Backend JPA (`UsuarioServiceImpl`):**
  1. Verifica disponibilidad de email con `usuarioRepository.existsByEmail(dto.getEmail())`. Si ya existe, lanza excepción de conflicto (`409 Conflict`).
  2. Encripta la contraseña usando `BCryptPasswordEncoder.encode(dto.getPassword())`.
  3. Instancia la entidad `Usuario`, asignando rol `RolUsuario.AGENTE`, estado `activo = true` y los datos del DTO.
  4. Guarda con `usuarioRepository.save(usuario)`.
* **Respuesta Exitosa (`201 Created`):**
  ```json
  {
    "mensaje": "Usuario registrado exitosamente",
    "usuarioId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
  }
  ```
* **Comportamiento Frontend:** Muestra un toast de éxito y redirige a `/login`.

---

### 3.2. Vista 2: Inicio de Sesión (`/login`)

#### A. Componente Frontend: `LoginComponent`
* Formulario simple con campos `email` y `password`, botón de ingresar y enlace a registro.

#### B. Endpoint y Lógica de Negocio
* **Endpoint:** `POST /api/v1/auth/login`
* **Request Body:**
  ```json
  {
    "email": "cmendoza@agenciavaluo.pe",
    "password": "Password123!"
  }
  ```
* **Lógica Backend JPA:**
  1. Recupera la entidad mediante `usuarioRepository.findByEmailAndActivoTrue(dto.getEmail())`.
  2. Compara contraseñas usando `passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())`. Si no coincide, retorna `401 Unauthorized` genérico.
  3. Genera un token JWT firmado con `HMAC-SHA256` con caducidad de 8 horas conteniendo:
     * `sub`: ID del usuario (`UUID`).
     * `email`: Correo electrónico.
     * `role`: `'AGENTE'` o `'ADMINISTRADOR'`.
     * `nombreCompleto`: Nombre y Apellido.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipo": "Bearer",
    "expiracionMs": 28800000,
    "usuario": {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "nombre": "Carlos",
      "apellido": "Mendoza",
      "email": "cmendoza@agenciavaluo.pe",
      "rol": "AGENTE"
    }
  }
  ```
* **Comportamiento Frontend (`AuthService`):**
  * Almacena el token en `localStorage` o cookie de sesión.
  * Configura el `HttpInterceptor` para adjuntar automáticamente la cabecera `Authorization: Bearer <token>` en todas las llamadas subsiguientes.
  * Redirige al Dashboard principal `/dashboard`.

---

### 3.3. Vista 3: Visualización y Edición de Perfil (`/profile`)

#### A. Componente Frontend: `ProfileComponent`
* **Datos Mostrados:**
  * Tarjeta de datos personales: Nombre, apellido, email (campo de solo lectura), teléfono editable, RUC editable, nombre de agencia y distrito de especialización.
  * **Sección de Firma / Rúbrica / Logotipo:**
    * Muestra la previsualización de la firma o logotipo digital actual.
    * Input de carga de archivo (`<input type="file" accept="image/png, image/jpeg">`).
    * *Propósito Indispensable:* Esta firma/logo es requerida por el Módulo de Reportes PDF para sellar los reportes comerciales con la identidad del agente.

#### B. Endpoints y Lógica de Negocio

##### 1. Obtener Datos del Perfil
* **Endpoint:** `GET /api/v1/usuarios/perfil`
* **Headers:** `Authorization: Bearer <token>`
* **Lógica Backend JPA:**
  * Extrae el `userId` autenticado desde el SecurityContext / token JWT.
  * Invoca `usuarioRepository.findByIdAndActivoTrue(userId)`.
  * Mapea la entidad a `UsuarioPerfilResponseDTO`.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "email": "cmendoza@agenciavaluo.pe",
    "nombre": "Carlos",
    "apellido": "Mendoza",
    "telefono": "987654321",
    "ruc": "10456789012",
    "nombreAgencia": "Mendoza Bienes Raíces",
    "distritoEspecializacionId": 16,
    "urlFirmaDigital": "https://storage.valuo.pe/firmas/agente-carlos-mendoza.png",
    "urlFotoPerfil": "https://storage.valuo.pe/fotos/agente-carlos.jpg"
  }
  ```
* **Binding Frontend:** Setea los valores en el formulario reactivo y asigna `firmaPreviewUrl = data.urlFirmaDigital`.

##### 2. Carga de Archivo de Firma Digital / Logotipo
* **Endpoint:** `POST /api/v1/usuarios/perfil/firma`
* **Headers:** `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`
* **Lógica Backend JPA:**
  * Valida el formato de imagen y la sube al almacenamiento (Cloudinary / Azure Blob Storage).
  * Obtiene la URL pública generada.
  * Recupera el usuario con `usuarioRepository.findById(userId)`, actualiza `usuario.setUrlFirmaDigital(urlGenerada)` y guarda con JPA.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  {
    "urlFirmaDigital": "https://storage.valuo.pe/firmas/agente-carlos-mendoza-v2.png"
  }
  ```

##### 3. Actualizar Datos de Perfil
* **Endpoint:** `PUT /api/v1/usuarios/perfil`
* **Request Body:**
  ```json
  {
    "telefono": "999888777",
    "ruc": "10456789012",
    "nombreAgencia": "Mendoza & Asociados Inmobiliaria",
    "distritoEspecializacionId": 16,
    "urlFirmaDigital": "https://storage.valuo.pe/firmas/agente-carlos-mendoza-v2.png"
  }
  ```
* **Lógica Backend JPA:**
  * En un método con `@Transactional`, recupera la entidad `Usuario`.
  * Aplica los cambios mediante setters y deja que el mecanismo de *Dirty Checking* de JPA sincronice los datos.
* **Respuesta Exitosa (`200 OK`):** Objeto de perfil actualizado.

---

## 🏡 4. MÓDULO 2: Gestión de Cartera de Inmuebles

Este módulo centraliza la administración de propiedades del agente, su estado comercial y el historial de estimaciones asociadas a cada inmueble.

```
MÓDULO 2: INMUEBLES
├── Vistas Frontend
│   ├── DashboardComponent (/dashboard)
│   ├── CarteraComponent (/inmuebles)
│   └── InmuebleDetalleComponent (/inmuebles/:id)
│       └── CambiarEstadoModalComponent
└── Backend (msInmuebles: 8082)
    ├── DashboardController (/api/v1/inmuebles/dashboard)
    └── InmuebleController (/api/v1/inmuebles)
```

### 4.1. Vista 1: Pantalla Principal / Dashboard del Agente (`/dashboard`)

#### A. Estrategia de Endpoints del Dashboard: ¿Uno solo o Múltiples?

> [!TIP]
> **Decisión Arquitectónica:** En arquitecturas de microservicios limpias **NO es recomendable mezclar todo en un único endpoint monolítico** porque obligaría a `msInmuebles` a consultar tablas de `msPrediccion` (violando el principio de Database-per-Service) o a acoplarse sincrónicamente.
> 
> **Enfoque Adoptado (Endpoints Específicos y Desacoplados):**
> 1. `GET /api/v1/inmuebles/dashboard/kpis` $\rightarrow$ Servido por `msInmuebles`.
> 2. `GET /api/v1/inmuebles/recientes` $\rightarrow$ Servido por `msInmuebles`.
> 3. `GET /api/v1/predicciones/recientes` $\rightarrow$ Servido por `msPrediccion`.
> 
> **En Angular (`DashboardComponent`):** Se consumen de forma concurrente con `forkJoin` o suscripciones asíncronas independientes. Si uno tarda unos milisegundos más, las tarjetas de KPIs ya se muestran sin bloquear el resto de la pantalla.

#### B. Componente Frontend: `DashboardComponent`
Se compone de 3 bloques funcionales claros y directos:

1. **Bloque de Indicadores (KPI Cards):**
   * *Inmuebles Activos:* Propiedades en estado `DISPONIBLE`.
   * *Estimaciones Generadas:* Total de predicciones solicitadas por el agente.
   * *Inmuebles Cerrados:* Propiedades vendidas o alquiladas en el año actual (`VENDIDO` + `ALQUILADO`).
2. **Bloque de Inmuebles Activos Recientes (Sustituto de "Cartera Destacada"):**
   * Muestra las últimas 4 propiedades registradas por el agente (miniatura de foto, precio publicado, m² y distrito).
3. **Bloque de Historial de Estimaciones Recientes:**
   * Tabla con las últimas 5 estimaciones realizadas (Fecha, Inmueble/Distrito, Tipo Venta/Alquiler, Valor estimado USD y botón para ver detalle).
   * *Nota de Alcance:* Se omite la sección de SHAP global de mercado en el dashboard para enfocar el esfuerzo en el SHAP individual de cada estimación.

#### C. Endpoints y Lógica de Negocio JPA

##### Endpoint 1: Métricas / KPIs del Agente
* **Microservicio:** `msInmuebles`
* **Endpoint:** `GET /api/v1/inmuebles/dashboard/kpis`
* **Headers:** `Authorization: Bearer <token>`
* **Lógica Backend JPA (`DashboardServiceImpl`):**
  * Extrae `agenteId` del token JWT.
  * Para inmuebles activos: invoca `inmuebleRepository.countByAgenteIdAndEstadoComercialAndDeletedAtIsNull(agenteId, EstadoComercial.DISPONIBLE)`.
  * Para cierres del año: invoca `inmuebleRepository.countCierresAnioActual(agenteId, anioActual)` filtrando por estados `VENDIDO` y `ALQUILADO` dentro del año calendario.
  * Para el total de estimaciones: llama mediante cliente Feign/WebClient a `msPrediccion` (`GET /api/v1/predicciones/conteo?agenteId=...`) o se consulta de forma directa desde Angular en paralelo.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  {
    "inmueblesActivos": 12,
    "estimacionesTotales": 48,
    "cierresAnioActual": 5
  }
  ```

##### Endpoint 2: Inmuebles Recientes del Agente
* **Microservicio:** `msInmuebles`
* **Endpoint:** `GET /api/v1/inmuebles/recientes`
* **Headers:** `Authorization: Bearer <token>`
* **Lógica Backend JPA:**
  * Invoca `inmuebleRepository.findTop4ByAgenteIdAndDeletedAtIsNullOrderByCreatedAtDesc(agenteId)`.
  * Mapea las entidades a DTOs incluyendo la URL de la imagen principal.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  [
    {
      "id": "b2c3d4e5-1111-2222-3333-444455556666",
      "titulo": "Dpto frente al Golf",
      "distrito": "San Isidro",
      "precio": 320000.00,
      "moneda": "USD",
      "superficieTotalM2": 140.0,
      "urlImagen": "https://storage.valuo.pe/inmuebles/golf-principal.jpg"
    }
  ]
  ```

##### Endpoint 3: Estimaciones Recientes del Agente
* **Microservicio:** `msPrediccion`
* **Endpoint:** `GET /api/v1/predicciones/recientes`
* **Headers:** `Authorization: Bearer <token>`
* **Lógica Backend JPA:**
  * Invoca `prediccionRepository.findTop5ByAgenteIdOrderByCreatedAtDesc(agenteId)`.
  * Mapea a DTOs con fecha, distrito, tipo de operación y valor estimado.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  [
    {
      "id": "f1a2b3c4-7777-8888-9999-000011112222",
      "distrito": "Miraflores",
      "tipoOperacion": "VENTA",
      "valorEstimadoUsd": 195000.00,
      "fecha": "2026-10-05T14:30:00Z"
    }
  ]
  ```

---

### 4.2. Vista 2: Catálogo "Mi Cartera" (`/inmuebles`)

#### A. Componente Frontend: `CarteraComponent`
* **Barra de Métricas Superiores:** Disponibles (badge verde), En Negociación (badge amarillo), Cerrados (badge azul), Desactivados (badge gris).
* **Filtros Multicriterio Reactivos:**
  * *Distrito:* Dropdown con los 22 distritos del BCRP.
  * *Tipo:* Radio buttons o select con solo dos opciones: `DEPARTAMENTO` o `CASA`.
  * *Estado Comercial:* `DISPONIBLE`, `EN_NEGOCIACION`, `VENDIDO`, `ALQUILADO`, `INACTIVO`.
  * *Rango de Precio:* Inputs de valor mínimo y máximo en USD.
* **Cuadrícula / Lista Paginada de Propiedades:**
  * Muestra tarjetas con la foto principal, badge del estado comercial, título, distrito, m², habitaciones, baños y precio publicado / estimado.
  * Botón directo: *[Ver Detalle]* que navega a `/inmuebles/:id`.

#### B. Endpoint y Lógica de Negocio JPA
* **Endpoint:** `GET /api/v1/inmuebles`
* **Parámetros Query:**
  `?distritoId=16&tipo=DEPARTAMENTO&estado=DISPONIBLE&precioMin=100000&precioMax=300000&pagina=0&tamanio=9`
* **Lógica Backend JPA (`InmuebleServiceImpl`):**
  1. Construye una especificación dinámica mediante `JpaSpecification` combinando predicados `CriteriaBuilder`:
     * Obligatorio: `agenteId == authUserId`.
     * Condicional: si `estado != INACTIVO`, se exige `deletedAt IS NULL`; si se filtra por `INACTIVO`, se evalúa `estadoComercial == INACTIVO`.
     * Filtros opcionales: distrito, tipo de inmueble y rango de precios.
  2. Ejecuta la consulta paginada:
     `inmuebleRepository.findAll(specification, PageRequest.of(pagina, tamanio, Sort.by("createdAt").descending()))`.
  3. Mapea la página de entidades a una página de DTOs para la vista.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  {
    "resumenEstados": {
      "disponibles": 12,
      "enNegociacion": 3,
      "cerrados": 5,
      "desactivados": 2
    },
    "paginacion": {
      "totalElementos": 12,
      "totalPaginas": 2,
      "paginaActual": 0,
      "contenido": [
        {
          "id": "e3a1b2c4-8888-4444-9999-111122223333",
          "titulo": "Departamento con vista al mar",
          "tipoInmueble": "DEPARTAMENTO",
          "distrito": "Barranco",
          "superficieTotalM2": 88.0,
          "habitaciones": 2,
          "banos": 2.0,
          "estacionamientos": 1,
          "precioPublicado": 175000.00,
          "moneda": "USD",
          "estadoComercial": "DISPONIBLE",
          "urlImagenPrincipal": "https://storage.valuo.pe/inmuebles/barranco-1.jpg"
        }
      ]
    }
  }
  ```
* **Binding Frontend:**
  * Al modificar cualquier control de filtro, Angular vuelve a emitir la petición con la nueva combinación de parámetros y refresca la lista.

---

### 4.3. Vista 3: Ficha Detalle del Inmueble (`/inmuebles/:id`)

#### A. Componente Frontend: `InmuebleDetalleComponent`
Contiene la información integral del inmueble y centraliza las acciones operativas:

1. **Galería de Imágenes:** Slider/carrusel de fotografías cargadas del inmueble.
2. **Ficha de Atributos Físicos:** Tabla de características (superficie total y techada, ambientes, piso, antigüedad, estreno, vista).
3. **Ubicación en Mapa:** Mapa estático o embebido centrado en `latitud` y `longitud` del inmueble.
4. **Acciones Operativas Principales:**
   * **[Cambiar Estado Comercial]:** Abre el modal `CambiarEstadoModalComponent` para actualizar a `EN_NEGOCIACION`, `VENDIDO`, `ALQUILADO` o `DISPONIBLE`.
   * **[Desactivar de Cartera]:** Botón con alerta de confirmación para aplicar Soft Delete.
   * **[Calcular Rentabilidad Financiera]:** Redirige al Módulo 4 (`/rentabilidad/nuevo?inmuebleId=...`).
   * **[Nueva Estimación para este Inmueble]:** Permite re-estimar el valor si se actualizaron características físicas.
5. **Historial de Estimaciones Realizadas:**
   * Tabla histórica con cada cálculo predictivo ejecutado para este inmueble:
     * Columnas: `Fecha de cálculo` | `Operación (Venta / Alquiler)` | `Valor Estimado (USD)` | `Rango Min - Max` | `Acción: [Ver Factores SHAP]`.
     * Al presionar *[Ver Factores SHAP]* se abre un modal con el gráfico de barras individual de esa fecha.

#### B. Endpoints y Lógica de Negocio JPA

##### 1. Obtener Ficha del Inmueble
* **Microservicio:** `msInmuebles`
* **Endpoint:** `GET /api/v1/inmuebles/{id}`
* **Lógica Backend JPA:**
  * Invoca `inmuebleRepository.findById(id)` cargando la relación de imágenes con `@EntityGraph` o Join Fetch para evitar problemas de N+1 queries.
  * Valida que el inmueble pertenezca al `agenteId` autenticado.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  {
    "id": "e3a1b2c4-8888-4444-9999-111122223333",
    "titulo": "Departamento con vista al mar",
    "descripcion": "Acogedor departamento en malecón de Barranco con excelente iluminación natural.",
    "tipoInmueble": "DEPARTAMENTO",
    "distritoId": 2,
    "distritoNombre": "Barranco",
    "superficieTotalM2": 88.0,
    "superficieTechadaM2": 88.0,
    "habitaciones": 2,
    "banos": 2.0,
    "estacionamientos": 1,
    "piso": 5,
    "antiguedadAnios": 3,
    "esEstreno": false,
    "vistaExterior": true,
    "estadoComercial": "DISPONIBLE",
    "moneda": "USD",
    "precioPublicado": 175000.00,
    "direccion": "Malecón Paul Harris 320",
    "latitud": -12.1485,
    "longitud": -77.0221,
    "imagenes": [
      { "id": "img-1", "url": "https://storage.valuo.pe/inmuebles/barranco-1.jpg", "esPrincipal": true },
      { "id": "img-2", "url": "https://storage.valuo.pe/inmuebles/barranco-2.jpg", "esPrincipal": false }
    ]
  }
  ```

##### 2. Obtener Historial de Estimaciones del Inmueble
* **Microservicio:** `msPrediccion`
* **Endpoint:** `GET /api/v1/predicciones/inmueble/{inmuebleId}`
* **Lógica Backend JPA:**
  * Invoca `prediccionRepository.findAllByInmuebleIdOrderByCreatedAtDesc(inmuebleId)`.
  * Retorna la lista de snapshots con valores estimados y el objeto JSONB de `shapValues`.
* **Respuesta Exitosa (`200 OK`):**
  ```json
  [
    {
      "id": "pred-999",
      "tipoPrediccion": "VENTA",
      "valorEstimadoUsd": 172400.00,
      "valorEstimadoPen": 646500.00,
      "rangoMinUsd": 146471.04,
      "rangoMaxUsd": 198328.96,
      "versionModelo": "v1.2.0-2026-XGB",
      "fecha": "2026-10-05T16:20:00Z",
      "shapValues": {
        "Superficie Total (88 m²)": 15400.0,
        "Distrito (Barranco)": 12800.0,
        "Vista Exterior": 3200.0,
        "Antigüedad (3 años)": -2500.0
      }
    }
  ]
  ```

##### 3. Actualizar Estado Comercial (desde el Modal)
* **Microservicio:** `msInmuebles`
* **Endpoint:** `PATCH /api/v1/inmuebles/{id}/estado`
* **Request Body:**
  ```json
  {
    "nuevoEstado": "VENDIDO"
  }
  ```
* **Lógica Backend JPA:**
  * En método anotado con `@Transactional`:
    1. Recupera la entidad: `inmuebleRepository.findByIdAndAgenteId(id, authUserId)`.
    2. Aplica el cambio: `inmueble.setEstadoComercial(dto.getNuevoEstado())`.
    3. JPA sincroniza automáticamente la actualización en la base de datos al finalizar la transacción.
* **Respuesta Exitosa (`200 OK`):** `{ "mensaje": "Estado comercial actualizado exitosamente" }`.

##### 4. Desactivar Inmueble (Soft Delete)
* **Microservicio:** `msInmuebles`
* **Endpoint:** `DELETE /api/v1/inmuebles/{id}`
* **Lógica Backend JPA:**
  * En método anotado con `@Transactional`:
    1. Recupera la entidad por ID y agente.
    2. Establece `inmueble.setEstadoComercial(EstadoComercial.INACTIVO)`.
    3. Registra la marca temporal de borrado lógico: `inmueble.setDeletedAt(LocalDateTime.now())`.
* **Respuesta Exitosa (`200 OK`):** `{ "mensaje": "Inmueble retirado de la cartera activa" }`.

---

## 📈 5. MÓDULO 3: Predicción y Estimación de Mercado

Este módulo ejecuta la inferencia estadística sobre los modelos de Machine Learning (XGBoost) entrenados con datos del BCRP, calcula las bandas de confianza basadas en el MAPE y genera la explicabilidad de factores con SHAP individual.

```
MÓDULO 3: ESTIMACIÓN PREDICTIVA
├── Vistas Frontend
│   ├── NuevaEstimacionComponent (/predicciones/nueva)
│   └── ResultadoEstimacionComponent (/predicciones/resultado/:id)
│       └── ShapChartComponent (Barras horizontales de explicabilidad)
└── Backend
    ├── msPrediccion (Port 8083 - Spring Boot JPA)
    │   └── PrediccionController (/api/v1/predicciones)
    └── mlService (Port 8000 - Python FastAPI)
        ├── POST /predict/sale
        └── POST /predict/rental
```

### 5.1. Vista 1: Formulario de Nueva Estimación (`/predicciones/nueva`)

#### A. Componente Frontend: `NuevaEstimacionComponent`
Diseñado para la captura limpia y validada de las variables requeridas por el modelo analítico:

1. **Tipo de Inmueble:**
   * Selector Toggle: `[ DEPARTAMENTO ]` o `[ CASA ]`.
   * *Lógica de Formulario Reactivo:*
     * Si el usuario selecciona `CASA`: el campo `piso` se **deshabilita**, se remueve su validador y su valor se fija en `null`.
     * Si selecciona `DEPARTAMENTO`: el campo `piso` se **habilita** como numérico requerido ($\ge 1$).
2. **Catálogo de Distritos BCRP:**
   * Desplegable `<select>` limitado a los **22 distritos del BCRP**:
     * Ate Vitarte, Barranco, Bellavista, Breña, Carabayllo, Cercado de Lima, Chorrillos, Comas, Jesús María, La Molina, La Perla, La Victoria, Lince, Los Olivos, Magdalena del Mar, Miraflores, Pueblo Libre, San Borja, San Isidro, San Miguel, Santiago de Surco, Surquillo.
3. **Variables Físicas Estrictas:**
   * `superficieTotalM2`: Numérico decimal $> 0$.
   * `superficieTechadaM2`: Numérico decimal $> 0$.
   * `habitaciones`: Entero $\ge 0$.
   * `banos`: Decimal $\ge 1.0$ con paso `0.5` para admitir medios baños (1.0, 1.5, 2.0, 2.5, etc.).
   * `estacionamientos`: Entero $\ge 0$.
   * `antiguedadAnios`: Entero $\ge 0$ (0 si es estreno).
   * `esEstreno`: Checkbox / switch booleano.
   * `vistaExterior`: Checkbox / switch booleano (Marcado = Vista a calle/parque; Desmarcado = Interior/pozo de luz).
   * *Regla para el Desarrollador:* **NO agregar campos como ascensor, piscina, seguridad o gimnasio.** El modelo BCRP no los contempla y solo añadirían fricción.
4. **Geolocalización con Google Maps:**
   * Input de dirección conectado con `google.maps.places.Autocomplete`.
   * Mapa interactivo centrado en Lima: al seleccionar una dirección o arrastrar el pin (`marker.addListener('dragend')`), se capturan automáticamente `latitud` y `longitud`.
5. **Carga de Imágenes de Referencia:**
   * Input de archivo (`accept="image/*"`) con previsualización para cargar al menos 1 fotografía.
6. **Selector de Acción:**
   * Checkbox: *"¿Guardar simultáneamente este inmueble en mi cartera comercial?"* (si está marcado, crea el registro en `msInmuebles`).

#### B. Endpoint y Flujo de Comunicación
* **Endpoint:** `POST /api/v1/predicciones/estimar`
* **Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body enviado por Angular:**
  ```json
  {
    "titulo": "Departamento moderno en Miraflores",
    "tipoInmueble": "DEPARTAMENTO",
    "distritoId": 16,
    "distritoNombre": "Miraflores",
    "superficieTotalM2": 110.0,
    "superficieTechadaM2": 110.0,
    "habitaciones": 3,
    "banos": 2.5,
    "estacionamientos": 2,
    "piso": 4,
    "antiguedadAnios": 2,
    "esEstreno": false,
    "vistaExterior": true,
    "latitud": -12.1215,
    "longitud": -77.0298,
    "direccion": "Calle Alcanfores 450",
    "tipoPrediccion": "VENTA",
    "guardarEnCartera": true,
    "precioPublicado": 190000.00,
    "imagenesUrls": ["https://storage.valuo.pe/temp/foto1.jpg"]
  }
  ```

#### C. Lógica de Negocio y Flujo de Inferencia (`msPrediccion` $\rightarrow$ `mlService`)

1. **Llamada Interna a `mlService` (Python FastAPI en puerto 8000):**
   `msPrediccion` recibe los datos del formulario Angular, los valida y despacha la petición HTTP interna con el siguiente payload estandarizado:
   * **Endpoint:** `POST http://localhost:8000/predict/sale` (o `/predict/rental` según el tipo)
   * **Payload JSON con los datos del inmueble:**
     ```json
     {
       "distrito": "Magdalena del Mar",
       "superficie": 70.0,
       "habitaciones": 2,
       "banios": 1.5,
       "garajes": 1,
       "piso": 4,
       "antiguedad": 5,
       "vista_exterior": true
     }
     ```

2. **Respuesta Exacta de `mlService` para VENTA:**
   El motor analítico (Python FastAPI) ejecuta el modelo `xgboost_venta_v3` y calcula las contribuciones locales con `TreeExplainer`, respondiendo:
   ```json
   {
     "distrito": "San Miguel",
     "superficie_m2": 85.0,
     "prediccion": {
       "soles_constantes": 332450.75,
       "soles_nominales": 562435.18,
       "precio_m2_constantes": 3911.19,
       "precio_m2_nominales": 6616.88
     },
     "intervalo_confianza": {
       "inferior_nominales": 477788.69,
       "superior_nominales": 647081.67,
       "inferior_constantes": 282416.91,
       "superior_constantes": 382484.59,
       "mape_pct": 15.05
     },
     "explicabilidad": {
       "valor_base_constantes": 432295.69,
       "contribuciones": [
         {
           "feature": "Superficie",
           "label": "Superficie (85.0 m²)",
           "shap_value": 0.384215,
           "valor_feature": 85.0,
           "impacto": "positivo"
         },
         {
           "feature": "distrito_encoded",
           "label": "Distrito (San Miguel)",
           "shap_value": -0.198421,
           "valor_feature": 287982.23,
           "impacto": "negativo"
         },
         {
           "feature": "Garajes",
           "label": "Cocheras (1)",
           "shap_value": 0.084125,
           "valor_feature": 1.0,
           "impacto": "positivo"
         },
         {
           "feature": "nse_ab_pct",
           "label": "Nivel Socioeconómico AB (45.2%)",
           "shap_value": 0.05231,
           "valor_feature": 45.2,
           "impacto": "positivo"
         },
         {
           "feature": "Antiguedad",
           "label": "Antigüedad (6 años)",
           "shap_value": -0.04128,
           "valor_feature": 6.0,
           "impacto": "negativo"
         },
         {
           "feature": "Banos",
           "label": "Baños (2.5)",
           "shap_value": 0.03891,
           "valor_feature": 2.5,
           "impacto": "positivo"
         },
         {
           "feature": "Piso",
           "label": "Piso (5)",
           "shap_value": 0.01542,
           "valor_feature": 5.0,
           "impacto": "positivo"
         },
         {
           "feature": "vista_exterior",
           "label": "Vista Exterior",
           "shap_value": 0.01205,
           "valor_feature": 1.0,
           "impacto": "positivo"
         },
         {
           "feature": "dist_comisaria_km",
           "label": "Distancia a Comisaría (0.45 km)",
           "shap_value": 0.00841,
           "valor_feature": 0.45,
           "impacto": "positivo"
         },
         {
           "feature": "tasa_delitos_pc",
           "label": "Seguridad distrital",
           "shap_value": -0.00715,
           "valor_feature": 12.4,
           "impacto": "negativo"
         }
       ],
       "n_features_mostradas": 10,
       "n_features_total": 33,
       "nota": "Los shap_values están en escala logarítmica (transformación log1p del target). Para impacto en soles nominales: exp(shap_value) × (IPC/100)."
     },
     "modelo": {
       "version": "xgboost_venta_v3",
       "mape_test": 15.05,
       "r2_test": 0.7468,
       "ipc_factor": 169.18,
       "periodo_ipc": "2026-Q1",
       "train_periodo": "2016-2023",
       "test_periodo": "2024-2025"
     }
   }
   ```

3. **Respuesta Exacta de `mlService` para ALQUILER:**
   El modelo `xgboost_alquiler_v3` proyecta el canon mensual en soles constantes y nominales:
   ```json
   {
     "distrito": "Magdalena del Mar",
     "superficie_m2": 70.0,
     "prediccion": {
       "soles_constantes": 1395.20,
       "soles_nominales": 2360.40,
       "precio_m2_constantes": 19.93,
       "precio_m2_nominales": 33.72
     },
     "intervalo_confianza": {
       "inferior_nominales": 2035.13,
       "superior_nominales": 2685.67,
       "inferior_constantes": 1202.94,
       "superior_constantes": 1587.46,
       "mape_pct": 13.78
     },
     "explicabilidad": {
       "valor_base_constantes": 1641.51,
       "contribuciones": [
         {
           "feature": "distrito_encoded",
           "label": "Distrito (Magdalena del Mar)",
           "shap_value": 0.184512,
           "valor_feature": 26.42,
           "impacto": "positivo"
         },
         {
           "feature": "Superficie",
           "label": "Superficie (70.0 m²)",
           "shap_value": 0.142301,
           "valor_feature": 70.0,
           "impacto": "positivo"
         },
         {
           "feature": "Garajes",
           "label": "Cocheras (1)",
           "shap_value": 0.078921,
           "valor_feature": 1.0,
           "impacto": "positivo"
         },
         {
           "feature": "Banos",
           "label": "Baños (1.5)",
           "shap_value": 0.021430,
           "valor_feature": 1.5,
           "impacto": "positivo"
         }
       ],
       "n_features_mostradas": 10,
       "n_features_total": 33,
       "nota": "Los shap_values están en escala logarítmica (transformación log1p del target)."
     },
     "modelo": {
       "version": "xgboost_alquiler_v3",
       "mape_test": 13.78,
       "r2_test": 0.6784,
       "ipc_factor": 169.18,
       "periodo_ipc": "2026-Q1",
       "train_periodo": "2016-2023",
       "test_periodo": "2024-2025"
     }
   }
   ```

4. **Persistencia con Spring Data JPA en `msPrediccion`:**
   * Si `guardarEnCartera == true`, llama a `msInmuebles` para registrar la entidad y obtiene el `inmueble_id`.
   * Almacena en la entidad `Prediccion`:
     * Valores monetarios nominales (`soles_nominales`, equivalente en dólares con TC referencial BCRP).
     * Límites inferior y superior del `intervalo_confianza`.
     * Factor IPC y período del modelo.
     * El nodo completo de `explicabilidad.contribuciones` se guarda directamente en la columna `JSONB` de la entidad para auditoría y renderizado en PDF.
   * Invoca `prediccionRepository.save(prediccion)`.

5. **Respuesta hacia Angular (`201 Created`):**
   Retorna `{ "prediccionId": "pred-uuid-1234" }` para que Angular redirija a la pantalla de resultados `/predicciones/resultado/pred-uuid-1234`.

---

### 5.2. Vista 2: Resultados de la Estimación y Explicabilidad (`/predicciones/resultado/:id`)

#### A. Componente Frontend: `ResultadoEstimacionComponent`
Diseñado para la interpretación comercial por parte del agente:

1. **Tarjeta Principal de Valor de Mercado:**
   * Muestra el **Precio Estimado Sugerido**:
     * En **Soles Nominales:** `S/ 562,435.18` (precio actual de mercado).
     * En **Soles Constantes:** `S/ 332,450.75` (descontada la inflación acumulada).
     * Equivalente en **Dólares Americanos:** `$ 149,982.71 USD` (calculado con TC oficial BCRP).
   * **Valor por Metro Cuadrado:** `S/ 6,616.88 / m²` nominales (`S/ 3,911.19 / m²` constantes).
2. **Barra Gráfica de Rango de Negociación (Intervalo de Confianza):**
   * Visualiza gráficamente la banda empírica basada en el MAPE ($15.05\%$ en venta o $13.78\%$ en alquiler):
     * Límite inferior: `S/ 477,788.69`
     * Valor central sugerido: `S/ 562,435.18`
     * Límite superior: `S/ 647,081.67`
3. **Sección de Estimación Cruzada (Venta $\leftrightarrow$ Alquiler):**
   * Si la estimación inicial fue de **Venta**:
     * Botón: **[Estimar Alquiler Mensual Sugerido]**.
     * Al hacer clic, `msPrediccion` invoca `/predict/rental` enviando las mismas características físicas y muestra el canon mensual proyectado (ej. `S/ 2,360.40 / mes`).
   * Si fue de **Alquiler**: Muestra el botón para estimar el precio de venta.
4. **Gráfico SHAP de Factores Determinantes (`ShapChartComponent`):**
   * Renderiza el array `explicabilidad.contribuciones` mediante un gráfico de barras horizontales (Chart.js):
     * **Eje Y:** La etiqueta legible (`label`), por ejemplo: *"Superficie (85.0 m²)"*, *"Distrito (San Miguel)"*, *"Cocheras (1)"*, *"Antigüedad (6 años)"*.
     * **Barras Verdes (`impacto === 'positivo'`):** Factores que suman al precio (barra hacia la derecha).
     * **Barras Rojas (`impacto === 'negativo'`):** Factores que disminuyen el precio (barra hacia la izquierda).
     * **Tooltip:** Muestra el valor de la característica (`valor_feature`) y su magnitud relativa de contribución.
5. **Botones de Continuidad del Flujo:**
   * **[Ver en Mi Cartera]:** Redirige a `/inmuebles`.
   * **[Calcular Análisis de Rentabilidad]:** Redirige a `/rentabilidad/nuevo` pre-cargando el inmueble y los valores de venta y alquiler.

#### B. Endpoints y Lógica de Negocio JPA

##### 1. Consulta del Detalle de la Estimación
* **Endpoint:** `GET /api/v1/predicciones/{id}`
* **Headers:** `Authorization: Bearer <token>`
* **Lógica Backend JPA:**
  * Recupera la entidad mediante `prediccionRepository.findById(id)`.
  * Retorna el DTO con los bloques `prediccion`, `intervalo_confianza`, `explicabilidad` y `modelo`.
* **Binding Frontend Angular:**
  ```typescript
  // Preparar datasets para Chart.js
  const contribuciones = response.explicabilidad.contribuciones;
  this.chartLabels = contribuciones.map(c => c.label);
  this.chartData = contribuciones.map(c => c.shap_value);
  this.chartColors = contribuciones.map(c => c.impacto === 'positivo' ? '#10B981' : '#EF4444');
  ```

##### 2. Disparo de Estimación Cruzada de Alquiler
* **Endpoint:** `POST /api/v1/predicciones/{id}/estimar-alquiler-cruzado`
* **Lógica Backend JPA:**
  1. Recupera la entidad `Prediccion` original por ID para obtener el snapshot del inmueble.
  2. Llama a `http://localhost:8000/predict/rental` con el JSON de características del inmueble.
  3. Guarda una nueva entidad en `prediccionRepository` con `tipoPrediccion = TipoPrediccion.ALQUILER`.
  4. Retorna el resultado estructurado de alquiler para refrescar la vista en Angular.
