# Parking 2027 API

API REST de parqueadero (Spring Boot 4.1.1, Java 25, MySQL). Gestiona espacios, tarifas, entradas/salidas de vehiculos y cobro por **hora y fraccion**.

## Requisitos
- JDK 25
- MySQL 8+ en `localhost:3306` con la base `parqueadero_db` creada (`CREATE DATABASE parqueadero_db;`)
- Credenciales en `src/main/resources/application.properties` (por defecto `root`/`root`)

## Ejecutar
```bash
./mvnw spring-boot:run
```
Con la base vacia, `ddl-auto=update` crea todas las tablas y, al arrancar, se siembra una tarifa vigente por tipo de vehiculo (ver *Tarifas*). No hace falta ningun script SQL.

## Estructura
```
config/      TarifaProperties (valores iniciales), TarifaInicializador (siembra), ZonaHoraria
controller/  Espacio, Registro, Vehiculo, Tarifa
dto/         Requests/Responses (las entidades no se exponen)
exception/   ApiException + GlobalExceptionHandler (errores JSON uniformes)
model/       Entidades (Tarifa nueva)
repository/  Repositorios Spring Data JPA
service/     EspacioService, RegistroService, TarifaService
```

## Tarifas (tabla `tarifas`)
Cada fila: tipo, valor por hora, valor por fraccion, minutos de la fraccion y rango de vigencia (`vigenteDesde` / `vigenteHasta`).
- La tarifa **vigente** de un tipo es la que tiene `vigenteHasta = null`.
- Las tarifas **no se editan ni se borran**: `POST /api/tarifas` crea una nueva y cierra la anterior, conservando el historial.
- Al arrancar, si un tipo no tiene tarifa vigente, se siembra con los valores `parking.tarifa.*` de `application.properties`. Esos valores son solo iniciales.
- Se cobra con la tarifa vigente **en el momento de la salida**. Un vehiculo que ya estaba dentro cuando cambia la tarifa paga la nueva.
- No se permite registrar entradas de un tipo sin tarifa vigente (409).

Reglas de calculo:
1. El tiempo se redondea hacia arriba al minuto.
2. Cada hora completa se cobra a la tarifa de hora.
3. Los minutos sobrantes se cobran por fracciones iniciadas (bloques de `fraccionMinutos`), sin superar nunca el valor de una hora.
4. Minimo cobrable: una fraccion.

Ejemplos (carro: hora 6000, fraccion 1500 cada 15 min):

| Tiempo | Calculo | Total |
|---|---|---|
| 3 min | 1 fraccion | 1.500 |
| 16 min | 2 fracciones | 3.000 |
| 50 min | 4 fracciones = 6.000 (tope: 1 hora) | 6.000 |
| 1 h 20 min | 6.000 + 2 fracciones (3.000) | 9.000 |
| 2 h 01 min | 12.000 + 1 fraccion | 13.500 |

La hora usada es `America/Bogota`. La respuesta de salida y de `/cobro` incluye `cobro.idTarifa` (la tarifa aplicada).

## Endpoints

### Tarifas
| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/tarifas` | Nueva tarifa vigente `{ "tipo": "CARRO", "valorHora": 7000, "valorFraccion": 1800, "fraccionMinutos": 15 }` |
| GET | `/api/tarifas?tipo=&vigentes=` | Listar / historial (`vigentes=true` solo las activas) |
| GET | `/api/tarifas/{id}` | Obtener |

### Espacios
| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/espacios` | Crear espacio `{ "numero": 1, "tipo": "CARRO" }` |
| POST | `/api/espacios/lote` | Crear varios `{ "desde": 1, "hasta": 10, "tipo": "CARRO" }` (max 500) |
| GET | `/api/espacios?tipo=&estado=` | Listar (filtros opcionales) |
| GET | `/api/espacios/{id}` | Obtener |
| PUT | `/api/espacios/{id}` | Modificar numero/tipo (no si esta ocupado) |
| DELETE | `/api/espacios/{id}` | Eliminar (no si esta ocupado o tiene historial) |

