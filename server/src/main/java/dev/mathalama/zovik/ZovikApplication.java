package dev.mathalama.zovik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZovikApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZovikApplication.class, args);
    }

}
