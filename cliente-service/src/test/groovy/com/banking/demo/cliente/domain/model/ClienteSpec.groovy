package com.banking.demo.cliente.domain.model

import com.banking.demo.cliente.application.dto.ClienteRequestDto
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime

class ClienteSpec extends Specification {

    @Shared
    Validator validator

    def setupSpec() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    def "deberia instanciar un Cliente y verificar herencia de Persona"() {
        given: "Datos para construir un cliente"
        def cliente = Cliente.builder()
                .id(1L)
                .nombre("Jose Lema")
                .genero("Masculino")
                .edad(35)
                .identificacion("1234567890")
                .direccion("Otavalo sn y principal")
                .telefono("098254785")
                .contrasena("1234")
                .estado(true)
                .build()

        expect: "El cliente hereda correctamente de Persona y retiene todos sus atributos"
        cliente instanceof Persona
        cliente.id == 1L
        cliente.nombre == "Jose Lema"
        cliente.genero == "Masculino"
        cliente.edad == 35
        cliente.identificacion == "1234567890"
        cliente.direccion == "Otavalo sn y principal"
        cliente.telefono == "098254785"
        cliente.contrasena == "1234"
        cliente.estado == true
    }

    def "deberia inicializar estado activo y fechaCreacion en onCreate cuando son nulos"() {
        given: "Un cliente sin estado ni fechaCreacion definidos"
        def cliente = new Cliente()
        cliente.nombre = "Marianela Montalvo"
        cliente.identificacion = "098254785"
        cliente.contrasena = "5678"

        when: "Se ejecuta el callback de ciclo de vida onCreate"
        cliente.onCreate()

        then: "El cliente se inicializa en estado activo y con fecha de creacion no nula"
        cliente.estado == Boolean.TRUE
        cliente.fechaCreacion != null
        cliente.fechaCreacion.isBefore(LocalDateTime.now().plusSeconds(1))
    }

    def "deberia actualizar fechaModificacion al invocar onUpdate"() {
        given: "Un cliente existente"
        def cliente = Cliente.builder()
                .nombre("Juan Osorio")
                .identificacion("097548965")
                .contrasena("1245")
                .estado(true)
                .build()

        when: "Se ejecuta el callback onUpdate"
        cliente.onUpdate()

        then: "La fecha de modificacion se asigna correctamente"
        cliente.fechaModificacion != null
    }

    @Unroll
    def "no deberia permitir cliente con contrasena invalida: '#passwordDescripcion'"() {
        given: "Un DTO de creacion de cliente con contrasena nula o en blanco"
        def dto = ClienteRequestDto.builder()
                .nombre("Test Name")
                .identificacion("1104589632")
                .edad(25)
                .contrasena(contrasenaInvalida)
                .estado(true)
                .build()

        when: "Se valida el DTO"
        def violations = validator.validate(dto)

        then: "Existe al menos una violacion en el campo contrasena con mensaje obligatorio"
        violations.any { it.propertyPath.toString() == "contrasena" && it.message == "La contraseña es obligatoria" }

        where:
        passwordDescripcion | contrasenaInvalida
        "nula"              | null
        "vacia"             | ""
        "solo espacios"     | "   "
    }
}
