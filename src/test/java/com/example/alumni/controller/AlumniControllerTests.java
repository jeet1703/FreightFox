package com.example.alumni.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.alumni.client.PhantomBusterClient;
import com.example.alumni.exception.PhantomBusterException;
import com.example.alumni.client.ScrapedProfile;
import com.example.alumni.repository.AlumniProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop" })
@AutoConfigureMockMvc
class AlumniControllerTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	AlumniProfileRepository repository;

	@MockitoBean
	PhantomBusterClient phantomBuster;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	@Test
	void searchReturnsProfilesAndSavesThem() throws Exception {
		when(phantomBuster.searchAlumni("University of XYZ", "Software Engineer", 2020)).thenReturn(List.of(
				new ScrapedProfile("John Doe", "Software Engineer", "University of XYZ", "New York, NY",
						"Passionate Software Engineer at XYZ Corp", 2020, "https://www.linkedin.com/in/johndoe"),
				new ScrapedProfile("Old Grad", "Software Engineer", "University of XYZ", "Boston, MA",
						"Engineer", 2011, "https://www.linkedin.com/in/oldgrad"),
				new ScrapedProfile("No Year", null, null, null, null, null, null)));

		// passoutYear sent as a string, as in the assignment's sample request
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content("""
				{"university": "University of XYZ", "designation": "Software Engineer", "passoutYear": "2020"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("success"))
				.andExpect(jsonPath("$.message").doesNotExist())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].name").value("John Doe"))
				.andExpect(jsonPath("$.data[0].currentRole").value("Software Engineer"))
				.andExpect(jsonPath("$.data[0].university").value("University of XYZ"))
				.andExpect(jsonPath("$.data[0].location").value("New York, NY"))
				.andExpect(jsonPath("$.data[0].linkedinHeadline").value("Passionate Software Engineer at XYZ Corp"))
				.andExpect(jsonPath("$.data[0].passoutYear").value(2020))
				.andExpect(jsonPath("$.data[1].name").value("No Year"))
				.andExpect(jsonPath("$.data[1].university").value("University of XYZ"))
				.andExpect(jsonPath("$.data[1].passoutYear").value(2020));

		mvc.perform(get("/api/alumni/all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("success"))
				.andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	void repeatedSearchDoesNotDuplicateProfiles() throws Exception {
		when(phantomBuster.searchAlumni(any(), any(), isNull())).thenReturn(List.of(
				new ScrapedProfile("Jane Smith", "Data Scientist", "University of XYZ", "San Francisco, CA",
						"Data Scientist | AI Enthusiast", 2019, "https://www.linkedin.com/in/janesmith")));
		String body = """
				{"university": "University of XYZ", "designation": "Data Scientist"}""";

		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[0].passoutYear").value(2019));

		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void missingFieldsAreRejected() throws Exception {
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON)
				.content("{\"university\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value("error"))
				.andExpect(jsonPath("$.message").value("designation is required; university is required"));
	}

	@Test
	void overLongInputIsRejected() throws Exception {
		String body = "{'university': '" + "x".repeat(300) + "', 'designation': 'D'}";
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON)
				.content(body.replace((char) 39, (char) 34)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("university must be at most 255 characters"));
	}

	@Test
	void profilesWithoutUrlAreNotDuplicatedAndLongTextIsCut() throws Exception {
		when(phantomBuster.searchAlumni(any(), any(), isNull())).thenReturn(List.of(
				new ScrapedProfile("No Url", "Dev", null, "y".repeat(400), null, null, null)));
		String body = """
				{"university": "University of XYZ", "designation": "Dev"}""";

		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.findAll().get(0).getLocation()).hasSize(255);
	}

	@Test
	void nonNumericYearIsRejected() throws Exception {
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content("""
				{"university": "U", "designation": "D", "passoutYear": "soon"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value("error"));
	}

	@Test
	void phantomBusterFailureBecomes502() throws Exception {
		when(phantomBuster.searchAlumni(any(), any(), any())).thenThrow(new PhantomBusterException("boom"));
		mvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content("""
				{"university": "U", "designation": "D"}"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value("error"))
				.andExpect(jsonPath("$.message").value("boom"));
	}
}
