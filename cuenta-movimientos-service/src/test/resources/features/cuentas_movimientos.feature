# language: es
Característica: Gestión de Cuentas y Registro de Movimientos

  Escenario: Depósito exitoso que actualiza el saldo disponible
    Dado que existe la cuenta "225487" de tipo "Corriente" con saldo inicial de 100
    Cuando realizo un movimiento de tipo "DEPOSITO" por valor de 600
    Entonces el sistema responde con código HTTP 201
    Y el nuevo saldo disponible es 700

  Escenario: Intento de retiro con saldo insuficiente
    Dado que existe la cuenta "478758" con saldo disponible de 200
    Cuando realizo un movimiento de tipo "RETIRO" por valor de 575
    Entonces el sistema responde con código HTTP 400
    Y el mensaje de error es "Saldo no disponible"
