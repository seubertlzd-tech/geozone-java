package br.com.geozone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GeozoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeozoneApplication.class, args);
        System.out.println("""
            ╔══════════════════════════════════════════╗
            ║   GeoZone v2.0 — API Java iniciada!      ║
            ║   URL:      http://localhost:8080         ║
            ║   H2:       http://localhost:8080/h2-console
            ║   Swagger:  http://localhost:8080/api/info║
            ╚══════════════════════════════════════════╝
            """);
    }
}
