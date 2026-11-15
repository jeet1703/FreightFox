package com.example.alumni.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.alumni.config.PhantomBusterProperties;
import com.example.alumni.exception.PhantomBusterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Talks to the PhantomBuster v2 API:
 * <ol>
 * <li>POST /agents/launch - starts the LinkedIn Search Export phantom with a people-search URL</li>
 * <li>GET /containers/fetch - polled until the run is no longer "running"</li>
 * <li>GET /containers/fetch-result-object - the scraped profiles; if the phantom did not set a result
 * object, falls back to the result.json the phantom writes to its S3 folder (GET /agents/fetch)</li>
 * </ol>
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "phantombuster.mock", havingValue = "false", matchIfMissing = true)
public class HttpPhantomBusterClient implements PhantomBusterClient {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
	};
	private static final Pattern YEAR = Pattern.compile("(?<!\\d)(19|20)\\d{2}(?!\\d)");

	private final PhantomBusterProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient api;
	private final RestClient plain = RestClient.create();

	public HttpPhantomBusterClient(PhantomBusterProperties props, ObjectMapper objectMapper) {
		this.props = props;
		this.objectMapper = objectMapper;
		this.api = RestClient.builder()
				.baseUrl(props.baseUrl())
				.defaultHeader("X-Phantombuster-Key", props.apiKey() == null ? "" : props.apiKey())
				.build();
	}

	@Override
	public List<ScrapedProfile> searchAlumni(String university, String designation, Integer passoutYear) {
		if (!StringUtils.hasText(props.apiKey()) || !StringUtils.hasText(props.agentId())) {
			throw new PhantomBusterException("PhantomBuster is not configured: set PHANTOMBUSTER_API_KEY and "
					+ "PHANTOMBUSTER_AGENT_ID (or PHANTOMBUSTER_MOCK=true to use sample data)");
		}
		try {
			String searchUrl = buildSearchUrl(university, designation, passoutYear);
			String containerId = launch(searchUrl);
			log.info("Launched PhantomBuster agent {} (container {})", props.agentId(), containerId);
			waitUntilFinished(containerId);
			List<ScrapedProfile> profiles = new ArrayList<>();
			for (Map<String, Object> row : forThisSearch(fetchResults(containerId), searchUrl)) {
				ScrapedProfile profile = toProfile(row);
				if (profile != null) {
					profiles.add(profile);
				}
			}
			return profiles;
		}
		// The upstream error text stays in the server log (as the cause), not in the API response.
		catch (RestClientResponseException e) {
			throw new PhantomBusterException("PhantomBuster request failed (HTTP " + e.getStatusCode().value() + ")", e);
		}
		catch (RestClientException | JacksonException e) {
			throw new PhantomBusterException("PhantomBuster request failed", e);
		}
	}

	/**
	 * The phantom's result.json can still hold rows from earlier runs. Each row names the search that
	 * produced it in "query", so when any row is from this search, only those are kept.
	 */
	static List<Map<String, Object>> forThisSearch(List<Map<String, Object>> rows, String searchUrl) {
		List<Map<String, Object>> own = rows.stream().filter(row -> searchUrl.equals(row.get("query"))).toList();
		return own.isEmpty() ? rows : own;
	}

	static String buildSearchUrl(String university, String designation, Integer passoutYear) {
		String keywords = designation.trim() + " \"" + university.trim() + "\""
				+ (passoutYear != null ? " " + passoutYear : "");
		return "https://www.linkedin.com/search/results/people/?keywords="
				+ URLEncoder.encode(keywords, StandardCharsets.UTF_8).replace("+", "%20")
				+ "&origin=GLOBAL_SEARCH_HEADER";
	}

	private String launch(String searchUrl) {
		Map<String, Object> argument = new LinkedHashMap<>();
		argument.put("search", searchUrl);
		argument.put("category", "People");
		argument.put("numberOfResultsPerSearch", props.maxProfiles());
		argument.put("numberOfLinesPerLaunch", 1);
		if (StringUtils.hasText(props.sessionCookie())) {
			argument.put("sessionCookie", props.sessionCookie());
		}
		// bonusArgument is merged over the argument saved in the phantom's setup, so anything
		// configured there (session cookie, user agent, ...) is kept.
		Map<String, Object> body = Map.of("id", props.agentId(), "bonusArgument", argument);
		Map<String, Object> response = api.post().uri("/agents/launch")
				.contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(MAP);
		Object containerId = response == null ? null : response.get("containerId");
		if (containerId == null) {
			throw new PhantomBusterException("PhantomBuster did not return a containerId when launching the agent");
		}
		return containerId.toString();
	}

	private void waitUntilFinished(String containerId) {
		long deadline = System.nanoTime() + props.timeout().toNanos();
		while (true) {
			Map<String, Object> container = api.get().uri("/containers/fetch?id={id}", containerId)
					.retrieve().body(MAP);
			Object status = container == null ? null : container.get("status");
			if (status == null) {
				throw new PhantomBusterException("PhantomBuster returned no status for the run");
			}
			if (!"running".equalsIgnoreCase(status.toString())) {
				Object exitCode = container.get("exitCode");
				if (exitCode instanceof Number n && n.intValue() != 0) {
					throw new PhantomBusterException("PhantomBuster run failed (exit code " + n.intValue()
							+ "). Check the phantom's console in PhantomBuster - an expired LinkedIn session "
							+ "cookie is the usual cause.");
				}
				return;
			}
			if (System.nanoTime() > deadline) {
				throw new PhantomBusterException("Timed out after " + props.timeout().toSeconds()
						+ "s waiting for PhantomBuster to finish");
			}
			try {
				Thread.sleep(props.pollInterval().toMillis());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new PhantomBusterException("Interrupted while waiting for PhantomBuster", e);
			}
		}
	}

	private List<Map<String, Object>> fetchResults(String containerId) {
		Map<String, Object> response = api.get().uri("/containers/fetch-result-object?id={id}", containerId)
				.retrieve().body(MAP);
		Object resultObject = response == null ? null : response.get("resultObject");
		if (resultObject instanceof String json && StringUtils.hasText(json)) {
			List<Map<String, Object>> rows = parseRows(json);
			if (!rows.isEmpty()) {
				return rows;
			}
		}
		return fetchResultFile();
	}

	/** Search Export phantoms write their output to result.json in the agent's S3 folder. */
	private List<Map<String, Object>> fetchResultFile() {
		Map<String, Object> agent = api.get().uri("/agents/fetch?id={id}", props.agentId()).retrieve().body(MAP);
		Object orgFolder = agent == null ? null : agent.get("orgS3Folder");
		Object agentFolder = agent == null ? null : agent.get("s3Folder");
		if (orgFolder == null || agentFolder == null) {
			return List.of();
		}
		String url = "https://phantombuster.s3.amazonaws.com/" + orgFolder + "/" + agentFolder + "/result.json";
		try {
			String json = plain.get().uri(url).retrieve().body(String.class);
			return json == null ? List.of() : parseRows(json);
		}
		catch (RestClientException e) {
			log.warn("No result.json available at {}: {}", url, e.getMessage());
			return List.of();
		}
	}

	private List<Map<String, Object>> parseRows(String json) {
		String trimmed = json.trim();
		if (!trimmed.startsWith("[")) {
			return List.of();
		}
		return objectMapper.readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {
		});
	}

	/** Field names differ between phantom versions, so several candidates are tried for each value. */
	static ScrapedProfile toProfile(Map<String, Object> row) {
		String name = first(row, "fullName", "name");
		if (name == null) {
			String firstName = first(row, "firstName");
			String lastName = first(row, "lastName");
			if (firstName != null) {
				name = lastName == null ? firstName : firstName + " " + lastName;
			}
		}
		if (name == null || row.get("error") != null) {
			return null;
		}
		String headline = first(row, "headline", "linkedinHeadline", "title", "job", "additionalInfo");
		String role = first(row, "jobTitle", "currentJob", "job", "title", "headline");
		String schoolDates = first(row, "schoolDateRange", "schoolDates");
		return new ScrapedProfile(name, role, first(row, "school", "schoolName"), first(row, "location"),
				headline, lastYear(schoolDates), first(row, "profileUrl", "linkedinProfileUrl", "defaultProfileUrl"));
	}

	private static String first(Map<String, Object> row, String... keys) {
		for (String key : keys) {
			Object value = row.get(key);
			if (value != null && StringUtils.hasText(value.toString())) {
				return value.toString().trim();
			}
		}
		return null;
	}

	/** "2016 - 2020" -> 2020 */
	static Integer lastYear(String text) {
		if (text == null) {
			return null;
		}
		Integer year = null;
		Matcher matcher = YEAR.matcher(text);
		while (matcher.find()) {
			year = Integer.valueOf(matcher.group());
		}
		return year;
	}
}
