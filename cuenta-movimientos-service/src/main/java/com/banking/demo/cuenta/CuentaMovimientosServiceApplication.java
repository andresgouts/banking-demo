package com.banking.demo.cuenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.banking.demo.cuenta", "com.banking.demo.security"})
public class CuentaMovimientosServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CuentaMovimientosServiceApplication.class, args);
    }
}
