package com.cgaraydev.incidentapi.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI incidentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Incident API")
                        .version("0.0.1")
                        .description("""
                                API de gestión de incidencias técnicas.
                                Permite crear, consultar y filtrar incidencias,
                                cambiar su estado y solicitar sugerencias
                                mediante Gemini.

                                El análisis no modifica la incidencia.
                                Utilizar únicamente datos ficticios.
                                """))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Servidor actual")));
    }
}