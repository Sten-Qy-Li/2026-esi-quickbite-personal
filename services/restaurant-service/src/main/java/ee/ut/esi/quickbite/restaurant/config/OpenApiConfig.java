package ee.ut.esi.quickbite.restaurant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI restaurantOpenApi() {
        return new OpenAPI().info(new Info()
            .title("QuickBite Restaurant Service")
            .description("Manages restaurant profiles, locations, and open/closed status.")
            .version("v1"));
    }
}
