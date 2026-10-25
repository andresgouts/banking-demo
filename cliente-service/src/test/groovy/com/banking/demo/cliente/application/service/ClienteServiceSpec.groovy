package com.banking.demo.cliente.application.service

import com.banking.demo.cliente.application.dto.ClientePatchDto
import com.banking.demo.cliente.application.dto.ClienteRequestDto
import com.banking.demo.cliente.application.dto.ClienteUpdateDto
import com.banking.demo.cliente.application.mapper.ClienteMapper
import com.banking.demo.cliente.application.port.out.ClientEventPublisherPort
import com.banking.demo.cliente.application.service.impl.ClienteServiceImpl
import com.banking.demo.cliente.domain.exception.DuplicateResourceException
import com.banking.demo.cliente.domain.exception.EntityNotFoundException
import com.banking.demo.cliente.domain.model.Cliente
import com.banking.demo.cliente.domain.repository.ClienteRepository
import com.banking.demo.event.ClientEventDto
import com.banking.demo.event.ClientEventType
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject

class ClienteServiceSpec extends Specification {

    ClienteRepository clienteRepository = Mock()
    ClienteMapper clienteMapper = new ClienteMapper()
    PasswordEncoder passwordEncoder = Mock()
    ClientEventPublisherPort clientEventPublisher = Mock()

    @Subject
    ClienteService clienteService = new ClienteServiceImpl(
            clienteRepository,
            clienteMapper,
            passwordEncoder,
            clientEventPublisher
    )

    def "deberia crear un cliente exitosamente y publicar evento asincrono CLIENT_CREATED"() {
        given: "Un DTO valido para crear un cliente"
        def requestDto = ClienteRequestDto.builder()
                .nombre("Jose Lema")
                .genero("Masculino")
                .edad(35)
                .identificacion("1234567890")
                .direccion("Otavalo sn y principal")
                .telefono("098254785")
                .contrasena("1234")
                .estado(true)
                .build()

        clienteRepository.existsByIdentificacion("1234567890") >> false
        passwordEncoder.encode("1234") >> "\$2a\$10\$encodedPassword"

        clienteRepository.save(_ as Cliente) >> { Cliente c ->
            c.id = 1L
            return c
        }

        when: "Se ejecuta la creacion del cliente"
        def response = clienteService.create(requestDto)

        then: "El cliente es guardado y se retorna el DTO esperado"
        response != null
        response.id == 1L
        response.nombre == "Jose Lema"
        response.identificacion == "1234567890"
        response.estado == true

        and: "Se publica el evento asincrono con tipo CLIENT_CREATED"
        1 * clientEventPublisher.publishClientCreated({ ClientEventDto event ->
            event.id == 1L &&
                    event.identificacion == "1234567890" &&
                    event.nombre == "Jose Lema" &&
                    event.estado == true &&
                    event.eventType == ClientEventType.CLIENT_CREATED.name()
        })
    }

    def "deberia lanzar DuplicateResourceException cuando la identificacion ya existe"() {
        given: "Un DTO con identificacion duplicada"
        def requestDto = ClienteRequestDto.builder()
                .identificacion("1234567890")
                .contrasena("1234")
                .build()

        clienteRepository.existsByIdentificacion("1234567890") >> true

        when: "Se intenta crear el cliente"
        clienteService.create(requestDto)

        then: "Lanza DuplicateResourceException y no persiste ni publica eventos"
        def ex = thrown(DuplicateResourceException)
        ex.message.contains("Ya existe un cliente con la identificación: 1234567890")
        0 * clienteRepository.save(_)
        0 * clientEventPublisher.publishClientCreated(_)
    }

    def "deberia listar todos los clientes registrados"() {
        given: "Dos clientes en la base de datos"
        def c1 = Cliente.builder().id(1L).nombre("Jose").identificacion("111").estado(true).build()
        def c2 = Cliente.builder().id(2L).nombre("Marianela").identificacion("222").estado(true).build()

        clienteRepository.findAll() >> [c1, c2]

        when: "Se consultan todos los clientes"
        def result = clienteService.findAll()

        then: "Retorna la lista de clientes mapeados a DTOs"
        result.size() == 2
        result[0].id == 1L
        result[1].id == 2L
    }

    def "deberia encontrar cliente por ID de entidad"() {
        given: "Un cliente existente recuperable por PK"
        def cliente = Cliente.builder()
                .id(1L)
                .identificacion("1234567890")
                .nombre("Jose Lema")
                .estado(true)
                .build()

        clienteRepository.findById(1L) >> Optional.of(cliente)

        when: "Se busca por ID numerico 1L"
        def result = clienteService.findById(1L)

        then: "Retorna el cliente correspondiente y no busca por identificacion"
        result != null
        result.id == 1L
        result.nombre == "Jose Lema"
        0 * clienteRepository.findByIdentificacion(_)
    }

