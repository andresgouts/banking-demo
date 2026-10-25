package com.banking.demo.cuenta.application.service

import com.banking.demo.cuenta.application.dto.CuentaPatchDto
import com.banking.demo.cuenta.application.dto.CuentaRequestDto
import com.banking.demo.cuenta.application.dto.CuentaResponseDto
import com.banking.demo.cuenta.application.dto.CuentaUpdateDto
import com.banking.demo.cuenta.application.mapper.CuentaMapper
import com.banking.demo.cuenta.application.service.impl.CuentaServiceImpl
import com.banking.demo.cuenta.domain.exception.DuplicateResourceException
import com.banking.demo.cuenta.domain.exception.EntityNotFoundException
import com.banking.demo.cuenta.domain.model.ClienteSnapshot
import com.banking.demo.cuenta.domain.model.Cuenta
import com.banking.demo.cuenta.domain.model.TipoCuenta
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository
import com.banking.demo.cuenta.domain.repository.CuentaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject

class CuentaServiceSpec extends Specification {

    CuentaRepository cuentaRepository = Mock()
    ClienteSnapshotRepository clienteSnapshotRepository = Mock()
    CuentaMapper cuentaMapper = new CuentaMapper()

    @Subject
    CuentaService cuentaService = new CuentaServiceImpl(
            cuentaRepository,
            clienteSnapshotRepository,
            cuentaMapper
    )

    ClienteSnapshot sampleSnapshot

