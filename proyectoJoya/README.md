# Cards4Play API

API REST para inventario, compras, boosters y torneos de Cards4Play. Requiere Docker para ejecutarse localmente. Los datos se guardan en JSON en un volumen Docker.

## Ejecutar localmente

Requisitos: Docker Desktop instalado y abierto, con el motor Linux en ejecución.

Desde esta carpeta:

```powershell
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:4567/
```

La respuesta esperada del último comando es similar a `service: Cards4Play API`, `status: ok`. Para ver los registros: `docker compose logs -f cards4play-api`. Para detener el servicio sin borrar los datos: `docker compose down`. El volumen persistente se llama `proyectojoya_cards4play-data` (Docker antepone el nombre de la carpeta de Compose).

La configuración está en `compose.yaml`: publica el puerto 4567, monta el volumen en `/data` y configura `DATA_FILE=/data/cards4play.json` y `TASA_DEFECTO=4000`. Para cambiar la tasa, edita `TASA_DEFECTO` y recrea el contenedor con `docker compose up --build -d`.

También se puede ejecutar manualmente:

```powershell
docker build -t cards4play-api .
docker volume create cards4play-data
docker run --name cards4play -p 4567:4567 -v cards4play-data:/data -e PORT=4567 -e DATA_FILE=/data/cards4play.json -e TASA_DEFECTO=4000 cards4play-api
```

Administrador inicial: `admin` / `admin123`. Para las rutas protegidas usa los encabezados `X-Username` y `X-Password`. Cambia la contraseña inicial antes de exponer el servicio públicamente.

## Configuración y persistencia

- `PORT`: puerto HTTP (por defecto 4567; Render lo configura automáticamente).
- `DATA_FILE`: archivo JSON de persistencia (por defecto `data/cards4play.json`; en Docker, `/data/cards4play.json`).
- `TASA_DEFECTO`: tasa COP por USD usada si falla el servicio externo (por defecto 4000).
- Tasa externa: `https://open.er-api.com/v6/latest/USD`, con timeout de tres segundos. Las respuestas de precios informan `fuenteTasa` como `API` o `DEFECTO`.
- Los JSON permanecen al borrar/recrear el contenedor mientras no se elimine el volumen. `docker compose down -v` sí elimina el volumen y sus datos.

## Autenticación

`POST /auth/login` recibe `{"username":"admin","password":"admin123"}`. Devuelve el rol. Las siguientes peticiones protegidas usan `X-Username` y `X-Password`. Solo el administrador puede registrar clientes mediante `POST /clients`; no hay registro público.

## Endpoints

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| GET | `/` | Público | Estado del servicio |
| POST | `/auth/login` | Público | Verificar credenciales |
| POST, GET | `/clients` | ADMIN | Registrar y consultar clientes |
| POST, GET | `/cards` | ADMIN / autenticado | Registrar cartas y listar con precios COP |
| POST | `/products/sealed` | ADMIN | Registrar producto sellado |
| POST | `/products/accessories` | ADMIN | Registrar accesorio |
| POST | `/sealed`, `/accessories` | ADMIN | Alias de registro |
| GET | `/products` | Autenticado | Consultar sellados y accesorios con precios COP |
| POST, GET | `/boosters` | ADMIN / autenticado | Crear y listar boosters disponibles sin revelar cartas |
| GET | `/boosters/:id` | Autenticado | Consultar booster sin revelar contenido antes de abrirse |
| POST | `/purchases` | CLIENT | Comprar un producto usando `productId` |
| POST | `/boosters/:id/open` | CLIENT | Abrir una vez un booster propio |
| GET | `/purchases` | CLIENT | Historial de compras |
| POST, GET | `/tournaments` | ADMIN / autenticado | Crear y consultar torneos |
| POST | `/tournaments/:id/register` | CLIENT | Inscribirse validando cupos y duplicados |

Los errores se responden como JSON (`{"error":"..."}`), con estados 400, 401, 403, 404, 409 y 500, además de respuestas exitosas 200 y 201. Las rarezas y probabilidades son N 50%, UC 25%, R 15%, SR 7% y UR 3%. Cada booster contiene diez cartas; si no hay cartas de la rareza sorteada, se busca la rareza disponible más cercana, primero inferior y luego superior. Las cartas no se descuentan del inventario.

## Postman

Importa `postman/Cards4Play.postman_collection.json`. La colección define `baseUrl` como `http://localhost:4567`, además de las variables `username` y `password`. Usa las credenciales del usuario para los endpoints protegidos.

## Despliegue en Render

`render.yaml` define un servicio Docker gratuito llamado `cards4play-api` y configura `TASA_DEFECTO=4000`. Para desplegarlo:

1. Sube este proyecto a un repositorio GitHub.
2. En Render, crea un Blueprint y conecta ese repositorio.
3. Confirma la creación del servicio `cards4play-api`.
4. Copia la URL pública que Render asigna y comprueba que `https://<URL>/` devuelva `status: ok`.
5. Pega la URL real en la entrega y en este README.

Render asigna `PORT` automáticamente. El almacenamiento local JSON puede reiniciarse en PaaS, según el enunciado. La URL pública de Render no está configurada en los archivos del proyecto; debe copiarse desde el servicio creado en la cuenta del grupo.

## Entrega y trabajo colaborativo

**Integrantes:** completar con los nombres del grupo.  
**Repositorio GitHub:** completar con la URL del repositorio.  
**URL pública Render:** completar después del despliegue.

Se recomienda una rama por funcionalidad y commits descriptivos de cada integrante. La rama `main` debe contener la versión estable. Documenta en el repositorio cualquier conflicto de fusión resuelto.

Las contraseñas se guardan en JSON sin cifrar; esta implementación es académica y requiere hashes y autenticación más sólida para un sistema real.