    def "deberia lanzar EntityNotFoundException cuando el cliente no existe por ID"() {
        given: "Un ID inexistente"
        clienteRepository.findById(999L) >> Optional.empty()

        when: "Se consulta por ID inexistente"
        clienteService.findById(999L)

        then: "Lanza EntityNotFoundException y no busca por identificacion"
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("Cliente no encontrado con ID: 999")
        0 * clienteRepository.findByIdentificacion(_)
    }

    def "deberia actualizar cliente exitosamente y emitir evento CLIENT_UPDATED"() {
        given: "Un cliente existente"
        def clienteExistente = Cliente.builder()
                .id(1L)
                .identificacion("1234567890")
                .nombre("Jose Lema")
                .direccion("Direccion Vieja")
                .telefono("098111111")
                .estado(true)
                .contrasena("oldPass")
                .build()

        def updateDto = ClienteUpdateDto.builder()
                .nombre("Jose Lema Modificado")
                .genero("Masculino")
                .edad(36)
                .identificacion("1234567890")
                .direccion("Direccion Nueva")
                .telefono("098222222")
                .contrasena("newPass")
                .estado(true)
                .build()

        clienteRepository.findById(1L) >> Optional.of(clienteExistente)
        passwordEncoder.encode("newPass") >> "encodedNewPass"
        clienteRepository.save(_ as Cliente) >> { Cliente c -> c }

        when: "Se actualiza el cliente por su ID"
        def response = clienteService.update(1L, updateDto)

        then: "Los datos se actualizan y se publica CLIENT_UPDATED"
        response.nombre == "Jose Lema Modificado"
        response.direccion == "Direccion Nueva"
        response.telefono == "098222222"

        1 * clientEventPublisher.publishClientUpdated({ ClientEventDto event ->
            event.id == 1L &&
                    event.nombre == "Jose Lema Modificado" &&
                    event.eventType == ClientEventType.CLIENT_UPDATED.name()
        })
    }

    def "deberia lanzar DuplicateResourceException al actualizar con identificacion de otro cliente"() {
        given: "Un cliente existente y un DTO con nueva identificacion ya en uso"
        def clienteExistente = Cliente.builder()
                .id(1L)
                .identificacion("1234567890")
                .build()

        def updateDto = ClienteUpdateDto.builder()
                .identificacion("098254785") // cambia a identificacion de otro
                .build()

        clienteRepository.findById(1L) >> Optional.of(clienteExistente)
        clienteRepository.existsByIdentificacion("098254785") >> true

        when: "Se intenta actualizar"
        clienteService.update(1L, updateDto)

        then: "Lanza DuplicateResourceException"
        def ex = thrown(DuplicateResourceException)
        ex.message.contains("Ya existe otro cliente con la identificación: 098254785")
        0 * clienteRepository.save(_)
        0 * clientEventPublisher.publishClientUpdated(_)
    }

    def "deberia aplicar cambios parciales (PATCH) y emitir evento CLIENT_UPDATED"() {
        given: "Un cliente existente"
        def clienteExistente = Cliente.builder()
                .id(1L)
                .identificacion("1234567890")
                .nombre("Jose Lema")
                .telefono("098111111")
                .estado(true)
                .build()

        def patchDto = ClientePatchDto.builder()
                .telefono("099999999")
                .estado(false)
                .build()

        clienteRepository.findById(1L) >> Optional.of(clienteExistente)
        clienteRepository.save(_ as Cliente) >> { Cliente c -> c }

        when: "Se aplica el patch"
        def response = clienteService.patch(1L, patchDto)

        then: "Solo los campos no nulos son actualizados"
        response.telefono == "099999999"
        response.estado == false
        response.nombre == "Jose Lema" // permanece intacto

        1 * clientEventPublisher.publishClientUpdated({ ClientEventDto event ->
            event.id == 1L &&
                    event.estado == false &&
                    event.eventType == ClientEventType.CLIENT_UPDATED.name()
        })
    }

    def "deberia realizar soft delete del cliente y emitir evento CLIENT_DELETED"() {
        given: "Un cliente activo"
        def cliente = Cliente.builder()
                .id(1L)
                .identificacion("1234567890")
                .nombre("Jose Lema")
                .estado(true)
                .build()

        clienteRepository.findById(1L) >> Optional.of(cliente)
        clienteRepository.save(_ as Cliente) >> { Cliente c -> c }

        when: "Se elimina el cliente por su ID"
        clienteService.delete(1L)

        then: "El estado pasa a false y se establece fechaEliminacion"
        cliente.estado == Boolean.FALSE
        cliente.fechaEliminacion != null

        1 * clientEventPublisher.publishClientDeleted({ ClientEventDto event ->
            event.id == 1L &&
                    event.estado == false &&
                    event.eventType == ClientEventType.CLIENT_DELETED.name()
        })
    }
}
