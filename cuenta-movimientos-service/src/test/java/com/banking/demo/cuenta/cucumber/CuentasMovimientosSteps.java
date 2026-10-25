package com.banking.demo.cuenta.cucumber;

import com.banking.demo.cuenta.application.dto.MovimientoRequestDto;
import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository;
import com.banking.demo.cuenta.domain.repository.CuentaRepository;
import com.banking.demo.cuenta.domain.repository.MovimientoRepository;
import com.banking.demo.security.jwt.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CuentasMovimientosSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private ClienteSnapshotRepository clienteSnapshotRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CucumberTestContext testContext;

    private String validAuthHeader;

    @Before
    public void setUp() {
        movimientoRepository.deleteAll();
        cuentaRepository.deleteAll();
        clienteSnapshotRepository.deleteAll();

        String token = jwtService.generateToken("99", Map.of("roles", List.of("ROLE_USER")));
        validAuthHeader = "Bearer " + token;

        // Registrar cliente por defecto para las pruebas de cuenta
        clienteSnapshotRepository.save(ClienteSnapshot.builder()
                .id(99L)
                .nombre("Cliente de Pruebas")
                .identificacion("1799999999")
                .estado(true)
                .build());
    }

    @Dado("que existe la cuenta {string} de tipo {string} con saldo inicial de {bigdecimal}")
    public void queExisteLaCuentaDeTipoConSaldoInicialDe(String numeroCuenta, String tipoCuentaStr, BigDecimal saldoInicial) {
        testContext.setCurrentNumeroCuenta(numeroCuenta);
        TipoCuenta tipo = TipoCuenta.valueOf(tipoCuentaStr.toUpperCase());
        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuenta)
                .tipoCuenta(tipo)
                .saldoInicial(saldoInicial)
                .saldoActual(saldoInicial)
                .estado(true)
                .clienteId(99L)
                .build();
        cuentaRepository.save(cuenta);
    }

    @Dado("que existe la cuenta {string} con saldo disponible de {bigdecimal}")
    public void queExisteLaCuentaConSaldoDisponibleDe(String numeroCuenta, BigDecimal saldo) {
        testContext.setCurrentNumeroCuenta(numeroCuenta);
        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuenta)
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(saldo)
                .saldoActual(saldo)
                .estado(true)
                .clienteId(99L)
                .build();
        cuentaRepository.save(cuenta);
    }

    @Cuando("realizo un movimiento de tipo {string} por valor de {bigdecimal}")
    public void realizoUnMovimientoDeTipoPorValorDe(String tipo, BigDecimal valor) {
        BigDecimal effectiveValue = "RETIRO".equalsIgnoreCase(tipo) && valor.compareTo(BigDecimal.ZERO) > 0
                ? valor.negate()
                : valor;

        MovimientoRequestDto request = MovimientoRequestDto.builder()
                .numeroCuenta(testContext.getCurrentNumeroCuenta())
                .valor(effectiveValue)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, validAuthHeader);
        HttpEntity<MovimientoRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/movimientos", entity, String.class);
        testContext.setLastResponse(response);
    }

    @Entonces("el sistema responde con código HTTP {int}")
    public void elSistemaRespondeConCodigoHttp(int expectedStatus) {
        assertThat(testContext.getLastResponse()).isNotNull();
        assertThat(testContext.getLastResponse().getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @Y("el nuevo saldo disponible es {bigdecimal}")
    public void elNuevoSaldoDisponibleEs(BigDecimal expectedSaldo) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(testContext.getLastResponse().getBody());
        BigDecimal saldoResponse = new BigDecimal(jsonNode.get("saldo").asText());
        assertThat(saldoResponse).isEqualByComparingTo(expectedSaldo);

        Cuenta updated = cuentaRepository.findByNumeroCuenta(testContext.getCurrentNumeroCuenta()).orElseThrow();
        assertThat(updated.getSaldoActual()).isEqualByComparingTo(expectedSaldo);
    }

    @Y("el mensaje de error es {string}")
    public void elMensajeDeErrorEs(String expectedMensaje) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(testContext.getLastResponse().getBody());
        String mensaje = jsonNode.has("mensaje") ? jsonNode.get("mensaje").asText() : "";
        assertThat(mensaje).isEqualTo(expectedMensaje);
    }
}