    def setup() {
        SecurityContextHolder.clearContext()
        sampleSnapshot = ClienteSnapshot.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(true)
                .build()
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "deberia crear una cuenta exitosamente inicializando saldoActual igual a saldoInicial"() {
        given: "Un DTO de solicitud de cuenta valido"
        def request = CuentaRequestDto.builder()
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        cuentaRepository.existsByNumeroCuenta("478758") >> false
        clienteSnapshotRepository.findById(1L) >> Optional.of(sampleSnapshot)

        when: "Se crea la cuenta"
        CuentaResponseDto response = cuentaService.create(request)

        then: "La cuenta se crea correctamente con saldoActual igual al saldoInicial"
        1 * cuentaRepository.save(_ as Cuenta) >> { Cuenta c ->
            c.id = 10L
            return c
        }
        response != null
        response.id == 10L
        response.numeroCuenta == "478758"
        response.tipoCuenta == TipoCuenta.AHORROS
        response.saldoInicial.compareTo(new BigDecimal("2000.00")) == 0
        response.saldoActual.compareTo(new BigDecimal("2000.00")) == 0
        response.estado == true
        response.clienteId == 1L
        response.nombreCliente == "Jose Lema"
    }

    def "deberia rechazar la creacion si el cliente autenticado no coincide con el cliente de la cuenta"() {
        given: "Un usuario autenticado diferente al cliente de la cuenta"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken("2", null, Collections.emptyList())

        def request = CuentaRequestDto.builder()
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .clienteId(1L)
                .build()

        when: "Se intenta crear la cuenta para otro cliente"
        cuentaService.create(request)

        then: "Se lanza AccessDeniedException"
        def ex = thrown(AccessDeniedException)
        ex.message.contains("No tiene permisos para operar sobre una cuenta que no le pertenece")
    }

    def "deberia lanzar DuplicateResourceException si el numero de cuenta ya existe"() {
        given: "Un numero de cuenta que ya existe"
        def request = CuentaRequestDto.builder()
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .clienteId(1L)
                .build()

        cuentaRepository.existsByNumeroCuenta("478758") >> true

        when: "Se intenta registrar la cuenta duplicada"
        cuentaService.create(request)

        then: "Se lanza DuplicateResourceException"
        def ex = thrown(DuplicateResourceException)
        ex.message.contains("Ya existe una cuenta con el numero: 478758")
    }

    def "deberia lanzar EntityNotFoundException si el cliente no existe en cliente_snapshot"() {
        given: "Un cliente no registrado en el snapshot"
        def request = CuentaRequestDto.builder()
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .clienteId(999L)
                .build()

        cuentaRepository.existsByNumeroCuenta("478758") >> false
        clienteSnapshotRepository.findById(999L) >> Optional.empty()

        when: "Se intenta registrar la cuenta"
        cuentaService.create(request)

        then: "Se lanza EntityNotFoundException"
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("No se encontro el cliente con identificador: 999")
    }

    def "deberia listar las cuentas asociadas a un clienteId"() {
        given: "Una cuenta asociada al cliente 1"
        def c1 = Cuenta.builder()
                .id(1L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoActual(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        clienteSnapshotRepository.findById(1L) >> Optional.of(sampleSnapshot)
        cuentaRepository.findByClienteId(1L) >> [c1]

        when: "Se consultan las cuentas del cliente"
        List<CuentaResponseDto> list = cuentaService.findByClienteId(1L)

        then: "Retorna la lista de cuentas con nombre del cliente"
        list.size() == 1
        list[0].numeroCuenta == "478758"
        list[0].nombreCliente == "Jose Lema"
    }

    def "deberia consultar detalle de cuenta por numero de cuenta"() {
        given: "Una cuenta existente"
        def c = Cuenta.builder()
                .id(1L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoActual(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(c)
        clienteSnapshotRepository.findById(1L) >> Optional.of(sampleSnapshot)

        when: "Se consulta la cuenta por su numero"
        CuentaResponseDto response = cuentaService.findByNumeroCuenta("478758")

        then: "Se retorna el detalle de la cuenta con los datos mapeados"
        response != null
        response.numeroCuenta == "478758"
        response.nombreCliente == "Jose Lema"
    }

    def "deberia rechazar la consulta de cuenta si no pertenece al usuario autenticado"() {
        given: "Un usuario autenticado diferente al dueno de la cuenta"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken("2", null, Collections.emptyList())

        def c = Cuenta.builder()
                .id(1L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoActual(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(c)

        when: "Se consulta la cuenta ajena"
        cuentaService.findByNumeroCuenta("478758")

        then: "Lanza AccessDeniedException"
        def ex = thrown(AccessDeniedException)
        ex.message.contains("No tiene permisos para operar sobre una cuenta que no le pertenece")
    }

    def "deberia actualizar cuenta con PUT"() {
        given: "Una cuenta existente y un DTO de actualizacion"
        def c = Cuenta.builder()
                .id(1L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoActual(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        def updateDto = CuentaUpdateDto.builder()
                .tipoCuenta(TipoCuenta.CORRIENTE)
                .estado(false)
                .build()

        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(c)
        cuentaRepository.save(_ as Cuenta) >> { Cuenta saved -> saved }
        clienteSnapshotRepository.findById(1L) >> Optional.of(sampleSnapshot)

        when: "Se ejecuta la actualizacion completa"
        CuentaResponseDto updated = cuentaService.update("478758", updateDto)

        then: "Los valores son actualizados en la entidad persistida"
        updated.tipoCuenta == TipoCuenta.CORRIENTE
        updated.estado == false
    }

    def "deberia actualizar parcialmente cuenta con PATCH"() {
        given: "Una cuenta existente y un DTO parcial"
        def c = Cuenta.builder()
                .id(1L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoActual(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        def patchDto = CuentaPatchDto.builder()
                .estado(false)
                .build()

        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(c)
        cuentaRepository.save(_ as Cuenta) >> { Cuenta saved -> saved }
        clienteSnapshotRepository.findById(1L) >> Optional.of(sampleSnapshot)

        when: "Se ejecuta la actualizacion parcial"
        CuentaResponseDto patched = cuentaService.patch("478758", patchDto)

        then: "Solo se actualiza el estado, manteniendo el tipo de cuenta intacto"
        patched.estado == false
        patched.tipoCuenta == TipoCuenta.AHORROS
    }
}
