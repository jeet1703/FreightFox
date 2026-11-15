package com.example.alumni.client;

import java.util.List;

public interface PhantomBusterClient {

	/**
	 * Runs a LinkedIn people search for alumni of {@code university} currently working as
	 * {@code designation}, optionally narrowed to a pass-out year, and returns the scraped profiles.
	 */
	List<ScrapedProfile> searchAlumni(String university, String designation, Integer passoutYear);
}
