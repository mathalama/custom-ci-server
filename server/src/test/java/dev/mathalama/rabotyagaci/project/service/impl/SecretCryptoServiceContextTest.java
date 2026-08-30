package dev.mathalama.rabotyagaci.project.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.beans.factory.BeanCreationException;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCryptoServiceContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecretCryptoService.class, PropertyPlaceholderAutoConfiguration.class);

    @Test
    void contextFailsWithoutMasterKey() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            Throwable ex = context.getStartupFailure();
            System.out.println("ACTUAL EXCEPTION THROWN: " + ex.getClass().getName());
            System.out.println("ACTUAL EXCEPTION MESSAGE: " + ex.getMessage());
            assertThat(ex).isInstanceOf(org.springframework.beans.factory.BeanCreationException.class);
        });
    }
}
