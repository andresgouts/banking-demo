package com.banking.demo.cuenta.application.service

import com.banking.demo.cuenta.application.dto.MovimientoRequestDto
import com.banking.demo.cuenta.application.mapper.MovimientoMapper
import com.banking.demo.cuenta.application.service.impl.MovimientoServiceImpl
import com.banking.demo.cuenta.domain.exception.EntityNotFoundException
import com.banking.demo.cuenta.domain.exception.InsufficientBalanceException
import com.banking.demo.cuenta.domain.model.ClienteSnapshot
import com.banking.demo.cuenta.domain.model.Cuenta
import com.banking.demo.cuenta.domain.model.Movimiento
import com.banking.demo.cuenta.domain.model.TipoCuenta
import com.banking.demo.cuenta.domain.model.TipoMovimiento
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository
import com.banking.demo.cuenta.domain.repository.CuentaRepository
import com.banking.demo.cuenta.domain.repository.MovimientoRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class MovimientoUseCaseSpec extends Specification {

    MovimientoRepository movimientoRepository = Mock()
    CuentaRepository cuentaRepository = Mock()
    ClienteSnapshotRepository clienteSnapshotRepository = Mock()
    MovimientoMapper movimientoMapper = new MovimientoMapper()

    @Subject
    MovimientoService movimientoService = new MovimientoServiceImpl(
            movimientoRepository,
            cuentaRepository,
            clienteSnapshotRepository,
            movimientoMapper
    )

    def setup() {
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Unroll
    def "deberia evaluar logica de saldo segun matriz: saldoInicial=#saldoInicial, valorMovimiento=#valorMovimiento"() {
        given: "Una cuenta activa con saldo inicial"
        def cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(BigDecimal.valueOf(saldoInicial))
                .saldoActual(BigDecimal.valueOf(saldoInicial))
                .estado(true)
                .clienteId(2L)
                .build()

        def request = MovimientoRequestDto.builder()
                .numeroCuenta("478758")
                .valor(BigDecimal.valueOf(valorMovimiento))
                .build()

        cuentaRepository.findByNumeroCuentaForUpdate("478758") >> Optional.of(cuenta)
        cuentaRepository.save(_ as Cuenta) >> { Cuenta c -> c }
        movimientoRepository.save(_ as Movimiento) >> { Movimiento m ->
            m.id = 100L
            return m
        }
        clienteSnapshotRepository.findById(2L) >> Optional.of(
                ClienteSnapshot.builder().id(2L).nombre("Marianela Montalvo").build()
        )

        when: "Se ejecuta el registro de la transaccion"
        BigDecimal saldoObtenido = null
        Exception excepcionCapturada = null
        try {
            def response = movimientoService.registrarMovimiento(request)
            saldoObtenido = response.saldo
        } catch (Exception ex) {
            excepcionCapturada = ex
        }

        then: "El resultado coincide con la regla de negocio y matriz de prueba"
        if (excepcionEsperada != null) {
            excepcionCapturada != null
            excepcionEsperada.isInstance(excepcionCapturada)
            excepcionCapturada.message == "Saldo no disponible"
            saldoObtenido == null
        } else {
            excepcionCapturada == null
            saldoObtenido != null
            saldoObtenido.compareTo(BigDecimal.valueOf(saldoEsperado)) == 0
            cuenta.saldoActual.compareTo(BigDecimal.valueOf(saldoEsperado)) == 0
        }

        where:
        saldoInicial | valorMovimiento || saldoEsperado | excepcionEsperada
        1000.0       | 500.0           || 1500.0        | null
        1000.0       | -400.0          || 600.0         | null
        540.0        | -540.0          || 0.0           | null
        100.0        | -200.0          || 0.0           | InsufficientBalanceException
    }

    def "deberia lanzar InsufficientBalanceException con mensaje explicito 'Saldo no disponible' ante retiro excesivo"() {
        given: "Una cuenta con saldo disponible de 200"
        def cuenta = Cuenta.builder()
                .numeroCuenta("478758")
                .saldoActual(BigDecimal.valueOf(200.0))
                .estado(true)
                .clienteId(2L)
                .build()

        def request = MovimientoRequestDto.builder()
                .numeroCuenta("478758")
                .valor(BigDecimal.valueOf(-575.0))
                .build()

        cuentaRepository.findByNumeroCuentaForUpdate("478758") >> Optional.of(cuenta)

        when: "Se intenta registrar un retiro de 575"
        movimientoService.registrarMovimiento(request)

        then: "Se rechaza la transaccion con InsufficientBalanceException y mensaje 'Saldo no disponible'"
        def ex = thrown(InsufficientBalanceException)
        ex.message == "Saldo no disponible"
        0 * movimientoRepository.save(_)
    }

    def "deberia registrar deposito exitoso asignando TipoMovimiento.DEPOSITO"() {
        given: "Una cuenta con saldo inicial de 100"
        def cuenta = Cuenta.builder()
                .id(2L)
                .numeroCuenta("225487")
                .tipoCuenta(TipoCuenta.CORRIENTE)
                .saldoInicial(BigDecimal.valueOf(100.0))
                .saldoActual(BigDecimal.valueOf(100.0))
                .estado(true)
                .clienteId(2L)
                .build()

        def request = MovimientoRequestDto.builder()
                .numeroCuenta("225487")
                .valor(BigDecimal.valueOf(600.0))
                .build()

        cuentaRepository.findByNumeroCuentaForUpdate("225487") >> Optional.of(cuenta)
        cuentaRepository.save(_ as Cuenta) >> { Cuenta c -> c }
        clienteSnapshotRepository.findById(2L) >> Optional.of(
                ClienteSnapshot.builder().id(2L).nombre("Marianela Montalvo").build()
        )

        when: "Se realiza un deposito de 600"
        def response = movimientoService.registrarMovimiento(request)

        then: "El saldo resultante es 700 y el movimiento es clasificado como DEPOSITO"
        1 * movimientoRepository.save({ Movimiento m ->
            m.tipoMovimiento == TipoMovimiento.DEPOSITO &&
                    m.valor == BigDecimal.valueOf(600.0) &&
                    m.saldo == BigDecimal.valueOf(700.0)
        }) >> { Movimiento m ->
            m.id = 1L
            return m
        }

        response != null
        response.tipoMovimiento == TipoMovimiento.DEPOSITO
        response.valor == BigDecimal.valueOf(600.0)
        response.saldo == BigDecimal.valueOf(700.0)
        response.saldoInicial == BigDecimal.valueOf(100.0)
        response.nombreCliente == "Marianela Montalvo"
    }

    def "deberia registrar retiro exitoso asignando TipoMovimiento.RETIRO"() {
        given: "Una cuenta con saldo de 540"
        def cuenta = Cuenta.builder()
                .id(3L)
                .numeroCuenta("495878")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(BigDecimal.valueOf(540.0))
                .saldoActual(BigDecimal.valueOf(540.0))
                .estado(true)
                .clienteId(3L)
                .build()

        def request = MovimientoRequestDto.builder()
                .numeroCuenta("495878")
                .valor(BigDecimal.valueOf(-540.0))
                .build()

        cuentaRepository.findByNumeroCuentaForUpdate("495878") >> Optional.of(cuenta)
        cuentaRepository.save(_ as Cuenta) >> { Cuenta c -> c }
        movimientoRepository.save(_ as Movimiento) >> { Movimiento m ->
            m.id = 2L
            return m
        }
        clienteSnapshotRepository.findById(3L) >> Optional.of(
                ClienteSnapshot.builder().id(3L).nombre("Juan Osorio").build()
        )

        when: "Se retira la totalidad del saldo (540)"
        def response = movimientoService.registrarMovimiento(request)

        then: "El saldo resultante es 0 y el movimiento es clasificado como RETIRO"
        response != null
        response.tipoMovimiento == TipoMovimiento.RETIRO
        response.valor == BigDecimal.valueOf(-540.0)
        response.saldo == BigDecimal.valueOf(0.0)
        response.nombreCliente == "Juan Osorio"
    }

    def "deberia lanzar IllegalArgumentException cuando el valor del movimiento es cero o nulo"() {
        given: "Un request con valor cero"
        def requestCero = MovimientoRequestDto.builder().numeroCuenta("123456").valor(BigDecimal.ZERO).build()
        def requestNulo = MovimientoRequestDto.builder().numeroCuenta("123456").valor(null).build()

        when: "Se intenta registrar transaccion con valor cero"
        movimientoService.registrarMovimiento(requestCero)

        then: "Lanza IllegalArgumentException"
        def ex1 = thrown(IllegalArgumentException)
        ex1.message == "El valor del movimiento no puede ser cero ni nulo"

        when: "Se intenta registrar transaccion con valor nulo"
        movimientoService.registrarMovimiento(requestNulo)

        then: "Lanza IllegalArgumentException"
        def ex2 = thrown(IllegalArgumentException)
        ex2.message == "El valor del movimiento no puede ser cero ni nulo"
    }

    def "deberia lanzar IllegalArgumentException cuando el numero de cuenta es vacio o nulo"() {
        given: "Requests con numero de cuenta invalido"
        def requestNull = MovimientoRequestDto.builder().numeroCuenta(null).valor(BigDecimal.TEN).build()
        def requestBlank = MovimientoRequestDto.builder().numeroCuenta("   ").valor(BigDecimal.TEN).build()

        when: "Numero de cuenta es null"
        movimientoService.registrarMovimiento(requestNull)

        then:
        thrown(IllegalArgumentException)

        when: "Numero de cuenta es en blanco"
        movimientoService.registrarMovimiento(requestBlank)

        then:
        thrown(IllegalArgumentException)
    }

    def "deberia lanzar EntityNotFoundException cuando la cuenta no existe"() {
        given: "Un numero de cuenta inexistente"
        def request = MovimientoRequestDto.builder().numeroCuenta("999999").valor(BigDecimal.TEN).build()
        cuentaRepository.findByNumeroCuentaForUpdate("999999") >> Optional.empty()

        when: "Se intenta registrar movimiento"
        movimientoService.registrarMovimiento(request)

        then: "Se lanza EntityNotFoundException"
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("No se encontro la cuenta con el numero: 999999")
    }

    def "deberia lanzar IllegalArgumentException cuando la cuenta se encuentra inactiva"() {
        given: "Una cuenta con estado false"
        def cuentaInactiva = Cuenta.builder()
                .numeroCuenta("478758")
                .saldoActual(BigDecimal.valueOf(100.0))
                .estado(false)
                .clienteId(2L)
                .build()

        def request = MovimientoRequestDto.builder().numeroCuenta("478758").valor(BigDecimal.TEN).build()
        cuentaRepository.findByNumeroCuentaForUpdate("478758") >> Optional.of(cuentaInactiva)

        when: "Se intenta registrar movimiento en cuenta inactiva"
        movimientoService.registrarMovimiento(request)

        then: "Se rechaza informando inactividad de la cuenta"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("se encuentra inactiva")
    }

    def "deberia rechazar el registro de movimiento si el cliente autenticado no es el dueno de la cuenta"() {
        given: "Un usuario autenticado diferente al dueno de la cuenta"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken("999", null, Collections.emptyList())

        def cuenta = Cuenta.builder()
                .numeroCuenta("478758")
                .saldoActual(BigDecimal.valueOf(500.0))
                .estado(true)
                .clienteId(1L)
                .build()

        def request = MovimientoRequestDto.builder().numeroCuenta("478758").valor(BigDecimal.TEN).build()
        cuentaRepository.findByNumeroCuentaForUpdate("478758") >> Optional.of(cuenta)

        when: "Se intenta registrar movimiento en cuenta de otro cliente"
        movimientoService.registrarMovimiento(request)

        then: "Lanza AccessDeniedException"
        def ex = thrown(AccessDeniedException)
        ex.message.contains("No tiene permisos para operar sobre una cuenta que no le pertenece")
        0 * movimientoRepository.save(_)
        0 * cuentaRepository.save(_)
    }

    def "deberia consultar movimientos por numero de cuenta validando existencia previa"() {
        given: "Una cuenta existente con 2 movimientos"
        def cuenta = Cuenta.builder().numeroCuenta("478758").clienteId(1L).tipoCuenta(TipoCuenta.AHORROS).estado(true).build()
        def m1 = Movimiento.builder().id(1L).cuenta(cuenta).numeroCuenta("478758").valor(BigDecimal.valueOf(100.0)).saldo(BigDecimal.valueOf(100.0)).build()
        def m2 = Movimiento.builder().id(2L).cuenta(cuenta).numeroCuenta("478758").valor(BigDecimal.valueOf(-50.0)).saldo(BigDecimal.valueOf(50.0)).build()

        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(cuenta)
        movimientoRepository.findByNumeroCuentaWithCuenta("478758") >> [m1, m2]
        clienteSnapshotRepository.findById(1L) >> Optional.of(
                ClienteSnapshot.builder().id(1L).nombre("Jose Lema").build()
        )

        when: "Se consultan movimientos de la cuenta"
        def list = movimientoService.findByNumeroCuenta("478758")

        then: "Retorna ambos movimientos"
        list.size() == 2
        list[0].valor == BigDecimal.valueOf(100.0)
        list[1].valor == BigDecimal.valueOf(-50.0)
    }

    def "deberia rechazar consulta por numero de cuenta si la cuenta pertenece a otro cliente"() {
        given: "Usuario autenticado 999 y cuenta de cliente 1"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken("999", null, Collections.emptyList())

        def cuenta = Cuenta.builder().numeroCuenta("478758").clienteId(1L).build()
        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(cuenta)

        when: "Se consultan los movimientos de la cuenta ajena"
        movimientoService.findByNumeroCuenta("478758")

        then: "Lanza AccessDeniedException"
        def ex = thrown(AccessDeniedException)
        ex.message.contains("No tiene permisos para operar sobre una cuenta que no le pertenece")
    }

    def "deberia rechazar consulta por clienteId si no coincide con el usuario autenticado"() {
        given: "Usuario autenticado 1"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList())

        when: "Intenta consultar movimientos de cliente 2"
        movimientoService.findByClienteId(2L)

        then: "Lanza AccessDeniedException"
        def ex = thrown(AccessDeniedException)
        ex.message.contains("No tiene permisos para operar sobre una cuenta que no le pertenece")
    }

    def "deberia consultar por clienteId exitosamente cuando coincide con el usuario autenticado"() {
        given: "Usuario autenticado 1"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList())

        def cuenta = Cuenta.builder().numeroCuenta("478758").clienteId(1L).build()
        def m = Movimiento.builder().id(1L).cuenta(cuenta).numeroCuenta("478758").valor(BigDecimal.valueOf(100.0)).saldo(BigDecimal.valueOf(100.0)).build()

        movimientoRepository.findByClienteIdWithCuenta(1L) >> [m]
        clienteSnapshotRepository.findById(1L) >> Optional.of(
                ClienteSnapshot.builder().id(1L).nombre("Cliente Uno").build()
        )

        when: "Se consultan los movimientos de 1L"
        def list = movimientoService.findByClienteId(1L)

        then: "Retorna los movimientos asociados al cliente"
        list.size() == 1
        list[0].nombreCliente == "Cliente Uno"
    }
}
