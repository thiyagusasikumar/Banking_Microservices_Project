package com.banking.apigateway;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "spring.cloud.loadbalancer.cache.enabled=false"})
class GatewayRoutingTests {
    private static final DisposableServer UPSTREAM = HttpServer.create().host("127.0.0.1").port(0)
            .handle((request, response) -> response.header("Content-Type", "text/plain")
                    .sendString(request.receive().aggregate().asString().defaultIfEmpty("")
                            .map(body -> request.method().name() + " " + request.uri() + " "
                                    + request.requestHeaders().get("Authorization", "") + " " + body)))
            .bindNow();

    @DynamicPropertySource
    static void discovery(DynamicPropertyRegistry registry) {
        for (String service : new String[]{"auth-service", "account-service", "transaction-service",
                "ledger-service", "notification-service", "approval-service"}) {
            registry.add("spring.cloud.discovery.client.simple.instances." + service + "[0].uri",
                    () -> "http://127.0.0.1:" + UPSTREAM.port());
        }
    }

    @LocalServerPort
    int port;
    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("valid-token").header("alg", "RS256").subject("customer-123")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        when(jwtDecoder.decode("valid-token")).thenReturn(Mono.just(jwt));
        when(jwtDecoder.decode("invalid-token")).thenReturn(Mono.error(new BadJwtException("Invalid token")));
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10)).build();
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.disposeNow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/me", "/api/v1/accounts", "/api/v1/accounts/me", "/transactions/ref-123",
            "/ledger/entries/1", "/notification/getUnreadCount", "/approvals/1"})
    void routesThroughDiscoveryPreservingPathQueryAndBearerToken(String path) {
        client.get().uri(path + "?page=2").headers(h -> h.setBearerAuth("valid-token"))
                .exchange().expectStatus().isOk().expectBody(String.class)
                .isEqualTo("GET " + path + "?page=2 Bearer valid-token ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/login", "/auth/public/register", "/auth/refresh"})
    void publicAuthRequestsPreserveMethodAndBody(String path) {
        client.post().uri(path).header("Content-Type", "application/json").bodyValue("{\"sample\":true}")
                .exchange().expectStatus().isOk().expectBody(String.class)
                .isEqualTo("POST " + path + "  {\"sample\":true}");
    }

    @Test
    void protectsPrivateApisAndRejectsInvalidTokens() {
        client.get().uri("/transactions/ref-123").exchange().expectStatus().isUnauthorized();
        client.get().uri("/auth/me").headers(h -> h.setBearerAuth("invalid-token"))
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/auth/login").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void blocksInternalAccountOperations() {
        client.post().uri("/api/v1/accounts/operations").headers(h -> h.setBearerAuth("valid-token"))
                .exchange().expectStatus().isForbidden();
        client.get().uri("/api/v1/accounts/operations/ref-123").headers(h -> h.setBearerAuth("valid-token"))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void returnsServiceUnavailableWhenNoInstanceIsRegistered() {
        client.get().uri("/reconciliation/1").headers(h -> h.setBearerAuth("valid-token"))
                .exchange().expectStatus().isEqualTo(503);
    }

    @Test
    void doesNotExposeAutomaticServiceRoutes() {
        client.get().uri("/auth-service/actuator/health").headers(h -> h.setBearerAuth("valid-token"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void permitsOnlyConfiguredCorsPreflight() {
        client.options().uri("/api/v1/accounts").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
        client.options().uri("/api/v1/accounts").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
