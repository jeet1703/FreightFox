package com.example.alumni.service;

import java.util.ArrayList;
import java.util.List;

import com.example.alumni.client.PhantomBusterClient;
import com.example.alumni.client.ScrapedProfile;
import com.example.alumni.dto.AlumniProfileDto;
import com.example.alumni.dto.AlumniSearchRequest;
import com.example.alumni.model.AlumniProfile;
import com.example.alumni.repository.AlumniProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AlumniService {

	private final PhantomBusterClient phantomBuster;
	private final AlumniProfileRepository repository;
	private final TransactionTemplate transaction;
	private final Object saveLock = new Object();

	public List<AlumniProfileDto> search(AlumniSearchRequest request) {
		String university = request.university().trim();
		String designation = request.designation().trim();
		Integer year = request.passoutYear();

		// The scrape can take minutes, so it runs outside any database transaction.
		List<ScrapedProfile> scraped = phantomBuster.searchAlumni(university, designation, year);

		// shortcut: one save at a time, so two searches that find the same person cannot both insert
		// them. Holds only for a single app instance; with several, switch to a database upsert.
		synchronized (saveLock) {
			return transaction.execute(status -> save(scraped, university, designation, year));
		}
	}

	private List<AlumniProfileDto> save(List<ScrapedProfile> scraped, String university, String designation,
			Integer year) {
		List<AlumniProfileDto> saved = new ArrayList<>();
		for (ScrapedProfile s : scraped) {
			// Drop profiles whose known graduation year contradicts the requested one.
			if (year != null && s.passoutYear() != null && !year.equals(s.passoutYear())) {
				continue;
			}
			String name = cut(s.name(), 255);
			String school = cut(s.school() != null ? s.school() : university, 255);
			String url = StringUtils.hasText(s.profileUrl()) ? cut(s.profileUrl(), 1000) : null;
			// A profile is identified by its LinkedIn URL, or by name + university when it has none.
			AlumniProfile profile = (url != null
					? repository.findByLinkedinUrl(url)
					: repository.findFirstByLinkedinUrlIsNullAndNameAndUniversity(name, school))
					.orElseGet(AlumniProfile::new);
			profile.setName(name);
			profile.setCurrentRole(cut(s.currentRole() != null ? s.currentRole() : designation, 255));
			profile.setUniversity(school);
			profile.setLocation(cut(s.location(), 255));
			profile.setLinkedinHeadline(cut(s.headline(), 1000));
			profile.setPassoutYear(s.passoutYear() != null ? s.passoutYear() : year);
			profile.setLinkedinUrl(url);
			saved.add(AlumniProfileDto.from(repository.save(profile)));
		}
		return saved;
	}

	/** Scraped text is not ours to trust: keep it within the column length instead of failing the batch. */
	private static String cut(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max);
	}

	public List<AlumniProfileDto> findAll() {
		return repository.findAll(Sort.by("id")).stream().map(AlumniProfileDto::from).toList();
	}
}
