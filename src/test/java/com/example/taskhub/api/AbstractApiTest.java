package com.example.taskhub.api;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import com.example.taskhub.domain.Role;
import com.example.taskhub.domain.User;
import com.example.taskhub.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Black-box API tests: the full app on a random port, called over real HTTP.
 * (The .NET equivalent is WebApplicationFactory + HttpClient.)
 * Data is NOT rolled back between tests, so every test registers its own uniquely named users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractApiTest {

    /** Status + raw body of a response, with a tiny JsonPath helper. */
    record Resp(int status, String body) {
        <T> T json(String path) {
            return JsonPath.read(body, path);
        }
        /** Like json(), but returns null for both a JSON null and a missing field. */
        Object jsonOrNull(String path) {
            try {
                return JsonPath.read(body, path);
            } catch (com.jayway.jsonpath.PathNotFoundException e) {
                return null;
            }
        }
        long id() {
            Number n = json("$.id");
            return n.longValue();
        }
    }

    /** A logged-in test user. */
    record Session(long id, String username, String token) {}

    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private RestClient client;

    RestClient client() {
        if (client == null) {
            client = RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory())   // supports PATCH, unlike HttpURLConnection
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return client;
    }

    Resp call(HttpMethod method, String uri, String token, String jsonBody) {
        RestClient.RequestBodySpec spec = client().method(method).uri(uri).accept(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    if (token != null) {
                        h.setBearerAuth(token);
                    }
                });
        if (jsonBody != null) {
            spec.contentType(MediaType.APPLICATION_JSON).body(jsonBody);
        }
        return spec.exchange((req, res) -> {
            String body = "";
            try {
                body = new String(res.getBody().readAllBytes());
            } catch (IOException ignored) {
                // empty body
            }
            return new Resp(res.getStatusCode().value(), body);
        });
    }

    Resp get(String uri, String token)                  { return call(HttpMethod.GET, uri, token, null); }
    Resp post(String uri, String token, String body)    { return call(HttpMethod.POST, uri, token, body); }
    Resp put(String uri, String token, String body)     { return call(HttpMethod.PUT, uri, token, body); }
    Resp patch(String uri, String token, String body)   { return call(HttpMethod.PATCH, uri, token, body); }
    Resp delete(String uri, String token)               { return call(HttpMethod.DELETE, uri, token, null); }

    // ---- fixtures ----

    String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    Session registerUser(String prefix) {
        String name = unique(prefix);
        Resp r = post("/api/auth/register", null,
                "{\"username\":\"%s\",\"email\":\"%s@test.dev\",\"password\":\"Password1\"}".formatted(name, name));
        if (r.status() != 201) {
            throw new IllegalStateException("register failed: " + r);
        }
        Number id = r.json("$.user.id");
        return new Session(id.longValue(), name, r.json("$.token"));
    }

    Session adminUser() {
        String name = unique("admin");
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.dev");
        u.setPasswordHash(encoder.encode("Password1"));
        u.setRole(Role.ADMIN);
        users.saveAndFlush(u);
        Resp login = post("/api/auth/login", null, "{\"username\":\"%s\",\"password\":\"Password1\"}".formatted(name));
        return new Session(u.getId(), name, login.json("$.token"));
    }

    long createProject(Session owner, String name) {
        Resp r = post("/api/projects", owner.token(), "{\"name\":\"%s\"}".formatted(name));
        if (r.status() != 201) {
            throw new IllegalStateException("create project failed: " + r);
        }
        return r.id();
    }

    long createTask(Session s, long projectId, String json) {
        Resp r = post("/api/projects/" + projectId + "/tasks", s.token(), json);
        if (r.status() != 201) {
            throw new IllegalStateException("create task failed: " + r);
        }
        return r.id();
    }
}
