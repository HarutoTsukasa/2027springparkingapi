# Parking 2027 API

API REST de parqueadero (Spring Boot 4.1.1, Java 25, MySQL). Gestiona espacios, tarifas, entradas y salidas de vehiculos y el cobro por **hora y fraccion**.

## Contenido
1. [Puesta en marcha](#puesta-en-marcha)
2. [Datos iniciales](#datos-iniciales)
3. [Reglas de negocio](#reglas-de-negocio)
4. [Convenciones de la API](#convenciones-de-la-api)
5. [Resumen de endpoints](#resumen-de-endpoints)
6. [Documentacion de endpoints](#documentacion-de-endpoints)
7. [Modelos de datos](#modelos-de-datos)
8. [Flujo tipico de consumo](#flujo-tipico-de-consumo)
9. [Pruebas con Postman](#pruebas-con-postman)
10. [Estructura del proyecto](#estructura-del-proyecto)
11. [Concurrencia y limitaciones](#concurrencia-y-limitaciones)

## Puesta en marcha

Requisitos: **JDK 25** y **MySQL 8+** en `localhost:3306` con usuario `root` / clave `root` (ajustable en `src/main/resources/application.properties`).

```bash
./mvnw spring-boot:run
```

- La base `parqueadero_db_2027` se crea sola (`createDatabaseIfNotExist=true`) y `ddl-auto=update` crea las tablas. No hace falta ningun script SQL.
- La API queda en `http://localhost:8080`.

## Datos iniciales

En el primer arranque con la base vacia la API siembra automaticamente:

**Espacios (50)** - solo si la tabla `espacios_parqueo` esta vacia:

| Tipo | Cantidad | Numeros |
|---|---|---|
| CARRO | 25 | 1 a 25 |
| MOTO | 20 | 26 a 45 |
| BICICLETA | 5 | 46 a 50 |

**Tarifas (COP)** - una vigente por tipo, solo si el tipo no tiene tarifa vigente:

| Tipo | Valor hora | Valor fraccion | Minutos por fraccion |
|---|---|---|---|
| CARRO | 6.000 | 1.500 | 15 |
| MOTO | 3.000 | 750 | 15 |
| BICICLETA | 1.000 | 250 | 15 |

Las cantidades y valores iniciales se cambian en `application.properties` (`parking.espacios.iniciales.*` y `parking.tarifa.*`) **antes del primer arranque**. Despues, las tarifas se administran por `/api/tarifas` y los espacios por `/api/espacios`.

## Reglas de negocio

### Entrada
- La **placa es obligatoria para todos los tipos** (5 a 7 caracteres alfanumericos). Para bicicletas use un identificador (serial, codigo).
- Un vehiculo no puede entrar si ya esta dentro.
- Una placa pertenece a un unico tipo de vehiculo.
- El espacio debe ser del mismo tipo que el vehiculo. Si no se indica `numeroEspacio`, se asigna el libre de menor numero.
- Solo se permite entrar si el tipo tiene una tarifa vigente.

### Cobro por hora y fraccion
Cada tarifa define `valorHora`, `valorFraccion` y `fraccionMinutos`.
1. El tiempo se redondea hacia arriba al minuto.
2. Cada hora completa se cobra a `valorHora`.
3. Los minutos sobrantes se cobran por **fracciones iniciadas** (bloques de `fraccionMinutos`), sin superar nunca `valorHora`.
4. El minimo cobrable es una fraccion.
5. Se usa la tarifa vigente **en el momento de la salida**.

Ejemplos con CARRO (hora 6.000, fraccion 1.500 cada 15 min):

| Tiempo | Calculo | Total |
|---|---|---|
| 3 min | 1 fraccion | 1.500 |
| 16 min | 2 fracciones | 3.000 |
| 50 min | 4 fracciones = 6.000 (tope: una hora) | 6.000 |
| 1 h 20 min | 6.000 + 2 fracciones (3.000) | 9.000 |
| 2 h 01 min | 12.000 + 1 fraccion | 13.500 |

### Tarifas
Forman un historial: **no se editan ni se eliminan**. `POST /api/tarifas` crea la nueva vigente y cierra la anterior. La respuesta del cobro incluye `cobro.idTarifa` para saber que tarifa se aplico.

### Salida
Se puede registrar por **id de registro** o por **placa**. Fija `fechaSalida`, guarda `valorPagado` y libera el espacio. Una salida repetida por id responde `409`; por placa responde `404` (ya no hay registro activo).

## Convenciones de la API

| Aspecto | Valor |
|---|---|
| URL base | `http://localhost:8080` |
| Formato | JSON (`Content-Type: application/json` en peticiones con cuerpo) |
| Fechas | ISO-8601 sin zona, hora de Colombia (`America/Bogota`), ej. `2026-10-05T15:30:00` |
| Dinero | Pesos colombianos (COP), sin decimales significativos |
| Enumeraciones | `tipo`: `CARRO`, `MOTO`, `BICICLETA` - `estado`: `LIBRE`, `OCUPADO` |
| Autenticacion | Ninguna |

**Formato de error** (todos los errores usan el mismo cuerpo):
```json
{
  "timestamp": "2026-10-05T15:31:00",
  "status": 409,
  "error": "Conflict",
  "mensaje": "El espacio 1 esta ocupado",
  "detalles": []
}
```
`detalles` lista los errores por campo en las validaciones (`400`). Codigos usados: `200` OK, `201` creado, `204` sin contenido, `400` datos invalidos, `404` no encontrado, `409` conflicto con el estado actual.

## Resumen de endpoints

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/tarifas` | Listar tarifas (historial) |
| GET | `/api/tarifas/{id}` | Consultar una tarifa por id |
| POST | `/api/tarifas` | Crear nueva tarifa vigente |
| GET | `/api/espacios` | Listar espacios |
| GET | `/api/espacios/{id}` | Consultar un espacio por id |
| POST | `/api/espacios` | Crear un espacio |
| POST | `/api/espacios/lote` | Crear espacios en lote |
| PUT | `/api/espacios/{id}` | Modificar un espacio |
| DELETE | `/api/espacios/{id}` | Eliminar un espacio |
| POST | `/api/registros/entrada` | Registrar entrada de un vehiculo |
| GET | `/api/registros` | Listar registros |
| GET | `/api/registros/{id}` | Consultar un registro por id |
| GET | `/api/registros/{id}/cobro` | Consultar cobro por id de registro |
| POST | `/api/registros/{id}/salida` | Registrar salida por id de registro |
| GET | `/api/registros/placa/{placa}` | Consultar registro activo por placa |
| GET | `/api/registros/placa/{placa}/cobro` | Consultar cobro por placa |
| POST | `/api/registros/placa/{placa}/salida` | Registrar salida por placa |
| GET | `/api/vehiculos` | Listar vehiculos |
| GET | `/api/vehiculos/{placa}/registros` | Historial de una placa |

## Documentacion de endpoints

En los ejemplos `curl` se usan ids y placas de muestra; reemplacelos por los suyos.

### Tarifas

#### Listar tarifas (historial)
`GET /api/tarifas`

Devuelve las tarifas ordenadas por tipo y por inicio de vigencia descendente. La tarifa **vigente** de un tipo es la que tiene `vigenteHasta = null` (`vigente: true`).

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `tipo` | query | string (CARRO, MOTO, BICICLETA) | No | Filtra por tipo de vehiculo. |
| `vigentes` | query | boolean | No | `true`: solo las vigentes. `false`: solo las historicas. Sin parametro: todas. |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/tarifas"
```

**Respuesta exitosa: `200`**

```json
[
  {
    "idTarifa": 1,
    "tipo": "CARRO",
    "valorHora": 6000,
    "valorFraccion": 1500,
    "fraccionMinutos": 15,
    "vigenteDesde": "2026-10-05T15:30:00",
    "vigenteHasta": null,
    "vigente": true
  },
  {
    "idTarifa": 1,
    "tipo": "CARRO",
    "valorHora": 6000,
    "valorFraccion": 1500,
    "fraccionMinutos": 15,
    "vigenteDesde": "2026-10-05T15:30:00",
    "vigenteHasta": "2026-10-06T08:00:00",
    "vigente": false
  }
]
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | Un filtro tiene un valor invalido (ej. `tipo=AVION`). | Valor invalido para el parametro 'tipo' |

#### Consultar una tarifa por id
`GET /api/tarifas/{id}`

Util para ver la tarifa exacta que se aplico en un cobro (`cobro.idTarifa`).

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador de la tarifa (`idTarifa`). |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/tarifas/1"
```

**Respuesta exitosa: `200`**

```json
{
  "idTarifa": 1,
  "tipo": "CARRO",
  "valorHora": 6000,
  "valorFraccion": 1500,
  "fraccionMinutos": 15,
  "vigenteDesde": "2026-10-05T15:30:00",
  "vigenteHasta": null,
  "vigente": true
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe una tarifa con ese id. | Tarifa 99 no encontrada |

#### Crear nueva tarifa vigente
`POST /api/tarifas`

Crea la nueva tarifa vigente del tipo indicado y **cierra la anterior** (su `vigenteHasta` pasa a ser el momento actual). Las tarifas no se editan ni se eliminan para conservar el historial. Se cobra con la tarifa vigente en el momento de la **salida**, por lo que los vehiculos que ya estan dentro pagan la nueva.

**Cuerpo (JSON)**

| Campo | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `tipo` | string (CARRO, MOTO, BICICLETA) | Si | Tipo de vehiculo al que aplica. |
| `valorHora` | entero >= 0 | Si | Pesos COP por hora completa. |
| `valorFraccion` | entero >= 0 | Si | Pesos COP por cada fraccion iniciada del tiempo sobrante (nunca se cobra mas que `valorHora`). |
| `fraccionMinutos` | entero 1 a 60 | Si | Duracion en minutos de cada fraccion. |

```json
{
  "tipo": "CARRO",
  "valorHora": 7000,
  "valorFraccion": 1800,
  "fraccionMinutos": 15
}
```

**Ejemplo**

```bash
curl -X POST "http://localhost:8080/api/tarifas" \
  -H "Content-Type: application/json" \
  -d '{"tipo": "CARRO", "valorHora": 7000, "valorFraccion": 1800, "fraccionMinutos": 15}'
```

**Respuesta exitosa: `201`**

```json
{
  "idTarifa": 4,
  "tipo": "CARRO",
  "valorHora": 7000,
  "valorFraccion": 1800,
  "fraccionMinutos": 15,
  "vigenteDesde": "2026-10-05T15:30:00",
  "vigenteHasta": null,
  "vigente": true
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | Falta un campo o esta fuera de rango. | Datos de entrada invalidos (detalles: `fraccionMinutos: must be less than or equal to 60`) |
| 400 | El tipo no es CARRO, MOTO ni BICICLETA. | JSON mal formado o con valores no permitidos (tipo debe ser CARRO, MOTO o BICICLETA) |

### Espacios

#### Listar espacios
`GET /api/espacios`

Devuelve los espacios ordenados por numero. Al iniciar con la tabla vacia la API crea 50: carros 1-25, motos 26-45 y bicicletas 46-50.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `tipo` | query | string (CARRO, MOTO, BICICLETA) | No | Filtra por tipo de espacio. |
| `estado` | query | string (LIBRE, OCUPADO) | No | Filtra por estado. |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/espacios"
```

**Respuesta exitosa: `200`**

```json
[
  {
    "idEspacio": 1,
    "numero": 1,
    "tipo": "CARRO",
    "estado": "LIBRE"
  },
  {
    "idEspacio": 2,
    "numero": 2,
    "tipo": "CARRO",
    "estado": "LIBRE"
  }
]
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | Un filtro tiene un valor invalido. | Valor invalido para el parametro 'estado' |

#### Consultar un espacio por id
`GET /api/espacios/{id}`

Devuelve el espacio con el `idEspacio` indicado (no confundir con `numero`).

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador del espacio (`idEspacio`). |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/espacios/1"
```

**Respuesta exitosa: `200`**

```json
{
  "idEspacio": 1,
  "numero": 1,
  "tipo": "CARRO",
  "estado": "LIBRE"
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe el espacio. | Espacio 999 no encontrado |

#### Crear un espacio
`POST /api/espacios`

Crea un espacio con estado inicial `LIBRE`.

**Cuerpo (JSON)**

| Campo | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `numero` | entero > 0 | Si | Numero visible del espacio. Debe ser unico. |
| `tipo` | string (CARRO, MOTO, BICICLETA) | Si | Tipo de vehiculo que admite. |

```json
{
  "numero": 51,
  "tipo": "CARRO"
}
```

**Ejemplo**

```bash
curl -X POST "http://localhost:8080/api/espacios" \
  -H "Content-Type: application/json" \
  -d '{"numero": 51, "tipo": "CARRO"}'
```

**Respuesta exitosa: `201`**

```json
{
  "idEspacio": 51,
  "numero": 51,
  "tipo": "CARRO",
  "estado": "LIBRE"
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | Falta un campo o el numero no es positivo. | Datos de entrada invalidos (detalles: `numero: must be greater than 0`) |
| 409 | Ya existe un espacio con ese numero. | Ya existe el espacio numero 51 |

#### Crear espacios en lote
`POST /api/espacios/lote`

Crea espacios consecutivos del mismo tipo, de `desde` a `hasta` inclusive (maximo 500). Es todo o nada: si algun numero ya existe no se crea ninguno.

**Cuerpo (JSON)**

| Campo | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `desde` | entero > 0 | Si | Primer numero del lote. |
| `hasta` | entero > 0 | Si | Ultimo numero del lote (incluido). |
| `tipo` | string (CARRO, MOTO, BICICLETA) | Si | Tipo de todos los espacios del lote. |

```json
{
  "desde": 100,
  "hasta": 102,
  "tipo": "CARRO"
}
```

**Ejemplo**

```bash
curl -X POST "http://localhost:8080/api/espacios/lote" \
  -H "Content-Type: application/json" \
  -d '{"desde": 100, "hasta": 102, "tipo": "CARRO"}'
```

**Respuesta exitosa: `201`**

```json
[
  {
    "idEspacio": 52,
    "numero": 100,
    "tipo": "CARRO",
    "estado": "LIBRE"
  },
  {
    "idEspacio": 53,
    "numero": 101,
    "tipo": "CARRO",
    "estado": "LIBRE"
  },
  {
    "idEspacio": 54,
    "numero": 102,
    "tipo": "CARRO",
    "estado": "LIBRE"
  }
]
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | `desde` es mayor que `hasta`. | 'desde' no puede ser mayor que 'hasta' |
| 400 | El lote supera los 500 espacios. | Maximo 500 espacios por lote |
| 409 | Algun numero del rango ya existe. | Ya existe el espacio numero 100; no se creo ninguno |

#### Modificar un espacio
`PUT /api/espacios/{id}`

Cambia el numero y el tipo de un espacio. No se permite si esta `OCUPADO`.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador del espacio (`idEspacio`). |

**Cuerpo (JSON)**

| Campo | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `numero` | entero > 0 | Si | Nuevo numero (unico). |
| `tipo` | string (CARRO, MOTO, BICICLETA) | Si | Nuevo tipo. |

```json
{
  "numero": 1,
  "tipo": "CARRO"
}
```

**Ejemplo**

```bash
curl -X PUT "http://localhost:8080/api/espacios/1" \
  -H "Content-Type: application/json" \
  -d '{"numero": 1, "tipo": "CARRO"}'
```

**Respuesta exitosa: `200`**

```json
{
  "idEspacio": 1,
  "numero": 1,
  "tipo": "CARRO",
  "estado": "LIBRE"
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe el espacio. | Espacio 999 no encontrado |
| 409 | El espacio esta ocupado. | No se puede modificar un espacio ocupado |
| 409 | El nuevo numero pertenece a otro espacio. | Ya existe el espacio numero 2 |

#### Eliminar un espacio
`DELETE /api/espacios/{id}`

Elimina un espacio. No se permite si esta `OCUPADO` ni si ya tiene historial de registros (para conservar la trazabilidad).

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador del espacio (`idEspacio`). |

**Ejemplo**

```bash
curl -X DELETE "http://localhost:8080/api/espacios/1"
```

**Respuesta exitosa: `204`**

Sin cuerpo.

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe el espacio. | Espacio 999 no encontrado |
| 409 | El espacio esta ocupado. | No se puede eliminar un espacio ocupado |
| 409 | El espacio ya fue usado alguna vez. | El espacio tiene historial de registros y no puede eliminarse |

### Registros

#### Registrar entrada de un vehiculo
`POST /api/registros/entrada`

Registra la entrada, asigna un espacio y lo marca `OCUPADO`. Si no se envia `numeroEspacio`, se asigna el espacio libre de menor numero del mismo tipo. Si la placa es nueva se crea el vehiculo; si ya existe, debe ser del mismo tipo. La placa es obligatoria para **todos** los tipos (para bicicletas use un identificador, ej. un serial). Requiere tarifa vigente para el tipo.

**Cuerpo (JSON)**

| Campo | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `placa` | string | Si | 5 a 7 caracteres alfanumericos. Se normaliza a mayusculas sin guiones ni espacios. |
| `tipo` | string (CARRO, MOTO, BICICLETA) | Si | Tipo de vehiculo. |
| `descripcion` | string (max 1000) | No | Texto libre (marca, color...). |
| `numeroEspacio` | entero > 0 | No | Numero del espacio deseado. Debe existir, estar libre y ser del mismo tipo. |

```json
{
  "placa": "ABC123",
  "tipo": "CARRO",
  "descripcion": "Mazda rojo",
  "numeroEspacio": 1
}
```

**Ejemplo**

```bash
curl -X POST "http://localhost:8080/api/registros/entrada" \
  -H "Content-Type: application/json" \
  -d '{"placa": "ABC123", "tipo": "CARRO", "descripcion": "Mazda rojo", "numeroEspacio": 1}'
```

**Respuesta exitosa: `201`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": null,
  "valorPagado": null,
  "cobro": null
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | Falta la placa o el tipo. | Datos de entrada invalidos (detalles: `placa: must not be blank`) |
| 400 | La placa no tiene 5 a 7 caracteres alfanumericos. | Placa invalida: debe tener entre 5 y 7 caracteres alfanumericos |
| 404 | `numeroEspacio` no existe. | Espacio 999 no existe |
| 409 | La placa ya tiene un registro activo. | El vehiculo con placa ABC123 ya esta dentro del parqueadero |
| 409 | La placa existe con otro tipo de vehiculo. | La placa ABC123 esta registrada como MOTO |
| 409 | El espacio pedido es de otro tipo. | El espacio 26 es para MOTO, no para CARRO |
| 409 | El espacio pedido esta ocupado. | El espacio 1 esta ocupado |
| 409 | No quedan espacios libres del tipo. | No hay espacios libres para CARRO |
| 409 | El tipo no tiene tarifa vigente. | No hay tarifa vigente para CARRO |

#### Listar registros
`GET /api/registros`

Devuelve los registros del mas reciente al mas antiguo. No incluye el detalle del cobro (`cobro: null`).

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `activos` | query | boolean | No | `true`: solo vehiculos dentro. `false`: solo finalizados. Sin parametro: todos. |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/registros"
```

**Respuesta exitosa: `200`**

```json
[
  {
    "idRegistro": 1,
    "placa": "ABC123",
    "tipoVehiculo": "CARRO",
    "numeroEspacio": 1,
    "fechaEntrada": "2026-10-05T15:30:00",
    "fechaSalida": null,
    "valorPagado": null,
    "cobro": null
  }
]
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | `activos` no es true/false. | Valor invalido para el parametro 'activos' |

#### Consultar un registro por id
`GET /api/registros/{id}`

Devuelve un registro activo o finalizado.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador del registro (`idRegistro`). |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/registros/1"
```

**Respuesta exitosa: `200`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": null,
  "valorPagado": null,
  "cobro": null
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe el registro. | Registro 999 no encontrado |

#### Consultar cobro por id de registro
`GET /api/registros/{id}/cobro`

Calcula cuanto costaria salir **ahora**, sin cerrar el registro ni liberar el espacio. Si el registro ya tiene salida devuelve el cobro final. `cobro.idTarifa` indica la tarifa aplicada.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador del registro (`idRegistro`). |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/registros/1/cobro"
```

**Respuesta exitosa: `200`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": null,
  "valorPagado": null,
  "cobro": {
    "idTarifa": 1,
    "minutosTotales": 80,
    "horasCompletas": 1,
    "fraccionesCobradas": 2,
    "tarifaHora": 6000,
    "tarifaFraccion": 1500,
    "fraccionMinutos": 15,
    "valorTotal": 9000
  }
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe el registro. | Registro 999 no encontrado |

#### Registrar salida por id de registro
`POST /api/registros/{id}/salida`

Fija la fecha de salida, calcula y guarda `valorPagado`, y libera el espacio. No requiere cuerpo.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `id` | path | integer | Si | Identificador del registro (`idRegistro`). |

**Ejemplo**

```bash
curl -X POST "http://localhost:8080/api/registros/1/salida"
```

**Respuesta exitosa: `200`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": "2026-10-05T16:50:00",
  "valorPagado": 9000.0,
  "cobro": {
    "idTarifa": 1,
    "minutosTotales": 80,
    "horasCompletas": 1,
    "fraccionesCobradas": 2,
    "tarifaHora": 6000,
    "tarifaFraccion": 1500,
    "fraccionMinutos": 15,
    "valorTotal": 9000
  }
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 404 | No existe el registro. | Registro 999 no encontrado |
| 409 | El registro ya tenia salida. | El registro 1 ya tiene salida registrada |

#### Consultar registro activo por placa
`GET /api/registros/placa/{placa}`

Devuelve el registro vigente (sin salida) de la placa.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `placa` | path | string | Si | Placa del vehiculo (5 a 7 caracteres alfanumericos). Se normaliza: `abc-123` equivale a `ABC123`. |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/registros/placa/ABC123"
```

**Respuesta exitosa: `200`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": null,
  "valorPagado": null,
  "cobro": null
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | La placa no es valida. | Placa invalida: debe tener entre 5 y 7 caracteres alfanumericos |
| 404 | La placa no esta dentro del parqueadero. | El vehiculo con placa ABC123 no tiene un registro activo |

#### Consultar cobro por placa
`GET /api/registros/placa/{placa}/cobro`

Calcula cuanto costaria salir **ahora** al vehiculo con esa placa, sin cerrar el registro.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `placa` | path | string | Si | Placa del vehiculo (5 a 7 caracteres alfanumericos). Se normaliza: `abc-123` equivale a `ABC123`. |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/registros/placa/ABC123/cobro"
```

**Respuesta exitosa: `200`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": null,
  "valorPagado": null,
  "cobro": {
    "idTarifa": 1,
    "minutosTotales": 80,
    "horasCompletas": 1,
    "fraccionesCobradas": 2,
    "tarifaHora": 6000,
    "tarifaFraccion": 1500,
    "fraccionMinutos": 15,
    "valorTotal": 9000
  }
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | La placa no es valida. | Placa invalida: debe tener entre 5 y 7 caracteres alfanumericos |
| 404 | La placa no esta dentro del parqueadero. | El vehiculo con placa ABC123 no tiene un registro activo |

#### Registrar salida por placa
`POST /api/registros/placa/{placa}/salida`

Cierra el registro activo de la placa: fija la fecha de salida, calcula y guarda `valorPagado` y libera el espacio. No requiere cuerpo. Repetir la llamada responde 404 porque la placa ya no tiene registro activo.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `placa` | path | string | Si | Placa del vehiculo (5 a 7 caracteres alfanumericos). Se normaliza: `abc-123` equivale a `ABC123`. |

**Ejemplo**

```bash
curl -X POST "http://localhost:8080/api/registros/placa/ABC123/salida"
```

**Respuesta exitosa: `200`**

```json
{
  "idRegistro": 1,
  "placa": "ABC123",
  "tipoVehiculo": "CARRO",
  "numeroEspacio": 1,
  "fechaEntrada": "2026-10-05T15:30:00",
  "fechaSalida": "2026-10-05T16:50:00",
  "valorPagado": 9000.0,
  "cobro": {
    "idTarifa": 1,
    "minutosTotales": 80,
    "horasCompletas": 1,
    "fraccionesCobradas": 2,
    "tarifaHora": 6000,
    "tarifaFraccion": 1500,
    "fraccionMinutos": 15,
    "valorTotal": 9000
  }
}
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | La placa no es valida. | Placa invalida: debe tener entre 5 y 7 caracteres alfanumericos |
| 404 | La placa no esta dentro (o ya salio). | El vehiculo con placa ABC123 no tiene un registro activo |

### Vehiculos

#### Listar vehiculos
`GET /api/vehiculos`

Devuelve todos los vehiculos que han ingresado alguna vez (se crean al registrar su primera entrada).

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/vehiculos"
```

**Respuesta exitosa: `200`**

```json
[
  {
    "idVehiculo": 1,
    "placa": "ABC123",
    "tipo": "CARRO",
    "descripcion": "Mazda rojo"
  }
]
```

#### Historial de una placa
`GET /api/vehiculos/{placa}/registros`

Devuelve todos los registros (entradas y salidas) de la placa, del mas reciente al mas antiguo. Lista vacia si la placa nunca ingreso.

**Parametros**

| Nombre | Ubicacion | Tipo | Obligatorio | Descripcion |
|---|---|---|---|---|
| `placa` | path | string | Si | Placa del vehiculo (5 a 7 caracteres alfanumericos). Se normaliza: `abc-123` equivale a `ABC123`. |

**Ejemplo**

```bash
curl -X GET "http://localhost:8080/api/vehiculos/ABC123/registros"
```

**Respuesta exitosa: `200`**

```json
[
  {
    "idRegistro": 1,
    "placa": "ABC123",
    "tipoVehiculo": "CARRO",
    "numeroEspacio": 1,
    "fechaEntrada": "2026-10-05T15:30:00",
    "fechaSalida": "2026-10-05T16:50:00",
    "valorPagado": 9000.0,
    "cobro": {
      "idTarifa": 1,
      "minutosTotales": 80,
      "horasCompletas": 1,
      "fraccionesCobradas": 2,
      "tarifaHora": 6000,
      "tarifaFraccion": 1500,
      "fraccionMinutos": 15,
      "valorTotal": 9000
    }
  }
]
```

**Errores**

| Codigo | Cuando | `mensaje` |
|---|---|---|
| 400 | La placa no es valida. | Placa invalida: debe tener entre 5 y 7 caracteres alfanumericos |

## Modelos de datos

**Espacio** (`EspacioResponse`)

| Campo | Tipo | Descripcion |
|---|---|---|
| `idEspacio` | integer | Identificador interno |
| `numero` | integer | Numero visible, unico |
| `tipo` | string | CARRO, MOTO o BICICLETA |
| `estado` | string | LIBRE u OCUPADO |

**Tarifa** (`TarifaResponse`)

| Campo | Tipo | Descripcion |
|---|---|---|
| `idTarifa` | integer | Identificador |
| `tipo` | string | Tipo de vehiculo |
| `valorHora` | integer | COP por hora |
| `valorFraccion` | integer | COP por fraccion |
| `fraccionMinutos` | integer | Minutos de una fraccion |
| `vigenteDesde` | datetime | Inicio de vigencia |
| `vigenteHasta` | datetime / null | Fin de vigencia (`null` = vigente) |
| `vigente` | boolean | `true` si `vigenteHasta` es `null` |

**Registro** (`RegistroResponse`)

| Campo | Tipo | Descripcion |
|---|---|---|
| `idRegistro` | integer | Identificador |
| `placa` | string | Placa normalizada |
| `tipoVehiculo` | string | Tipo del vehiculo |
| `numeroEspacio` | integer | Espacio asignado |
| `fechaEntrada` | datetime | Entrada |
| `fechaSalida` | datetime / null | `null` mientras este dentro |
| `valorPagado` | number / null | COP cobrados al salir |
| `cobro` | objeto / null | Detalle del cobro (solo en salida y consultas de cobro) |

**Cobro** (`cobro`)

| Campo | Tipo | Descripcion |
|---|---|---|
| `idTarifa` | integer | Tarifa aplicada |
| `minutosTotales` | integer | Minutos cobrados (redondeo hacia arriba) |
| `horasCompletas` | integer | Horas completas |
| `fraccionesCobradas` | integer | Fracciones del tiempo sobrante |
| `tarifaHora` / `tarifaFraccion` / `fraccionMinutos` | integer | Valores de la tarifa aplicada |
| `valorTotal` | integer | Total en COP |

**Vehiculo** (`VehiculoResponse`)

| Campo | Tipo | Descripcion |
|---|---|---|
| `idVehiculo` | integer | Identificador |
| `placa` | string | Placa (unica) |
| `tipo` | string | Tipo |
| `descripcion` | string / null | Texto libre |

## Flujo tipico de consumo

1. `GET /api/tarifas?vigentes=true` y `GET /api/espacios?tipo=CARRO&estado=LIBRE` para ver tarifas y disponibilidad.
2. `POST /api/registros/entrada` con la placa y el tipo. Guarde `idRegistro` si va a operar por id.
3. (Opcional) `GET /api/registros/placa/{placa}/cobro` para informar al cliente cuanto pagaria.
4. `POST /api/registros/placa/{placa}/salida` para cobrar y liberar el espacio.
5. `GET /api/vehiculos/{placa}/registros` para consultar el historial.

## Pruebas con Postman

Se entregan dos archivos importables:

| Archivo | Uso |
|---|---|
| `postman/Parking2027.postman_collection.json` | Coleccion Postman v2.1 con carpetas por flujo, descripcion de cada peticion y scripts que guardan variables |
| `docs/openapi.json` | Especificacion OpenAPI 3.0 de la API |

**Importar:** Postman > *Import* > arrastre el archivo (o *Upload Files*). Si la coleccion fallara, importe `docs/openapi.json`: Postman genera una coleccion con todos los endpoints. Si el error persiste, copie el mensaje exacto que muestra Postman.

**Uso:** variables de coleccion `baseUrl` (`http://localhost:8080`), `placa` (`ABC123`), `idRegistro` e `idEspacioNuevo` (las llenan los scripts). Ejecute las carpetas en orden numerico:
1. *Verificar datos iniciales* - confirma los 50 espacios y las 3 tarifas.
2. *Flujo por placa* - entrada, cobro, salida por placa y errores esperados.
3. *Flujo por id* - el mismo ciclo operando con `idRegistro`.
4. *Motos y bicicletas* - asignacion automatica (motos desde el espacio 26) y validaciones.
5. *Tarifas* - historial y creacion de nuevas tarifas.
6. *Espacios (CRUD)* - crear, lote, modificar y eliminar.

Las peticiones con `(espera 4xx)` en el nombre estan pensadas para provocar ese error. Para ver saltos de fraccion sin esperar horas cree una tarifa con `fraccionMinutos: 1` (peticion *Tarifa de prueba*) y consulte el cobro cada minuto.

## Estructura del proyecto
```
config/      TarifaProperties, EspacioInicialProperties (valores iniciales),
             TarifaInicializador, EspacioInicializador (siembra), ZonaHoraria
controller/  Espacio, Registro, Vehiculo, Tarifa (Javadoc de cada metodo)
dto/         Requests y responses (las entidades no se exponen)
exception/   ApiException + GlobalExceptionHandler (errores JSON uniformes)
model/       Entidades JPA: EspacioParqueo, Vehiculo, RegistroParqueo, Tarifa
repository/  Repositorios Spring Data JPA
service/     EspacioService, RegistroService, TarifaService
```

## Concurrencia y limitaciones
- Las entradas de una misma placa se serializan bloqueando la fila del vehiculo; una placa nueva repetida en paralelo la rechaza el `UNIQUE` (409).
- Las salidas bloquean el registro (por id o por placa): una salida doble no cobra dos veces.
- Asignacion de espacios con bloqueo pesimista: dos entradas simultaneas no reciben el mismo espacio.
- MySQL no permite un indice unico parcial ("un solo registro activo por placa") sin cambiar el modelo; esa regla la garantiza el servicio con el bloqueo anterior.
- Sin autenticacion: cualquiera con acceso a la URL puede cambiar tarifas.
- No hay entidad de pagos o tickets: `valorPagado` se fija al registrar la salida.
- Cambiar una tarifa afecta a los vehiculos que ya estan dentro (se cobra con la vigente a la salida).
