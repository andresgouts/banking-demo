# language: es
Característica: Creación y Registro de Clientes en el Sistema Bancario

  Escenario: Registro exitoso de un nuevo cliente (auto-registro público)
    Cuando se envía una solicitud POST a "/api/clientes" para registrar un cliente con nombre "Jose Lema", identificación "1234567890", contraseña "1234", género "Masculino", edad 35, dirección "Otavalo sn y principal" y teléfono "098254785"
    Entonces el sistema responde con código HTTP 201
    Y el cliente queda registrado en el sistema con estado activo

  Escenario: Intento de registro con identificación duplicada
    Dado que existe un cliente registrado previamente con identificación "1234567890"
    Cuando se envía una solicitud POST a "/api/clientes" con la misma identificación "1234567890"
    Entonces el sistema responde con código HTTP 409
    Y se retorna un mensaje de error indicando duplicidad
