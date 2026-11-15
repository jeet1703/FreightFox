package com.example.alumni.repository;

import java.util.Optional;

import com.example.alumni.model.AlumniProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, Long> {

	Optional<AlumniProfile> findByLinkedinUrl(String linkedinUrl);

	Optional<AlumniProfile> findFirstByLinkedinUrlIsNullAndNameAndUniversity(String name, String university);
}
