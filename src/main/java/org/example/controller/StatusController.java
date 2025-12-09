package org.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Check application status
        status.put("application", "running");
        status.put("message", "Aplicação iniciada com sucesso");
        
        // Check PostgreSQL connection
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                status.put("database", "connected");
                status.put("databaseMessage", "PostgreSQL conectado e funcionando");
                status.put("databaseType", connection.getMetaData().getDatabaseProductName());
                status.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            } else {
                status.put("database", "error");
                status.put("databaseMessage", "Conexão PostgreSQL inválida");
            }
        } catch (Exception e) {
            status.put("database", "error");
            status.put("databaseMessage", "Erro ao conectar ao PostgreSQL: " + e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
}
