# language: es
Característica: Generación de Reporte de Estado de Cuenta

  Escenario: Consulta de estado de cuenta en rango de fechas para un cliente por su Id (Caso de Uso 5)
    Dado que existe el cliente "Marianela Montalvo" con identificación "097548965" y id 2
    Y la cuenta "225487" de tipo "Corriente" con saldo inicial de 100 asociada al cliente 2
    Y la cuenta "496825" de tipo "Ahorros" con saldo inicial de 540 asociada al cliente 2
    Y se registró un movimiento en la cuenta "225487" de tipo "DEPOSITO" por valor de 600 con fecha "10/02/2022" resultando en saldo disponible de 700
    Y se registró un movimiento en la cuenta "496825" de tipo "RETIRO" por valor de -540 con fecha "08/02/2022" resultando en saldo disponible de 0
    Cuando consulto el reporte de estado de cuenta para el cliente con id "2" entre "01/02/2022" y "15/02/2022"
    Entonces el sistema responde con código HTTP 200
    Y el reporte contiene 2 cuentas
    Y la cuenta "225487" tiene saldo 700 con 1 movimientos donde el movimiento 1 tiene saldo inicial 100, valor 600 y saldo disponible 700
    Y la cuenta "496825" tiene saldo 0 con 1 movimientos donde el movimiento 1 tiene saldo inicial 540, valor -540 y saldo disponible 0
