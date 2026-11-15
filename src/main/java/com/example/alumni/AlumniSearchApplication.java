package com.example.alumni;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlumniSearchApplication {

	public static void main(String[] args) {
		// Run in UTC: PostgreSQL rejects legacy JVM zone ids such as "Asia/Calcutta" at connect time.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(AlumniSearchApplication.class, args);
	}
}
