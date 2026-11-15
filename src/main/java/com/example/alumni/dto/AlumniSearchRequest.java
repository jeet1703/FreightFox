package com.example.alumni.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/alumni/search. passoutYear is optional and may be sent as a number or a numeric string. */
public record AlumniSearchRequest(
		@NotBlank(message = "university is required")
		@Size(max = 255, message = "university must be at most 255 characters")
		String university,
		@NotBlank(message = "designation is required")
		@Size(max = 255, message = "designation must be at most 255 characters")
		String designation,
		@Min(value = 1900, message = "passoutYear must be 1900 or later")
		@Max(value = 2100, message = "passoutYear must be 2100 or earlier")
		Integer passoutYear) {
}
