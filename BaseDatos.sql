-- =============================================================================
-- SISTEMA BANCARIO - SCRIPT COMPLETO DE BASE DE DATOS (BaseDatos.sql)
-- Proyecto: Banking-Demo
-- Arquitectura: Microservicios con Database-per-Service (PostgreSQL 16)
-- Microservicios:
--   1. cliente-service            -> Base de datos: db_clientes
--   2. cuenta-movimientos-service  -> Base de datos: db_cuentas
-- =============================================================================

-- =============================================================================
-- 1. CREACIÓN DE BASES DE DATOS (Si no existen)
-- =============================================================================
SELECT 'CREATE DATABASE db_clientes' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_clientes')\gexec
SELECT 'CREATE DATABASE db_cuentas' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_cuentas')\gexec

GRANT ALL PRIVILEGES ON DATABASE db_clientes TO postgres;
GRANT ALL PRIVILEGES ON DATABASE db_cuentas TO postgres;


-- =============================================================================
-- 2. ESQUEMA Y DATOS: db_clientes (cliente-service)
-- =============================================================================
\c db_clientes;

-- Tabla: personas
CREATE TABLE IF NOT EXISTS personas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    genero VARCHAR(20),
    edad INTEGER,
    identificacion VARCHAR(20) NOT NULL UNIQUE,
    direccion VARCHAR(200),
    telefono VARCHAR(20)
);

-- Tabla: clientes (Estrategia de Herencia JOINED con personas)
CREATE TABLE IF NOT EXISTS clientes (
    persona_id BIGINT PRIMARY KEY,
    contrasena VARCHAR(100) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP WITHOUT TIME ZONE,
    fecha_eliminacion TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_clientes_persona FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE
);

-- Índices para optimización de consultas
CREATE INDEX IF NOT EXISTS idx_personas_identificacion ON personas(identificacion);

-- Limpieza previa para idempotencia
DELETE FROM clientes;
DELETE FROM personas;
ALTER SEQUENCE personas_id_seq RESTART WITH 1;

-- -----------------------------------------------------------------------------
-- Inserción de Datos: Caso de Uso 1 - Creación de Usuarios
-- Contraseñas encriptadas con algoritmo BCrypt (fuerza 10):
--   '1234' -> $2a$10$QIFURNJF4kcHUkW7xTkVBuBoBG7LuYbpieV4sxMtYnpYPxdRmZovO
--   '5678' -> $2a$10$X7tm6R6n6Ln2icoyrQeqmOh79fv7M3qpSh5ESenFrpJ1i9YJqwY.K
--   '1245' -> $2a$10$oEkLcsQcVuiBStuwCIhvMumcODEsS92j.MDbifyIODNhSJhbE7KDy
-- -----------------------------------------------------------------------------

-- Usuario 1: Jose Lema
INSERT INTO personas (id, nombre, genero, edad, identificacion, direccion, telefono)
VALUES (1, 'Jose Lema', 'Masculino', 35, '1234567890', 'Otavalo sn y principal', '098254785');

INSERT INTO clientes (persona_id, contrasena, estado, fecha_creacion)
VALUES (1, '$2a$10$QIFURNJF4kcHUkW7xTkVBuBoBG7LuYbpieV4sxMtYnpYPxdRmZovO', TRUE, CURRENT_TIMESTAMP);

-- Usuario 2: Marianela Montalvo
INSERT INTO personas (id, nombre, genero, edad, identificacion, direccion, telefono)
VALUES (2, 'Marianela Montalvo', 'Femenino', 28, '097548965', 'Amazonas y NNUU', '097548965');

INSERT INTO clientes (persona_id, contrasena, estado, fecha_creacion)
VALUES (2, '$2a$10$X7tm6R6n6Ln2icoyrQeqmOh79fv7M3qpSh5ESenFrpJ1i9YJqwY.K', TRUE, CURRENT_TIMESTAMP);

-- Usuario 3: Juan Osorio
INSERT INTO personas (id, nombre, genero, edad, identificacion, direccion, telefono)
VALUES (3, 'Juan Osorio', 'Masculino', 40, '098874587', '13 junio y Equinoccial', '098874587');

INSERT INTO clientes (persona_id, contrasena, estado, fecha_creacion)
VALUES (3, '$2a$10$oEkLcsQcVuiBStuwCIhvMumcODEsS92j.MDbifyIODNhSJhbE7KDy', TRUE, CURRENT_TIMESTAMP);

SELECT setval('personas_id_seq', (SELECT MAX(id) FROM personas));


-- =============================================================================
-- 3. ESQUEMA Y DATOS: db_cuentas (cuenta-movimientos-service)
-- =============================================================================
\c db_cuentas;

-- Tabla: cliente_snapshots (Proyección y Réplica Asíncrona Event-Driven)
CREATE TABLE IF NOT EXISTS cliente_snapshots (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    identificacion VARCHAR(20) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: cuentas
CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(30) NOT NULL UNIQUE,
    tipo_cuenta VARCHAR(20) NOT NULL,
    saldo_inicial NUMERIC(15, 2) NOT NULL,
    saldo_actual NUMERIC(15, 2) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    cliente_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP WITHOUT TIME ZONE
);

