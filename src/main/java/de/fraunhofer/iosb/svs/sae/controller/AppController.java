package de.fraunhofer.iosb.svs.sae.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.auth0.jwt.interfaces.DecodedJWT;

import de.fraunhofer.iosb.svs.sae.configuration.RestApiV1Controller;
import de.fraunhofer.iosb.svs.sae.db.App;
import de.fraunhofer.iosb.svs.sae.db.AppRepository;
import de.fraunhofer.iosb.svs.sae.security.JWTMisc;
import de.fraunhofer.iosb.svs.sae.security.ReloadableX509TrustManager;
import de.fraunhofer.iosb.svs.sae.security.RestTemplateWithReloadableTruststore;

@RestApiV1Controller
public class AppController {
    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    
	@Autowired
	private AppRepository appRepository;

	@Autowired
	static ReloadableX509TrustManager reloadbleTrustManager;

	@Autowired
	RestTemplateWithReloadableTruststore restTemplateCreator;

	@Value("${jwt.secret}")
	private String jwtSecret;

	@PostMapping(path = "/app/register", produces = "application/json")
	public ResponseEntity<HashMap<String, String>> appRegister(@RequestBody App app) throws Exception {

		HashMap<String, String> data = new HashMap<>();
		ResponseEntity<HashMap<String, String>> response;

		if (!appRepository.existsById(app.getKey())) {
			// Save app in DB
			app.setPending(true);
			appRepository.save(app);

			// Generate JWT
			List<Pair<String, String>> claims = new ArrayList<Pair<String, String>>();
			claims.add(new Pair<String, String>("name", app.getKey()));
			String token = JWTMisc.signAndGenerate(claims);

			// Create request
			data.put("token", token);
			data.put("status", "App is pending");
			response = new ResponseEntity<HashMap<String, String>>(data, HttpStatus.OK);
		} else {
			data.put("status", "App with this name already exists");
			response = new ResponseEntity<HashMap<String, String>>(data, HttpStatus.BAD_REQUEST);
		}

		return response;
	}
	
	@PostMapping(path = "/app/unregister", produces = "application/json")
	public ResponseEntity<HashMap<String, String>> appUnregister(@RequestHeader("token") String token)
			throws Exception {

		HashMap<String, String> data = new HashMap<>();
		HttpStatus status;

		DecodedJWT jwt = JWTMisc.verifyAndDecode(token);

		if (jwt != null) {
			String name = jwt.getClaim("name").asString();
			Optional<App> app = appRepository.findById(name);

			if (app.isPresent()) {
				appRepository.deleteById(name);
				data.put("status", "Unsubscribed successfully");
				status = HttpStatus.OK;
			} else {
				data.put("status", "App doesn't exist");
				status = HttpStatus.BAD_REQUEST;
				log.debug("App doens't exist");
			}
		}
		else {
			data.put("status", "Invalid token");
			status = HttpStatus.BAD_REQUEST;
			log.debug("Invalid token");
		}

		return new ResponseEntity<HashMap<String, String>>(data, status);
	}

	@PostMapping(path = "/app/deactivate", produces = "application/json")
	public ResponseEntity<HashMap<String, String>> appDeactivate(@RequestHeader("token") String token)
			throws Exception {

		HashMap<String, String> data = new HashMap<>();
		HttpStatus status;

		DecodedJWT jwt = JWTMisc.verifyAndDecode(token);

		if (jwt != null) {
			String name = jwt.getClaim("name").asString();
			Optional<App> appData = appRepository.findById(name);

			if (appData.isPresent()) {
				App app = appData.get();
				if(app.getActive()) {
					app.setActive(false);
					appRepository.save(app);
					data.put("status", "Deactivated successfully");
					status = HttpStatus.OK;
				}
				else {
					data.put("status", "App is already deactivated");
					status = HttpStatus.OK;
				}

			} else {
				data.put("status", "App doesn't exist");
				status = HttpStatus.BAD_REQUEST;
			}
		}
		else {
			data.put("status", "Invalid token");
			status = HttpStatus.BAD_REQUEST;
		}

		return new ResponseEntity<HashMap<String, String>>(data, status);
	}

	@PostMapping(path = "/app/activate", produces = "application/json")
	public ResponseEntity<HashMap<String, String>> appActivate(@RequestHeader("token") String token)
			throws Exception {

		HashMap<String, String> data = new HashMap<>();
		HttpStatus status;

		DecodedJWT jwt = JWTMisc.verifyAndDecode(token);

		if (jwt != null) {
			String name = jwt.getClaim("name").asString();
			Optional<App> appData = appRepository.findById(name);

			if (appData.isPresent()) {
				App app = appData.get();
				if(!app.getActive()) {
					app.setActive(true);
					appRepository.save(app);
					data.put("status", "Activated successfully");
					status = HttpStatus.OK;
				}
				else {
					data.put("status", "App is already activated");
					status = HttpStatus.OK;
				}

			} else {
				data.put("status", "App doesn't exist");
				status = HttpStatus.BAD_REQUEST;
			}
		}
		else {
			data.put("status", "Invalid token");
			status = HttpStatus.BAD_REQUEST;
		}

		return new ResponseEntity<HashMap<String, String>>(data, status);
	}


	@GetMapping(path = "/app", produces = "application/json")
	public ResponseEntity<HashMap<String,Object>> getAppStatus(@RequestHeader("token") String token) throws Exception {

		DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
		HashMap<String, Object> data = new HashMap<>();
		HttpStatus status;

		if (jwt != null) {
			String name = jwt.getClaim("name").asString();
			Optional<App> app = appRepository.findById(name);

			if (app.isPresent()) {

				data.put("pending", app.get().isPending());
				data.put("active", app.get().isActive());
				status = HttpStatus.OK;
			} else {
				data.put("status", "App doesn't exist");
				status = HttpStatus.FAILED_DEPENDENCY;
			}
		}
		else {
			data.put("status", "Invalid token");
			status = HttpStatus.BAD_REQUEST;
		}

		return new ResponseEntity<HashMap<String, Object>>(data, status);
	}
}
