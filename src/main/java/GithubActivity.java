import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;


public class GithubActivity {
    private static final String API_BASE = "https://api.github.com/users/";

    private final HttpClient client;

    public GithubActivity() {
        this(HttpClient.newHttpClient());
    }

    public GithubActivity(HttpClient client) {
        this.client = client;
    }

    public JSONArray fetchUserActivity(String userName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + userName + "/events"))
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new Exception("Could not reach GitHub: " + e.getMessage());
        }

        int responseCode = response.statusCode();
        if(responseCode == 404) {
            throw new Exception("User not found. Please check the username.");
        } else if(responseCode !=200) {
            throw new Exception("Error fetching data: " + responseCode);
        }

        return new JSONArray(response.body());
    }

    public static void displayActivity(JSONArray events) {
        if(events.isEmpty()) {
            System.out.println("No recent activity available");
        }
        for(int i=0;i<events.length();i++) {
            System.out.println("- " + describeEvent(events.getJSONObject(i)));
        }
    }

    static String describeEvent(JSONObject event) {
        String type = event.getString("type");
        String repoName = event.getJSONObject("repo").getString("name");
        JSONObject payload = event.optJSONObject("payload", new JSONObject());

        switch (type) {
            case "PushEvent":
                // GitHub no longer always includes the commits list in push payloads
                int commitCount = payload.has("commits") ? payload.getJSONArray("commits").length()
                        : payload.optInt("size", -1);
                if(commitCount < 0) {
                    return "Pushed to " + repoName;
                }
                return "Pushed " + commitCount + (commitCount == 1 ? " commit to " : " commits to ") + repoName;
            case "IssuesEvent":
                return firstLetterToUpper(payload.optString("action", "updated")) + " an issue in " + repoName;
            case "WatchEvent":
                return "Starred " + repoName;
            case "ForkEvent":
                return "Forked " + repoName;
            case "CreateEvent":
                return "Created " + payload.optString("ref_type", "repository") + " in " + repoName;
            default:
                return type.replace("Event", "") + " in " + repoName;
        }
    }

    private static String firstLetterToUpper(String str) {
        if(str == null || str.isEmpty()) return str;
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }
}
