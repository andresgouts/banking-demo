package com.banking.demo.cliente.infrastructure.adapter.in.rest

import com.banking.demo.cliente.application.dto.ClientePatchDto
import com.banking.demo.cliente.application.dto.ClienteRequestDto
import com.banking.demo.cliente.application.dto.ClienteResponseDto
import com.banking.demo.cliente.application.dto.ClienteUpdateDto
import com.banking.demo.cliente.application.service.ClienteService
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import spock.lang.Specification
import spock.lang.Subject

class ClienteControllerSpec extends Specification {

    ClienteService clienteService = Mock()

    @Subject
    ClienteController controller = new ClienteController(clienteService)

    def setup() {
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI("/api/clientes")
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    def "getById deberia invocar clienteService.findById con el ID de la entidad y retornar 200 OK"() {
        given: "Un ID de tipo Long"
        Long id = 1L
        def responseDto = ClienteResponseDto.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("1234567890")
                .estado(true)
                .build()

        when: "Se invoca getById con el ID de la entidad"
        def response = controller.getById(id)

        then: "Retorna 200 OK y el DTO esperado"
        1 * clienteService.findById(1L) >> responseDto
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.id == 1L
        response.body.nombre == "Jose Lema"
    }

    def "create deberia registrar el cliente y retornar 201 Created con location"() {
        given: "Un DTO de creacion"
        def requestDto = ClienteRequestDto.builder()
                .nombre("Jose Lema")
                .identificacion("1234567890")
                .build()

        def responseDto = ClienteResponseDto.builder()
                .id(5L)
                .nombre("Jose Lema")
                .identificacion("1234567890")
                .build()

        when: "Se invoca create"
        def response = controller.create(requestDto)

        then: "Retorna 201 Created"
        1 * clienteService.create(requestDto) >> responseDto
        response.statusCode == HttpStatus.CREATED
        response.body.id == 5L
    }

    def "update deberia recibir el ID de tipo Long y actualizar el cliente"() {
        given: "Un ID y un DTO de actualizacion"
        Long id = 1L
        def updateDto = ClienteUpdateDto.builder().nombre("Jose Lema Modificado").build()
        def responseDto = ClienteResponseDto.builder().id(1L).nombre("Jose Lema Modificado").build()

        when: "Se invoca update"
        def response = controller.update(id, updateDto)

        then: "Retorna 200 OK"
        1 * clienteService.update(1L, updateDto) >> responseDto
        response.statusCode == HttpStatus.OK
        response.body.nombre == "Jose Lema Modificado"
    }

    def "patch deberia recibir el ID de tipo Long y aplicar cambios parciales"() {
        given: "Un ID y un DTO de parche"
        Long id = 1L
        def patchDto = ClientePatchDto.builder().telefono("099999999").build()
        def responseDto = ClienteResponseDto.builder().id(1L).telefono("099999999").build()

        when: "Se invoca patch"
        def response = controller.patch(id, patchDto)

        then: "Retorna 200 OK"
        1 * clienteService.patch(1L, patchDto) >> responseDto
        response.statusCode == HttpStatus.OK
        response.body.telefono == "099999999"
    }

    def "delete deberia recibir el ID de tipo Long y retornar 204 No Content"() {
        given: "Un ID de tipo Long"
        Long id = 1L

        when: "Se invoca delete"
        def response = controller.delete(id)

        then: "Retorna 204 No Content"
        1 * clienteService.delete(1L)
        response.statusCode == HttpStatus.NO_CONTENT
    }
}
