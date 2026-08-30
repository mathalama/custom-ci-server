package dev.mathalama.rabotyagaci.system.controller;

import dev.mathalama.rabotyagaci.common.api.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Value("${rabotyagaci.version:0.1.0}")
    private String version;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("version", version);
        info.put("status", "UP");
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
