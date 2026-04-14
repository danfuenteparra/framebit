
### Capas

**View (UI)**  
Desarrollada íntegramente con Jetpack Compose.

**ViewModel**  
Gestiona el estado de la UI mediante StateFlow y coordina los casos de uso.

**Repository**  
Abstrae el acceso a datos, combinando fuentes locales (Room) y remotas (Retrofit).

**Data Sources**  
Room para persistencia local y Retrofit para consumo de API.

Se utilizan Coroutines para operaciones asíncronas y Flow para observar cambios reactivos en base de datos.

---

## Interfaz de Usuario

La UI está desarrollada completamente con Jetpack Compose, sin uso de XML.

### Características implementadas

- Material Design 3 con tema personalizado  
- Navegación con Navigation Compose  
- Mínimo 4 pantallas diferenciadas  
- Componentes reutilizables  
- Listas con LazyColumn y LazyRow  
- Formularios con validación básica  
- Estados reactivos mediante StateFlow  

### Pantallas principales

- Login  
- Home / Explorar  
- Búsqueda  
- Detalle  
- Mi Lista / Watchlist  
- Reseñas  

---

## Persistencia Local con Room

Room se utiliza como ORM para almacenamiento local y caché de contenido.

### Entidades

- Movie  
- TvShow  
- WatchlistItem  
- Review  
- Genre  

### Funcionalidades implementadas

- Operaciones CRUD (Insert, Update, Delete, Select)  
- Uso de Flow para observar cambios en la base de datos  
- Queries con @Query  
- Búsqueda y filtrado simple  
- Gestión de estado de visualización (pendiente, vista, favorita)  
- Notas personales  

Room actúa tanto como almacenamiento persistente del usuario como sistema de caché local.

---

## Consumo de API REST

Integración con la API pública de TMDB mediante Retrofit2.

### Endpoints utilizados

- Obtener películas populares  
- Obtener series populares  
- Búsqueda por título  
- Detalle de película o serie  
- Información de reparto  

### Implementación técnica

- Retrofit2  
- Gson para serialización JSON  
- DTOs para transferencia de datos  
- Manejo básico de errores con try-catch  
- Patrón Repository para desacoplar la capa de red  

---

## Autenticación

La autenticación se implementa mediante Auth0.

### Funcionalidades

- Login seguro  
- Manejo básico de tokens  
- Logout  
- Protección de rutas principales  
- Visualización de información básica del usuario autenticado  

Las pantallas principales solo son accesibles tras autenticación válida.

---

## Tecnologías Utilizadas

- Kotlin  
- Jetpack Compose  
- Navigation Compose  
- ViewModel  
- StateFlow  
- Coroutines  
- Room  
- Retrofit2  
- Gson  
- Auth0  

---

## Funcionalidades Core

### Explorar
- Películas populares  
- Series populares  
- Mejor valoradas  

### Búsqueda
- Por título  
- Por género  

### Detalle
- Sinopsis  
- Reparto  
- Valoraciones  
- Información general  

### Mi Lista
- Pendientes  
- Vistas  
- Favoritas  

### Reseñas
- Crear reseñas personales  
- Consultar reseñas guardadas  

---

## Conclusión

MovieBox integra UI moderna con Compose, arquitectura MVVM correctamente estructurada, persistencia local con Room, consumo de API REST con Retrofit y autenticación segura con Auth0.

El proyecto prioriza estabilidad, claridad estructural y cumplimiento completo de los requisitos técnicos obligatorios.
