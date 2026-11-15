package com.example.alumni.client;

/** One LinkedIn profile as returned by a PhantomBuster search, normalised to the fields this app uses. */
public record ScrapedProfile(
		String name,
		String currentRole,
		String school,
		String location,
		String headline,
		Integer passoutYear,
		String profileUrl) {
}
