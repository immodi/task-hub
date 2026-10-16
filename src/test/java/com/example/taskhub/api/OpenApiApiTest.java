package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Guards the published OpenAPI document so the docs can't silently drift from the API. */
class OpenApiApiTest extends AbstractApiTest {

    @Test
    @SuppressWarnings("unchecked")
    void documentDescribesInfoSecurityTagsAndPaths() {
        Resp r = get("/v3/api-docs", null);

        assertThat(r.status()).isEqualTo(200);
        assertThat((String) r.json("$.info.title")).isEqualTo("TaskHub API");
        assertThat((String) r.json("$.info.version")).isEqualTo("v1");
        assertThat((String) r.json("$.info.license.name")).isEqualTo("MIT");

        assertThat((String) r.json("$.components.securitySchemes['bearer-jwt'].scheme")).isEqualTo("bearer");
        assertThat((String) r.json("$.components.securitySchemes['bearer-jwt'].bearerFormat")).isEqualTo("JWT");

        Map<String, Object> paths = r.json("$.paths");
        assertThat(paths).containsKeys(
                "/api/auth/register", "/api/auth/login", "/api/auth/me",
                "/api/projects", "/api/projects/{id}", "/api/projects/{id}/stats",
                "/api/projects/{projectId}/tasks", "/api/tasks/{id}", "/api/tasks/{id}/status",
                "/api/tasks/{id}/assignee", "/api/tasks/{taskId}/comments",
                "/api/admin/users", "/api/admin/activity");

        List<String> tags = r.json("$.tags[*].name");
        assertThat(tags).contains("Auth", "Projects", "Tasks", "Comments", "Admin");
    }

    @Test
    void authEndpointsAreDocumentedAsPublic() {
        Resp r = get("/v3/api-docs", null);

        // Empty security requirements means "no bearer token needed".
        assertThat(r.<List<Object>>json("$.paths['/api/auth/login'].post.security")).isEmpty();
        assertThat(r.<List<Map<String, Object>>>json("$.paths['/api/auth/register'].post.security")).isEmpty();
    }

    @Test
    void pageableIsDocumentedAsPageSizeAndSortParameters() {
        Resp r = get("/v3/api-docs", null);

        List<Map<String, Object>> params = r.json("$.paths['/api/projects'].get.parameters");
        List<String> names = params.stream().map(p -> (String) p.get("name")).toList();

        assertThat(names).contains("page", "size", "sort").doesNotContain("pageable");
    }
}
