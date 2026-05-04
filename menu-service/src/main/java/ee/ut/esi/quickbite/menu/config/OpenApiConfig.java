package ee.ut.esi.quickbite.menu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuOpenApi() {
        return new OpenAPI().info(new Info()
            .title("QuickBite Menu Service")
            .description("Manages menu items and batch validation for Order Service.")
            .version("v1"));
    }
}
