# Parking 2027 API

API REST de parqueadero (Spring Boot 4.1.1, Java 25, MySQL). Gestiona espacios, entradas/salidas de vehiculos y cobro por **hora y fraccion**.

## Requisitos
- JDK 25
- MySQL 8+ en `localhost:3306` con la base `parqueadero_db` creada (`CREATE DATABASE parqueadero_db;`)
- Credenciales en `src/main/resources/application.properties` (por defecto `root`/`root`)

## Estructura
```
config/      TarifaProperties (lee tarifas de application.properties)
controller/  EspacioController, RegistroController, VehiculoController
dto/         Requests/Responses (las entidades no se exponen)
exception/   ApiException + GlobalExceptionHandler (errores JSON uniformes)
repository/  Repositorios Spring Data JPA
service/     EspacioService, RegistroService, TarifaService
```

## Tarifas (por hora y fraccion)
Como no se agregan tablas ni clases al `model`, las tarifas se configuran en `application.properties`:

```properties
parking.tarifa.fraccion-minutos=15
parking.tarifa.tipos.carro.hora=6000
parking.tarifa.tipos.carro.fraccion=1500
parking.tarifa.tipos.moto.hora=3000
parking.tarifa.tipos.moto.fraccion=750
parking.tarifa.tipos.bicicleta.hora=1000
parking.tarifa.tipos.bicicleta.fraccion=250
```

Reglas de calculo:
1. El tiempo se redondea hacia arriba al minuto.
2. Cada hora completa se cobra a la tarifa de hora.
3. Los minutos sobrantes se cobran por fracciones iniciadas (bloques de `fraccion-minutos`), sin superar nunca el valor de una hora.
4. Minimo cobrable: una fraccion.

Ejemplos (carro: hora 6000, fraccion 1500 cada 15 min):

| Tiempo | Calculo | Total |
|---|---|---|
| 3 min | 1 fraccion | 1.500 |
| 16 min | 2 fracciones | 3.000 |
| 50 min | 4 fracciones = 6.000 (tope: 1 hora) | 6.000 |
| 1 h 20 min | 6.000 + 2 fracciones (3.000) | 9.000 |
| 2 h 01 min | 12.000 + 1 fraccion | 13.500 |

La hora usada es `America/Bogota`.

## Endpoints

### Espacios
| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/espacios` | Crear espacio `{ "numero": 1, "tipo": "CARRO" }` |
| POST | `/api/espacios/lote` | Crear varios `{ "desde": 1, "hasta": 10, "tipo": "CARRO" }` (max 500) |
| GET | `/api/espacios?tipo=&estado=` | Listar (filtros opcionales: `tipo`, `estado`) |
| GET | `/api/espacios/{id}` | Obtener |
| PUT | `/api/espacios/{id}` | Modificar numero/tipo (no si esta ocupado) |
| DELETE | `/api/espacios/{id}` | Eliminar (no si esta ocupado o tiene historial) |

### Registros
| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/registros/entrada` | Registrar entrada |
| POST | `/api/registros/{id}/salida` | Registrar salida y cobrar |
| GET | `/api/registros/{id}/cobro` | Ver cuanto costaria salir ahora (no cierra el registro) |
| GET | `/api/registros/{id}` | Obtener registro |
| GET | `/api/registros?activos=true|false` | Listar (sin parametro: todos) |

Entrada:
```json
{ "placa": "ABC123", "tipo": "CARRO", "descripcion": "Mazda rojo", "numeroEspacio": 3 }
```
- `placa`: obligatoria para CARRO y MOTO; opcional para BICICLETA. Se normaliza (mayusculas, sin guiones/espacios), 5 a 7 caracteres alfanumericos.
- `numeroEspacio`: opcional. Si falta, se asigna el espacio libre de menor numero del mismo tipo.

### Vehiculos
| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/vehiculos` | Listar vehiculos conocidos |
| GET | `/api/vehiculos/{placa}/registros` | Historial de una placa |

## Errores
Formato uniforme:
```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "El espacio 3 esta ocupado", "detalles": [] }
```
`400` datos invalidos · `404` no encontrado · `409` conflicto de negocio (placa ya dentro, espacio ocupado/inexistente para el tipo, sin espacios libres, salida ya registrada).

## Pruebas con Postman
1. Importar `postman/Parking2027.postman_collection.json`.
2. La variable `baseUrl` ya apunta a `http://localhost:8080`.
3. Orden sugerido: *Crear espacios en lote* -> *Entrada carro* -> *Ver cobro* -> *Salida* -> *Listar espacios*.
   La peticion de entrada guarda `idRegistro` en una variable de coleccion y las siguientes la reutilizan.
4. Para probar tarifas sin esperar horas, baje temporalmente `parking.tarifa.fraccion-minutos` y las tarifas, o consulte `/cobro` tras unos minutos.
