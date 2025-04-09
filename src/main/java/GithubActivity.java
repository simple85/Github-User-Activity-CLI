import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;


public class GithubActivity {
    public static JSONArray fetchUserActivity (String userName) throws Exception {

        String urlString = "https://api.github.com/users/" + userName + "/events";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();

        if(responseCode == 404) {
            throw new Exception("User not found. Please check the username.");
        } else if(responseCode !=200) {
            throw new Exception("Error fetching data: " + responseCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = br.readLine()) != null) {
            content.append(inputLine);
        }
        br.close();
        conn.disconnect();

        return new JSONArray(content.toString());
    }
    public static void displayActivity(JSONArray events) {
        if(events.isEmpty()) {
            System.out.println("No recent activity available");
        }
        for(int i=0;i<events.length();i++) {
            JSONObject event = events.getJSONObject(i);
            String type = event.getString("type");
            JSONObject repo = event.getJSONObject("repo");
            JSONObject payload = event.getJSONObject("payload");
            String action;

            switch (type) {
                case "PushEvent":
                    int commitCount = payload.getJSONArray("commits").length();
                    action = "Pushed " + commitCount + (commitCount<2 ? " commit to " : " commits to ") + repo.getString("name");
                    break;
                case "IssuesEvent":
                    String issueAction = payload.getString("action");
                    action = firstLetterToUpper(issueAction) + " an issue in " + repo.getString("name");
                    break;
                case "WatchEvent":
                    action = "Starred" + repo.getString("name");
                    break;
                case "ForkEvent":
                    action = "Forked" + repo.getString("name");
                    break;
                case "CreateEvent":
                    String refType = payload.getString("ref_type");
                    action = "Created " + refType + " in " + repo.getString("name");
                    break;
                default:
                    action = type.replace("Event", "") + " in " + repo.getString("name");
                    break;
            }
            System.out.println("- " + action);
        }
    }
    private static String firstLetterToUpper(String str) {
        if(str == null || str.isEmpty()) return str;
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }
}
