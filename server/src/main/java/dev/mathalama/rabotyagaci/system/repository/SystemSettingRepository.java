package dev.mathalama.rabotyagaci.system.repository;

import dev.mathalama.rabotyagaci.system.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
