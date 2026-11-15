package com.example.alumni.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HttpPhantomBusterClientTests {

	@Test
	void buildsLinkedInPeopleSearchUrl() {
		assertThat(HttpPhantomBusterClient.buildSearchUrl("University of XYZ", "Software Engineer", 2020))
				.isEqualTo("https://www.linkedin.com/search/results/people/?keywords="
						+ "Software%20Engineer%20%22University%20of%20XYZ%22%202020&origin=GLOBAL_SEARCH_HEADER");
	}

	@Test
	void mapsPhantomRowToProfile() {
		ScrapedProfile profile = HttpPhantomBusterClient.toProfile(Map.of(
				"fullName", "John Doe",
				"headline", "Passionate Software Engineer at XYZ Corp",
				"jobTitle", "Software Engineer",
				"location", "New York, NY",
				"school", "University of XYZ",
				"schoolDateRange", "2016 - 2020",
				"profileUrl", "https://www.linkedin.com/in/johndoe"));

		assertThat(profile).isEqualTo(new ScrapedProfile("John Doe", "Software Engineer", "University of XYZ",
				"New York, NY", "Passionate Software Engineer at XYZ Corp", 2020,
				"https://www.linkedin.com/in/johndoe"));
	}

	@Test
	void keepsOnlyRowsFromThisSearch() {
		Map<String, Object> mine = Map.of("fullName", "A", "query", "https://search/mine");
		Map<String, Object> stale = Map.of("fullName", "B", "query", "https://search/older");

		assertThat(HttpPhantomBusterClient.forThisSearch(List.of(mine, stale), "https://search/mine"))
				.containsExactly(mine);
		// rows that do not say which search they came from are all kept
		assertThat(HttpPhantomBusterClient.forThisSearch(List.of(Map.of("fullName", "C")), "https://search/mine"))
				.hasSize(1);
	}

	@Test
	void skipsRowsWithoutAName() {
		assertThat(HttpPhantomBusterClient.toProfile(Map.of("error", "No results found"))).isNull();
	}
}
