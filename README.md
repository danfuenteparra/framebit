# Framebit

Aplicación social Android para el seguimiento y reseña de películas, series y videojuegos.

Framebit unifica en una sola aplicación nativa Android el seguimiento de los tres principales medios de ocio audiovisual, ofreciendo catálogos integrados, listas personales, un sistema completo de reseñas con interacción social y mensajería directa entre usuarios.

Proyecto Intermodular del Ciclo de Grado Superior de Desarrollo de Aplicaciones Multiplataforma — IES Isidra de Guzmán, curso 2025/2026.

Repositorio: https://github.com/danfuenteparra/framebit

---

## Arquitectura

Framebit sigue el patrón **MVVM** con **Repository**, recomendado por la guía oficial de arquitectura de Android.

### Capas

**View (UI)**
Desarrollada íntegramente con Jetpack Compose y Material 3.

**ViewModel**
Gestiona el estado de la UI mediante StateFlow y coordina la lógica de presentación.

**Repository**
Abstrae el acceso a datos, combinando fuentes locales (Room), remotas (Retrofit) y servicios en la nube (Cloud Firestore).

**Data Sources**
Room para persistencia local, Retrofit para consumo de las APIs de TMDB y RAWG, FirestoreService para acceso a Cloud Firestore y AuthManager para la gestión unificada de Auth0 y Firebase Authentication.

Se utilizan Coroutines para operaciones asíncronas y Flow / StateFlow para observar cambios reactivos.

---

## Interfaz de Usuario

La UI está desarrollada completamente con Jetpack Compose, sin uso de XML.

### Características implementadas

- Material Design 3 con tema personalizado
- Navegación con Navigation Compose
- Más de veinte pantallas diferenciadas
- Componentes reutilizables
- Listas con LazyColumn y LazyRow
- Formularios con validación
- Estados reactivos mediante StateFlow
- Diálogos personalizados para reseñas, compartir y búsqueda dentro de chat

### Pantallas principales

- Bienvenida y autenticación (Auth0 / correo y contraseña)
- Catálogos de Películas, Series y Juegos
- Búsqueda de contenido y de usuarios
- Pantalla de detalle (películas, series, juegos)
- Mi Lista (pendientes, favoritos, vistos/jugados)
- Perfil propio y ajeno con Top 3
- Editar perfil
- Reseña detallada con comentarios y likes
- Feed de actividad social
- Bandeja de entrada y conversaciones
- Gestión de usuarios bloqueados

---

## Persistencia Local con Room

Room se utiliza como ORM para almacenamiento local y soporte offline.

### Entidades

- MovieEntity
- TvShowEntity
- GameEntity
- ReviewEntity
- UserEntity
- TopItemEntity

### Funcionalidades implementadas

- Operaciones CRUD (Insert, Update, Delete, Select)
- Uso de Flow para observar cambios en la base de datos
- Queries con `@Query`
- Búsqueda y filtrado local
- Gestión de estado por contenido (watchlist, favorito, visto/jugado)
- Caché local del Top 3 con sincronización bidireccional contra Firestore

Room actúa como almacenamiento persistente del usuario para "Mi Lista" y como base estructural para futuras ampliaciones del soporte offline.

---

## Backend en la nube con Cloud Firestore

Cloud Firestore se utiliza como base de datos principal para los datos sociales y sincronizados.

### Colecciones principales

- `users/{userId}` con subcolecciones `following`, `followers`, `library`, `reviews`, `top_items`, `blocks`
- `chats/{chatId}` con subcolección `messages`

### Funcionalidades implementadas

- Reglas de seguridad declarativas
- Transacciones atómicas para seguimiento y bloqueo
- Consultas `collectionGroup` para reseñas y bloqueos cruzados
- Snapshot listeners para mensajería en tiempo real
- Caché persistente local activada para acceso offline a los datos sociales

---

## Consumo de APIs REST

Integración con las APIs públicas de TMDB (películas y series) y RAWG (videojuegos) mediante Retrofit 2.

