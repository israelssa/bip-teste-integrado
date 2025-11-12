package com.exemple.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Servidor de Desenvolvimento");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.seudominio.com");
        prodServer.setDescription("Servidor de Produção");

        Contact contact = new Contact();
        contact.setEmail("seu-email@empresa.com");
        contact.setName("Sua Empresa");
        contact.setUrl("https://www.seudominio.com");

        License mitLicense = new License()
            .name("MIT License")
            .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
            .title("API de Benefícios")
            .version("1.0")
            .contact(contact)
            .description("Esta API expõe endpoints para gerenciamento de benefícios com diferentes estratégias de controle de concorrência.")
            .license(mitLicense);

        return new OpenAPI()
            .info(info)
            .servers(List.of(devServer, prodServer));
    }
}