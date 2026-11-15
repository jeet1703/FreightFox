package com.example.alumni.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param apiKey        PhantomBuster API key (sent as X-Phantombuster-Key)
 * @param agentId       id of a "LinkedIn Search Export" phantom in your PhantomBuster workspace
 * @param sessionCookie LinkedIn li_at cookie; if blank, the one saved in the phantom's setup is used
 * @param baseUrl       PhantomBuster API base URL
 * @param maxProfiles   how many profiles to request per search
 * @param pollInterval  delay between status checks while the phantom runs
 * @param timeout       how long to wait for the phantom to finish
 */
@ConfigurationProperties(prefix = "phantombuster")
public record PhantomBusterProperties(
		String apiKey,
		String agentId,
		String sessionCookie,
		@DefaultValue("https://api.phantombuster.com/api/v2") String baseUrl,
		@DefaultValue("25") int maxProfiles,
		@DefaultValue("5s") Duration pollInterval,
		@DefaultValue("3m") Duration timeout) {
}