-- Tabla: movimientos (Ledger de Transacciones Inmutables)
CREATE TABLE IF NOT EXISTS movimientos (\n    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    numero_cuenta VARCHAR(30) NOT NULL,
    fecha TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    tipo_movimiento VARCHAR(20) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    saldo NUMERIC(15, 2) NOT NULL,
    CONSTRAINT fk_movimientos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE
);

-- Índices para optimización de consultas y reportes
CREATE INDEX IF NOT EXISTS idx_cliente_snapshots_identificacion ON cliente_snapshots(identificacion);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_id ON movimientos(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_numero_cuenta ON movimientos(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos(fecha);

-- Limpieza previa para idempotencia
DELETE FROM movimientos;
DELETE FROM cuentas;
DELETE FROM cliente_snapshots;
ALTER SEQUENCE cuentas_id_seq RESTART WITH 1;
ALTER SEQUENCE movimientos_id_seq RESTART WITH 1;

-- -----------------------------------------------------------------------------
-- Inserción de Datos: cliente_snapshots (Sincronización inicial con IDs de cliente)
-- -----------------------------------------------------------------------------
INSERT INTO cliente_snapshots (id, nombre, identificacion, estado, fecha_actualizacion)
VALUES 
    (1, 'Jose Lema', '1234567890', TRUE, CURRENT_TIMESTAMP),
    (2, 'Marianela Montalvo', '097548965', TRUE, CURRENT_TIMESTAMP),
    (3, 'Juan Osorio', '098874587', TRUE, CURRENT_TIMESTAMP);

-- -----------------------------------------------------------------------------
-- Inserción de Datos: Casos de Uso 2 y 3 - Creación de Cuentas de Usuario
-- Estado de saldos actualizados tras los movimientos del Caso de Uso 4
-- -----------------------------------------------------------------------------
INSERT INTO cuentas (id, numero_cuenta, tipo_cuenta, saldo_inicial, saldo_actual, estado, cliente_id, fecha_creacion)
VALUES 
    -- Caso 2: Jose Lema (ID: 1) - Cuenta Ahorros inicial 2000, actual 1425 tras retiro de 575
    (1, '478758', 'AHORROS', 2000.00, 1425.00, TRUE, 1, '2026-02-01 08:00:00'),
    -- Caso 2: Marianela Montalvo (ID: 2) - Cuenta Corriente inicial 100, actual 700 tras depósito de 600
    (2, '225487', 'CORRIENTE', 100.00, 700.00, TRUE, 2, '2026-02-01 08:30:00'),
    -- Caso 2: Juan Osorio (ID: 3) - Cuenta Ahorros inicial 0, actual 150 tras depósito de 150
    (3, '495878', 'AHORROS', 0.00, 150.00, TRUE, 3, '2026-02-01 09:00:00'),
    -- Caso 2: Marianela Montalvo (ID: 2) - Cuenta Ahorros inicial 540, actual 0 tras retiro de 540
    (4, '496825', 'AHORROS', 540.00, 0.00, TRUE, 2, '2026-02-01 09:30:00'),
    -- Caso 3: Jose Lema (ID: 1) - Nueva Cuenta Corriente inicial 1000
    (5, '585545', 'CORRIENTE', 1000.00, 1000.00, TRUE, 1, '2026-02-05 10:00:00');

SELECT setval('cuentas_id_seq', (SELECT MAX(id) FROM cuentas));

-- -----------------------------------------------------------------------------
-- Inserción de Datos: Casos de Uso 4 y 5 - Registro de Movimientos y Reportes
-- -----------------------------------------------------------------------------
INSERT INTO movimientos (id, cuenta_id, numero_cuenta, fecha, tipo_movimiento, valor, saldo)
VALUES 
    -- Movimiento 1: Retiro de 575 en cuenta 478758 (Jose Lema)
    (1, 1, '478758', '2026-02-10 10:00:00', 'RETIRO', -575.00, 1425.00),
    -- Movimiento 2: Deposito de 600 en cuenta 225487 (Marianela Montalvo - Caso 5)
    (2, 2, '225487', '2026-02-10 11:30:00', 'DEPOSITO', 600.00, 700.00),
    -- Movimiento 3: Deposito de 150 en cuenta 495878 (Juan Osorio)
    (3, 3, '495878', '2026-02-10 14:15:00', 'DEPOSITO', 150.00, 150.00),
    -- Movimiento 4: Retiro de 540 en cuenta 496825 (Marianela Montalvo - Caso 5)
    (4, 4, '496825', '2026-02-08 09:45:00', 'RETIRO', -540.00, 0.00);

SELECT setval('movimientos_id_seq', (SELECT MAX(id) FROM movimientos));

-- =============================================================================
-- FIN DEL SCRIPT BaseDatos.sql
-- =============================================================================
