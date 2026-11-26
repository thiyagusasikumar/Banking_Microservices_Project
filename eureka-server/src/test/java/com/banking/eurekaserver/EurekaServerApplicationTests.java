package com.banking.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.server.use-read-only-response-cache=false", "eureka.server.wait-time-in-ms-when-sync-empty=0"})
class EurekaServerApplicationTests {
    @Autowired
    TestRestTemplate client;

    @Test
    void servesDashboardAndHealth() {
        assertThat(client.getForEntity("/", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(client.getForEntity("/actuator/health", String.class).getBody()).contains("\"UP\"");
    }

    @Test
    void acceptsRegistrationHeartbeatLookupAndDeregistration() {
        String registration = """
                {"instance": {
                  "instanceId": "test-service:12345", "hostName": "localhost",
                  "app": "TEST-SERVICE", "ipAddr": "127.0.0.1", "status": "UP",
                  "port": {"$": 12345, "@enabled": "true"},
                  "dataCenterInfo": {"@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo", "name": "MyOwn"},
                  "vipAddress": "test-service"
                }}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        assertThat(client.postForEntity("/eureka/apps/TEST-SERVICE", new HttpEntity<>(registration, headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        try {
            assertThat(client.exchange("/eureka/apps/TEST-SERVICE/test-service:12345", HttpMethod.PUT,
                    new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            var response = client.exchange("/eureka/apps/TEST-SERVICE", HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("test-service:12345");
        } finally {
            assertThat(client.exchange("/eureka/apps/TEST-SERVICE/test-service:12345", HttpMethod.DELETE,
                    new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(client.getForEntity("/eureka/apps/TEST-SERVICE/test-service:12345", String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
