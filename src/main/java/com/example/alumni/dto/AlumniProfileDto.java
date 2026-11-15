package com.example.alumni.dto;

import com.example.alumni.model.AlumniProfile;

public record AlumniProfileDto(
		String name,
		String currentRole,
		String university,
		String location,
		String linkedinHeadline,
		Integer passoutYear) {

	public static AlumniProfileDto from(AlumniProfile p) {
		return new AlumniProfileDto(p.getName(), p.getCurrentRole(), p.getUniversity(), p.getLocation(),
				p.getLinkedinHeadline(), p.getPassoutYear());
	}
}
