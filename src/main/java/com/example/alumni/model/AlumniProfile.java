package com.example.alumni.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alumni_profiles")
@Getter
@Setter
@NoArgsConstructor
public class AlumniProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "current_role_title")
	private String currentRole;

	@Column(nullable = false)
	private String university;

	private String location;

	@Column(name = "linkedin_headline", length = 1000)
	private String linkedinHeadline;

	@Column(name = "passout_year")
	private Integer passoutYear;

	/** Used to de-duplicate: a profile found by several searches is stored once and refreshed. */
	@Column(name = "linkedin_url", unique = true, length = 1000)
	private String linkedinUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	@Setter(AccessLevel.NONE)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		createdAt = updatedAt = Instant.now();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