### Endpoints utilizados

- Catálogos: populares, mejor valorados y novedades
- Búsqueda por título
- Detalle de película, serie o juego
- Información de reparto y créditos
- Géneros y filtrado por género
- Vídeos (trailers)

### Implementación técnica

- Retrofit 2
- OkHttp con logging interceptor
- Gson para serialización JSON
- DTOs específicos por endpoint
- Manejo de errores con `Result` y try-catch
- Patrón Repository para desacoplar la capa de red

---

## Autenticación

La autenticación admite dos vías que conviven en la misma pantalla de bienvenida.

### Auth0

- Inicio de sesión con Google mediante OAuth 2.0 / OpenID Connect
- Flujo basado en Custom Tabs del navegador del sistema
- Tras el login, se abre una sesión anónima de Firebase Authentication para que las reglas de Firestore reconozcan al usuario

### Firebase Authentication

- Registro e inicio de sesión con correo electrónico y contraseña
- Validación de duplicados frente a cuentas de Auth0 ya existentes

### Sesión persistente

- La sesión se conserva entre cierres de la aplicación mediante SharedPreferences
- Acceso a la app sin volver a iniciar sesión, incluso sin conexión

---

## Tecnologías Utilizadas

- Kotlin
- Jetpack Compose · Material 3 · Navigation Compose
- ViewModel · StateFlow · Coroutines
- Hilt (inyección de dependencias)
- Room (persistencia local)
- Cloud Firestore (base de datos en la nube)
- Firebase Authentication
- Auth0 (OAuth 2.0 / OIDC)
- Retrofit 2 · OkHttp · Gson
- Coil (carga de imágenes)
- TMDB API · RAWG API

---

## Funcionalidades Core

### Explorar
- Películas populares, mejor valoradas y novedades
- Series populares, mejor valoradas y novedades
- Videojuegos populares, mejor valorados y novedades

### Búsqueda
- Por título dentro de cada catálogo
- Búsqueda global de usuarios por nombre

### Detalle
- Sinopsis, reparto, valoraciones y metadatos
- Reseñas públicas de la comunidad ordenadas por likes

### Mi Lista
- Pendientes, favoritos y vistos/jugados por tipo de contenido
- Acceso offline garantizado mediante Room

### Reseñas
- Valoración numérica obligatoria de 0 a 5
- Comentario libre opcional
- Likes y comentarios sobre reseñas de la comunidad

### Sistema social
- Perfiles públicos con Top 3 editable de los tres medios
- Seguidores y seguidos con contadores agregados
- Feed cronológico de actividad de los perfiles seguidos
- Bloqueo bidireccional con ocultación mutua

### Mensajería directa
- Chats privados entre usuarios con seguimiento mutuo
- Mensajes de texto, tarjetas de contenido y reseñas compartidas
- Lectura en tiempo real mediante snapshot listeners

---

## Funcionamiento offline

Tras al menos un uso con conexión, la aplicación funciona sin internet:

- Acceso directo sin pasar por la pantalla de login
- "Mi Lista" completamente operativa desde Room
- Perfil con Top 3, contadores y secciones agrupadas desde la caché persistente de Cloud Firestore
- Consulta de conversaciones y reseñas previamente abiertas

Las acciones que requieren conexión activa son la búsqueda de contenido nuevo, el envío de mensajes, la publicación de reseñas y la interacción con usuarios no consultados previamente.

---

## Conclusión

Framebit integra una UI moderna con Jetpack Compose, una arquitectura MVVM correctamente estructurada por capas, persistencia híbrida entre Room y Cloud Firestore, consumo de dos APIs REST externas y un sistema de autenticación dual con Auth0 y Firebase Authentication.

El proyecto prioriza la estabilidad, la claridad estructural y la cobertura completa de los requisitos funcionales y no funcionales planteados, ofreciendo una experiencia social fluida sobre los tres medios soportados.
