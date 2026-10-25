package com.banking.demo.cliente.cucumber;

import com.banking.demo.cliente.application.dto.ClienteRequestDto;
import com.banking.demo.cliente.domain.model.Cliente;
import com.banking.demo.cliente.domain.repository.ClienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CreacionClientesSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> lastResponse;
    private String lastIdentificacion;

    @Before
    public void setUp() {
        clienteRepository.deleteAll();
    }

    @Cuando("se envía una solicitud POST a {string} para registrar un cliente con nombre {string}, identificación {string}, contraseña {string}, género {string}, edad {int}, dirección {string} y teléfono {string}")
    public void seEnviaUnaSolicitudPostParaRegistrarCliente(String path, String nombre, String identificacion,
                                                          String contrasena, String genero, int edad,
                                                          String direccion, String telefono) {
        this.lastIdentificacion = identificacion;
        ClienteRequestDto request = ClienteRequestDto.builder()
                .nombre(nombre)
                .identificacion(identificacion)
                .contrasena(contrasena)
                .genero(genero)
                .edad(edad)
                .direccion(direccion)
                .telefono(telefono)
                .build();

        lastResponse = restTemplate.postForEntity(path, request, String.class);
    }

    @Entonces("el sistema responde con código HTTP {int}")
    public void elSistemaRespondeConCodigoHttp(int expectedStatus) {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @Y("el cliente queda registrado en el sistema con estado activo")
    public void elClienteQuedaRegistradoEnElSistemaConEstadoActivo() {
        Optional<Cliente> clienteOpt = clienteRepository.findByIdentificacion(lastIdentificacion);
        assertThat(clienteOpt).isPresent();
        assertThat(clienteOpt.get().getEstado()).isTrue();
    }

    @Dado("que existe un cliente registrado previamente con identificación {string}")
    public void queExisteUnClienteRegistradoPreviamenteConIdentificacion(String identificacion) {
        this.lastIdentificacion = identificacion;
        Cliente cliente = Cliente.builder()
                .nombre("Jose Lema Previo")
                .identificacion(identificacion)
                .contrasena(passwordEncoder.encode("1234"))
                .genero("Masculino")
                .edad(35)
                .direccion("Otavalo")
                .telefono("098254785")
                .estado(true)
                .build();
        clienteRepository.save(cliente);
    }

    @Cuando("se envía una solicitud POST a {string} con la misma identificación {string}")
    public void seEnviaUnaSolicitudPostConLaMismaIdentificacion(String path, String identificacion) {
        ClienteRequestDto duplicateRequest = ClienteRequestDto.builder()
                .nombre("Jose Lema Duplicado")
                .identificacion(identificacion)
                .contrasena("5678")
                .genero("Masculino")
                .edad(35)
                .direccion("Otavalo")
                .telefono("098254785")
                .build();

        lastResponse = restTemplate.postForEntity(path, duplicateRequest, String.class);
    }

    @Y("se retorna un mensaje de error indicando duplicidad")
    public void seRetornaUnMensajeDeErrorIndicandoDuplicidad() throws Exception {
        assertThat(lastResponse.getBody()).isNotNull();
        JsonNode jsonNode = objectMapper.readTree(lastResponse.getBody());
        String mensaje = jsonNode.has("mensaje") ? jsonNode.get("mensaje").asText() : "";
        assertThat(mensaje).contains("Ya existe un cliente con la identificación");
    }
}
