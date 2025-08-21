package uy.com.bay.utiles.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uy.com.bay.utiles.data.Role;
import uy.com.bay.utiles.data.SurveyToGoUser;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.User;
import uy.com.bay.utiles.data.UserRepository;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.services.UserService;

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class SurveyToGoSurveyorRetriever {

	private final SurveyorService surveyorService;
	private final UserService userService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${surveyToGo.username}")
	private String username;

	@Value("${surveyToGo.password}")
	private String password;

	private final RestTemplate restTemplate;

	public SurveyToGoSurveyorRetriever(SurveyorService surveyorService, UserService userService,
			UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.surveyorService = surveyorService;
		this.userService = userService;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.restTemplate = new RestTemplate();
	}

	@Scheduled(cron = "0 * * * * ?") // Runs every hour
	public void retrieveAndSaveSurveyors() {
		System.out.println("Starting SurveyToGo Surveyor Retriever Task...");

		try {
			HttpHeaders headers = new HttpHeaders();
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
			String authHeader = "Basic " + new String(encodedAuth);
			headers.set("Authorization", authHeader);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			String url = "http://api.dooblo.net/newapi/Admin/GetSurveyorUsers";
			ResponseEntity<SurveyToGoUser[]> response = restTemplate.exchange(url, HttpMethod.GET, entity,
					SurveyToGoUser[].class);

			SurveyToGoUser[] users = response.getBody();
			if (users == null || users.length == 0) {
				System.out.println("No surveyors fetched from SurveyToGo. Task finished.");
				return;
			}

			System.out.println("Fetched " + users.length + " users from SurveyToGo.");

			for (SurveyToGoUser user : users) {
				if (user.getId() == null || user.getId().trim().isEmpty()) {
					System.out.println("Skipping user with null or empty ID.");
					continue;
				}

				Optional<Surveyor> existingSurveyorOpt = surveyorService.findBySurveyToGoId(user.getId());
				Surveyor surveyor = existingSurveyorOpt.orElse(new Surveyor());

				surveyor.setSurveyToGoId(user.getId());
				surveyor.setFirstName(user.getUsername());
				surveyor.setCi(user.getExternalRefID());

				surveyorService.save(surveyor);

				Optional<User> existingUserOpt = userRepository.findByUsername(surveyor.getFirstName());
				if (existingUserOpt.isEmpty()) {
					User newUser = new User();
					newUser.setUsername(surveyor.getFirstName());
					newUser.setName(surveyor.getFirstName());
					newUser.setPassword(passwordEncoder.encode(surveyor.getCi()));
					Set<Role> roles = new HashSet<>();
					roles.add(Role.ENCUESTADORES);
					newUser.setRoles(roles);
					userService.save(newUser);
				}
			}

			System.out.println("SurveyToGo Surveyor Retriever Task finished successfully.");

		} catch (Exception e) {
			System.err.println("Error during SurveyToGo Surveyor Retriever Task: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
