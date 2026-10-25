package com.banking.demo.cuenta.cucumber;

import io.cucumber.spring.ScenarioScope;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Data
@Component
@ScenarioScope
public class CucumberTestContext {
    private ResponseEntity<String> lastResponse;
    private String currentNumeroCuenta;
}
