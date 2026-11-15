package com.example.alumni.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Returns deterministic sample profiles so the API can be exercised without PhantomBuster credentials. */
@Component
@ConditionalOnProperty(name = "phantombuster.mock", havingValue = "true")
public class MockPhantomBusterClient implements PhantomBusterClient {

	private static final String[][] PEOPLE = {
			{ "John Doe", "New York, NY", "XYZ Corp" },
			{ "Jane Smith", "San Francisco, CA", "Acme Labs" },
			{ "Arjun Mehta", "Bengaluru, India", "Globex" },
	};

	@Override
	public List<ScrapedProfile> searchAlumni(String university, String designation, Integer passoutYear) {
		List<ScrapedProfile> profiles = new ArrayList<>();
		for (int i = 0; i < PEOPLE.length; i++) {
			String[] person = PEOPLE[i];
			Integer year = passoutYear != null ? passoutYear : Integer.valueOf(2020 - i);
			String slug = (person[0] + "-" + university + "-" + designation + "-" + year)
					.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
			profiles.add(new ScrapedProfile(person[0], designation, university, person[1],
					designation + " at " + person[2], year, "https://www.linkedin.com/in/mock-" + slug));
		}
		return profiles;
	}
}
