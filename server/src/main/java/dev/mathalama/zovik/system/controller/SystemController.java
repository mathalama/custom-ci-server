package dev.mathalama.zovik.system.controller;

import dev.mathalama.zovik.common.api.dto.ApiResponse;
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

    @Value("${zovik.version:0.1.0}")
    private String version;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("version", version);
        info.put("status", "UP");
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
