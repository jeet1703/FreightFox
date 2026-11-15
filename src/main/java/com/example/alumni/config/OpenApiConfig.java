package com.example.alumni.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown in Swagger UI (/swagger-ui/index.html). */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI alumniOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Alumni LinkedIn Profile Searcher API")
				.version("1.0")
				.description("Searches LinkedIn for alumni of a university through PhantomBuster "
						+ "and stores the results in PostgreSQL."));
	}
}
