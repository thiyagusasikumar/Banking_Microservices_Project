import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Checks HTTP readiness without adding curl or a shell dependency to application images. */
public class ContainerHealthcheck {
    public static void main(String[] args) {
        try {
            String port = System.getenv().getOrDefault("SERVER_PORT", "8080");
            String path = System.getenv().getOrDefault("HEALTHCHECK_PATH", "/");
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            // Protected/unmapped roots may return 401/403/404; actuator checks require 200.
            boolean healthy = path.startsWith("/actuator/") ? status == 200 : status >= 200 && status < 500;
            System.exit(healthy ? 0 : 1);
        } catch (Exception exception) {
            System.exit(1);
        }
    }
}