### Registros
| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/registros/entrada` | Registrar entrada |
| POST | `/api/registros/placa/{placa}/salida` | **Registrar salida por placa** y cobrar |
| GET | `/api/registros/placa/{placa}/cobro` | Ver cuanto costaria salir ahora, por placa (no cierra el registro) |
| GET | `/api/registros/placa/{placa}` | Registro activo de una placa |
| POST | `/api/registros/{id}/salida` | Registrar salida por id de registro |
| GET | `/api/registros/{id}/cobro` | Ver cobro por id de registro |
| GET | `/api/registros/{id}` | Obtener registro por id |
| GET | `/api/registros?activos=true` | Listar (`true` dentro, `false` finalizados, sin parametro todos) |

Entrada:
```json
{ "placa": "ABC123", "tipo": "CARRO", "descripcion": "Mazda rojo", "numeroEspacio": 3 }
```
- `placa`: obligatoria para **todos** los tipos (la columna es `NOT NULL` y `UNIQUE`). Para bicicletas use un identificador (serial, codigo). Se normaliza (mayusculas, sin guiones/espacios), 5 a 7 caracteres alfanumericos.
- `numeroEspacio`: opcional. Si falta, se asigna el espacio libre de menor numero del mismo tipo.

Las rutas por placa actuan sobre el registro **activo** (sin salida) de esa placa. Si la placa no esta dentro del parqueadero responden `404`, tambien al repetir una salida. La placa se normaliza igual que en la entrada (`abc-123` equivale a `ABC123`).

### Vehiculos
| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/vehiculos` | Listar vehiculos conocidos |
| GET | `/api/vehiculos/{placa}/registros` | Historial de una placa |

## Errores
```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "El espacio 3 esta ocupado", "detalles": [] }
```
`400` datos invalidos · `404` no encontrado (incluye placa sin registro activo) · `409` conflicto (placa ya dentro, placa de otro tipo, espacio ocupado o de otro tipo, sin espacios libres, sin tarifa vigente, salida ya registrada, placa duplicada por concurrencia).

## Pruebas con Postman
1. Importar `postman/Parking2027.postman_collection.json` (variable `baseUrl` = `http://localhost:8080`).
2. Orden sugerido (carpeta *Flujo por placa*): *Tarifas vigentes* -> *Crear espacios en lote* -> *Entrada carro* -> *Ver registro activo por placa* -> *Ver cobro por placa* -> *Registrar salida por placa* -> *Salida repetida (404)*.
3. La variable de coleccion `placa` (por defecto `ABC123`) se usa en las rutas y en el cuerpo de la entrada; cambiela para probar otros vehiculos. La carpeta *Flujo por id* repite el ciclo con `idRegistro`, que la entrada guarda automaticamente.
4. Para ver saltos de fraccion sin esperar horas, cree una tarifa con `fraccionMinutos: 1` y consulte `/cobro` cada minuto.

## Concurrencia
- Las entradas de una misma placa se serializan bloqueando la fila del vehiculo; una placa nueva repetida en paralelo la rechaza el `UNIQUE` (409).
- Las salidas bloquean el registro (por id o por placa), por lo que una salida doble no cobra dos veces.
- MySQL no permite un indice unico parcial ("una sola fila activa por placa") sin cambiar el modelo; esa regla la garantiza el servicio con el bloqueo anterior.

## Limitaciones conocidas
- Sin autenticacion/autorizacion: cualquiera con acceso a la URL puede cambiar tarifas.
- No hay entidad de pagos/tickets: `valorPagado` se fija al registrar la salida.
- Cambiar una tarifa afecta a los vehiculos que ya estan dentro (se cobra con la vigente a la salida).
- Los espacios no tienen historial de cambios de tipo/numero.
