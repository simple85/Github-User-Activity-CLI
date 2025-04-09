import com.sun.jdi.event.ExceptionEvent;
import org.json.JSONArray;

public class GithubActivityCLI {
        public static void main(String[] args) {
                if(args.length == 0) {
                        System.err.println("Provide a Github username");
                        return;
                }

                try {
                       JSONArray events = GithubActivity.fetchUserActivity(args[0]);
                       GithubActivity.displayActivity(events);
                } catch (Exception e) {
                        System.err.println(e.getMessage());
                }
        }

}