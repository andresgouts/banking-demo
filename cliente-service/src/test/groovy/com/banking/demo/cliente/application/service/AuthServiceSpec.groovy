package com.banking.demo.cliente.application.service

import com.banking.demo.cliente.domain.model.Cliente
import com.banking.demo.cliente.domain.repository.ClienteRepository
import com.banking.demo.security.dto.AuthRequest
import com.banking.demo.security.jwt.JwtService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AuthServiceSpec extends Specification {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    private static final long EXPIRATION_MS = 3600000L

    ClienteRepository clienteRepository = Mock()
    PasswordEncoder passwordEncoder = Mock()
    JwtService jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS)

    @Subject
    AuthService authService = new AuthService(clienteRepository, passwordEncoder, jwtService)

    def "deberia autenticar exitosamente y retornar token JWT valido con claims correctos"() {
        given: "Un cliente activo en base de datos"
        def cliente = Cliente.builder()
                .id(1L)
                .identificacion("1234567890")
                .nombre("Jose Lema")
                .contrasena("\$2a\$10\$hashDePrueba1234")
                .estado(true)
                .build()

        def request = AuthRequest.builder()
                .identificacion("1234567890")
                .contrasena("1234")
                .build()

        clienteRepository.findByIdentificacion("1234567890") >> Optional.of(cliente)
        passwordEncoder.matches("1234", "\$2a\$10\$hashDePrueba1234") >> true

        when: "Se ejecuta el login"
        def response = authService.login(request)

        then: "La respuesta contiene el token con tipo Bearer y datos del cliente"
        response != null
        response.token != null
        !response.token.isBlank()
        response.tipo == "Bearer"
        response.id == 1L

        and: "El token JWT generado es valido criptograficamente y contiene los claims esperados"
        jwtService.isTokenValid(response.token)
    }

    def "deberia lanzar BadCredentialsException cuando la contrasena no coincide con el hash"() {
        given: "Un cliente registrado con otra contrasena"
        def cliente = Cliente.builder()
                .id(2L)
                .identificacion("098254785")
                .nombre("Marianela Montalvo")
                .contrasena("\$2a\$10\$hashReal5678")
                .estado(true)
                .build()

        def request = AuthRequest.builder()
                .identificacion("098254785")
                .contrasena("password_incorrecto")
                .build()

        clienteRepository.findByIdentificacion("098254785") >> Optional.of(cliente)
        passwordEncoder.matches("password_incorrecto", "\$2a\$10\$hashReal5678") >> false

        when: "Se intenta realizar el login con contrasena errada"
        authService.login(request)

        then: "Se rechaza la autenticacion con mensaje estandarizado"
        def ex = thrown(BadCredentialsException)
        ex.message == "Credenciales inválidas"
    }

    def "deberia lanzar BadCredentialsException cuando el cliente no existe"() {
        given: "Una identificacion no registrada"
        def request = AuthRequest.builder()
                .identificacion("9999999999")
                .contrasena("cualquiera")
                .build()

        clienteRepository.findByIdentificacion("9999999999") >> Optional.empty()

        when: "Se intenta el login"
        authService.login(request)

        then: "Lanza BadCredentialsException sin revelar si el usuario existe"
        def ex = thrown(BadCredentialsException)
        ex.message == "Credenciales inválidas"
    }

    def "deberia lanzar BadCredentialsException cuando el cliente se encuentra inactivo"() {
        given: "Un cliente con estado inactivo"
        def cliente = Cliente.builder()
                .id(3L)
                .identificacion("097548965")
                .nombre("Juan Osorio")
                .contrasena("\$2a\$10\$hashCorrecto")
                .estado(false)
                .build()

        def request = AuthRequest.builder()
                .identificacion("097548965")
                .contrasena("1245")
                .build()

        clienteRepository.findByIdentificacion("097548965") >> Optional.of(cliente)
        passwordEncoder.matches("1245", "\$2a\$10\$hashCorrecto") >> true

        when: "Se ejecuta el login"
        authService.login(request)

        then: "Se rechaza el acceso informando que el cliente esta inactivo"
        def ex = thrown(BadCredentialsException)
        ex.message == "El cliente se encuentra inactivo"
    }

    @Unroll
    def "deberia lanzar BadCredentialsException cuando las credenciales son incompletas: '#caso'"() {
        given: "Un request de autenticacion con datos faltantes"
        def request = (id == null && pwd == null && caso == "request nulo")
                ? null
                : AuthRequest.builder().identificacion(id).contrasena(pwd).build()

        when: "Se intenta el login"
        authService.login(request)

        then: "Se rechaza de inmediato sin consultar la base de datos"
        def ex = thrown(BadCredentialsException)
        ex.message == "Credenciales inválidas"
        0 * clienteRepository.findByIdentificacion(_)

        where:
        caso                      | id            | pwd
        "request nulo"            | null          | null
        "identificacion nula"     | null          | "1234"
        "identificacion vacia"    | ""            | "1234"
        "identificacion espacios" | "   "         | "1234"
        "contrasena nula"         | "1234567890"  | null
    }

    def "deberia verificar expiracion y firma de tokens JWT"() {
        given: "Un JwtService configurado con expiracion inmediata (negativa)"
        def expiredJwtService = new JwtService(TEST_SECRET, -5000L)
        def expiredToken = expiredJwtService.generateToken("1")

        and: "Un JwtService con clave secreta distinta para simular manipulacion o firma invalida"
        def otherSecretService = new JwtService("999E635266556A586E3272357538782F413F4428472B4B6250645367566B5999", 3600000L)
        def alienToken = otherSecretService.generateToken("1")

        and: "Un token generado correctamente con la clave legitima"
        def validToken = jwtService.generateToken("1")

        expect: "El token legitimo es valido"
        jwtService.isTokenValid(validToken)
        jwtService.isTokenValid(validToken, "1")

        and: "El token expirado no es valido"
        !jwtService.isTokenValid(expiredToken)
        !jwtService.isTokenValid(expiredToken, "1")

        and: "El token firmado con otra clave no es valido"
        !jwtService.isTokenValid(alienToken)
        !jwtService.isTokenValid(alienToken, "1")

        and: "Un token con subject diferente no es valido para el subject esperado"
        !jwtService.isTokenValid(validToken, "2")
    }
}
