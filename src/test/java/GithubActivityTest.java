import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubActivityTest {

    @Mock
    HttpClient client;

    @Mock
    HttpResponse<String> response;

    @Test
    void fetchCallsUserEventsEndpointAndParsesBody() throws Exception {
        stubResponse(200, fixture());

        JSONArray events = new GithubActivity(client).fetchUserActivity("simple85");

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any());
        assertThat(request.getValue().uri()).hasToString("https://api.github.com/users/simple85/events");
        assertThat(events.length()).isEqualTo(4);
    }

    @Test
    void fetchReportsUnknownUser() throws Exception {
        stubResponse(404, "{}");

        assertThatThrownBy(() -> new GithubActivity(client).fetchUserActivity("nobody"))
                .hasMessageContaining("User not found");
    }

    @Test
    void fetchReportsOtherHttpErrors() throws Exception {
        stubResponse(403, "{}");

        assertThatThrownBy(() -> new GithubActivity(client).fetchUserActivity("simple85"))
                .hasMessage("Error fetching data: 403");
    }

    @Test
    void fetchWrapsNetworkFailures() throws Exception {
        when(client.send(any(), any())).thenThrow(new IOException("timeout"));

        assertThatThrownBy(() -> new GithubActivity(client).fetchUserActivity("simple85"))
                .hasMessage("Could not reach GitHub: timeout");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            {"type":"PushEvent","repo":{"name":"u/r"},"payload":{"commits":[{}]}}        | Pushed 1 commit to u/r
            {"type":"PushEvent","repo":{"name":"u/r"},"payload":{"size":4}}              | Pushed 4 commits to u/r
            {"type":"PushEvent","repo":{"name":"u/r"},"payload":{}}                      | Pushed to u/r
            {"type":"IssuesEvent","repo":{"name":"u/r"},"payload":{"action":"closed"}}   | Closed an issue in u/r
            {"type":"WatchEvent","repo":{"name":"u/r"},"payload":{}}                     | Starred u/r
            {"type":"ForkEvent","repo":{"name":"u/r"},"payload":{}}                      | Forked u/r
            {"type":"CreateEvent","repo":{"name":"u/r"},"payload":{"ref_type":"tag"}}    | Created tag in u/r
            {"type":"PullRequestEvent","repo":{"name":"u/r"},"payload":{}}               | PullRequest in u/r
            {"type":"PublicEvent","repo":{"name":"u/r"}}                                 | Public in u/r
            """)
    void describesEachEventType(String json, String expected) {
        assertThat(GithubActivity.describeEvent(new JSONObject(json))).isEqualTo(expected);
    }

    @Test
    void displayActivityPrintsOneBulletPerEvent() throws Exception {
        JSONArray events = new JSONArray(fixture());
        String out = captureOutput(() -> GithubActivity.displayActivity(events));

        assertThat(out.lines()).containsExactly(
                "- Pushed 3 commits to simple85/Task-Tracker-CLI",
                "- Opened an issue in simple85/Weather-App",
                "- Starred spring-projects/spring-boot",
                "- Created branch in simple85/Snake-Game");
    }

    @Test
    void displayActivityHandlesEmptyFeed() {
        String out = captureOutput(() -> GithubActivity.displayActivity(new JSONArray()));

        assertThat(out.strip()).isEqualTo("No recent activity available");
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(int status, String body) throws Exception {
        when(response.statusCode()).thenReturn(status);
        if (status == 200) {
            when(response.body()).thenReturn(body);
        }
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    private static String fixture() throws IOException {
        try (var in = Objects.requireNonNull(GithubActivityTest.class.getResourceAsStream("/events.json"))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }
}
