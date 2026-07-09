package dev.mathalama.rabotyagaci.system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;
}
