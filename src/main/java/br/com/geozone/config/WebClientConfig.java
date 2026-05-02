package br.com.geozone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .defaultHeader("User-Agent", "GeoZone/2.0 (contact@geozone.com.br)")
                .defaultHeader("Accept-Language", "pt-BR,pt;q=0.9")
                .build();
    }
}
