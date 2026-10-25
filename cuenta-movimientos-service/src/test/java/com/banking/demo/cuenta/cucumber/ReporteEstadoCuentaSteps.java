package com.banking.demo.cuenta.cucumber;

import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.model.Movimiento;
import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.banking.demo.cuenta.domain.model.TipoMovimiento;
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository;
import com.banking.demo.cuenta.domain.repository.CuentaRepository;
import com.banking.demo.cuenta.domain.repository.MovimientoRepository;
import com.banking.demo.security.jwt.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ReporteEstadoCuentaSteps {

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

    private String getAuthHeader() {
        String token = jwtService.generateToken("admin-user", Map.of("roles", List.of("ROLE_USER")));
        return "Bearer " + token;
    }

    @Dado("que existe el cliente {string} con identificación {string} y id {long}")
    public void queExisteElClienteConIdentificacionYId(String nombre, String identificacion, Long id) {
        clienteSnapshotRepository.save(ClienteSnapshot.builder()
                .id(id)
                .nombre(nombre)
                .identificacion(identificacion)
                .estado(true)
                .build());
    }

    @Dado("la cuenta {string} de tipo {string} con saldo inicial de {bigdecimal} asociada al cliente {long}")
    public void laCuentaDeTipoConSaldoInicialDeAsociadaAlCliente(String numeroCuenta, String tipoCuentaStr, BigDecimal saldoInicial, Long clienteId) {
        TipoCuenta tipo = TipoCuenta.valueOf(tipoCuentaStr.toUpperCase());
        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuenta)
                .tipoCuenta(tipo)
                .saldoInicial(saldoInicial)
                .saldoActual(saldoInicial)
                .estado(true)
                .clienteId(clienteId)
                .build();
        cuentaRepository.save(cuenta);
    }

    @Dado("se registró un movimiento en la cuenta {string} de tipo {string} por valor de {bigdecimal} con fecha {string} resultando en saldo disponible de {bigdecimal}")
    public void seRegistroUnMovimientoEnLaCuentaDeTipoPorValorDeConFechaResultandoEnSaldoDisponibleDe(
            String numeroCuenta, String tipoMovimientoStr, BigDecimal valor, String fechaStr, BigDecimal saldoDisponible) {

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta).orElseThrow();
        LocalDate localDate = LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalDateTime fechaHora = localDate.atTime(LocalTime.of(10, 0));

        TipoMovimiento tipo = TipoMovimiento.valueOf(tipoMovimientoStr.toUpperCase());

        Movimiento movimiento = Movimiento.builder()
                .cuenta(cuenta)
                .numeroCuenta(numeroCuenta)
                .tipoMovimiento(tipo)
                .valor(valor)
                .saldo(saldoDisponible)
                .fecha(fechaHora)
                .build();
        movimientoRepository.save(movimiento);

        cuenta.setSaldoActual(saldoDisponible);
        cuentaRepository.save(cuenta);
    }

    @Cuando("consulto el reporte de estado de cuenta para el cliente con id {string} entre {string} y {string}")
    public void consultoElReporteDeEstadoDeCuentaParaElClienteConIdEntreY(String clienteId, String fechaInicio, String fechaFin) {
        consultarReporte(clienteId, fechaInicio, fechaFin);
    }

    private void consultarReporte(String clienteId, String fechaInicio, String fechaFin) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, getAuthHeader());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "/api/reportes?fechaInicio=" + fechaInicio + "&fechaFin=" + fechaFin + "&clienteId=" + clienteId;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        testContext.setLastResponse(response);
    }

    @Y("el reporte contiene {int} cuentas")
    public void elReporteContieneCuentas(int cantidadEsperada) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(jsonNode.isArray()).isTrue();
        assertThat(jsonNode.size()).isEqualTo(cantidadEsperada);
    }

    @Y("la cuenta {string} tiene saldo {bigdecimal} con {int} movimientos donde el movimiento {int} tiene saldo inicial {bigdecimal}, valor {bigdecimal} y saldo disponible {bigdecimal}")
    public void laCuentaTieneSaldoConMovimientosDondeElMovimientoTiene(
            String numeroCuenta, BigDecimal saldoEsperado, int cantMovs, int movIndex,
            BigDecimal saldoInicialEsperado, BigDecimal movimientoEsperado, BigDecimal saldoDisponibleEsperado) throws Exception {

        JsonNode jsonNode = objectMapper.readTree(testContext.getLastResponse().getBody());
        assertThat(jsonNode.isArray()).isTrue();

        boolean found = false;
        for (JsonNode item : jsonNode) {
            if (numeroCuenta.equals(item.path("numeroCuenta").asText())) {
                found = true;
                String saldoStr = item.has("saldo") ? item.path("saldo").asText() : item.path("Saldo").asText();
                BigDecimal saldo = new BigDecimal(saldoStr);
                assertThat(saldo).isEqualByComparingTo(saldoEsperado);

                JsonNode movs = item.has("movimientos") ? item.path("movimientos") : item.path("Movimientos");
                assertThat(movs.isArray()).isTrue();
                assertThat(movs.size()).isEqualTo(cantMovs);

                JsonNode mov = movs.get(movIndex - 1);
                BigDecimal saldoInicial = new BigDecimal(mov.path("saldoInicial").asText());
                BigDecimal movimiento = new BigDecimal(mov.path("movimiento").asText());
                BigDecimal saldoDisponible = new BigDecimal(mov.path("saldoDisponible").asText());

                assertThat(saldoInicial).isEqualByComparingTo(saldoInicialEsperado);
                assertThat(movimiento).isEqualByComparingTo(movimientoEsperado);
                assertThat(saldoDisponible).isEqualByComparingTo(saldoDisponibleEsperado);
                break;
            }
        }
        assertThat(found).withFailMessage("No se encontró cuenta " + numeroCuenta).isTrue();
    }
}
