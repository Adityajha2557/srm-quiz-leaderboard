import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class QuizSolver {

    static String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    static String REG_NO = "RA2311028010013";

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        Set<String> seenEvents = new HashSet<>();

        Map<String, Integer> scores = new HashMap<>();

        System.out.println("Starting to poll the API...");

        for (int poll = 0; poll <= 9; poll++) {

            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            System.out.println("Poll " + poll + " -> Status: " + resp.statusCode());
            System.out.println("Response: " + body);

            List<String[]> events = parseEvents(body);

            for (String[] ev : events) {
                String roundId = ev[0];
                String participant = ev[1];
                int score = Integer.parseInt(ev[2]);

                String dedupKey = roundId + "||" + participant;

                if (seenEvents.contains(dedupKey)) {
                    System.out.println("  Duplicate found, skipping: " + dedupKey);
                    continue;
                }

                seenEvents.add(dedupKey);

                // add score to this participant
                int current = scores.getOrDefault(participant, 0);
                scores.put(participant, current + score);

                System.out.println("  Added score for " + participant + " from round " + roundId + ": +" + score);
            }

            // 5 secs wait
            if (poll < 9) {
                System.out.println("Waiting 5 seconds before next poll...");
                Thread.sleep(5000);
            }
        }

        // build
        List<Map.Entry<String, Integer>> leaderboardList = new ArrayList<>(scores.entrySet());
        leaderboardList.sort((a, b) -> b.getValue() - a.getValue()); // highest score first

        System.out.println("\n=== Final Leaderboard ===");
        int grandTotal = 0;
        for (Map.Entry<String, Integer> entry : leaderboardList) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
            grandTotal += entry.getValue();
        }
        System.out.println("Grand Total: " + grandTotal);

        // build Json
        StringBuilder leaderboardJson = new StringBuilder();
        leaderboardJson.append("[");
        for (int i = 0; i < leaderboardList.size(); i++) {
            Map.Entry<String, Integer> e = leaderboardList.get(i);
            leaderboardJson.append("{\"participant\":\"").append(e.getKey())
                    .append("\",\"totalScore\":").append(e.getValue()).append("}");
            if (i < leaderboardList.size() - 1)
                leaderboardJson.append(",");
        }
        leaderboardJson.append("]");

        String submitBody = "{\"regNo\":\"" + REG_NO + "\",\"leaderboard\":" + leaderboardJson + "}";

        System.out.println("\nSubmitting leaderboard...");
        System.out.println("Payload: " + submitBody);

        HttpRequest submitReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(submitBody))
                .build();

        HttpResponse<String> submitResp = client.send(submitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nSubmit Response: " + submitResp.body());
    }

    static List<String[]> parseEvents(String json) {
        List<String[]> result = new ArrayList<>();

        int eventsStart = json.indexOf("\"events\"");
        if (eventsStart == -1) {
            System.out.println("  No events found in this response.");
            return result;
        }

        int arrStart = json.indexOf("[", eventsStart);
        int arrEnd = json.indexOf("]", arrStart);
        if (arrStart == -1 || arrEnd == -1)
            return result;

        String eventsSection = json.substring(arrStart, arrEnd + 1);

        String[] chunks = eventsSection.split("\\}");

        for (String chunk : chunks) {
            String roundId = extractValue(chunk, "roundId");
            String participant = extractValue(chunk, "participant");
            String scoreStr = extractValue(chunk, "score");

            if (roundId == null || participant == null || scoreStr == null)
                continue;

            scoreStr = scoreStr.replaceAll("[^0-9]", "").trim();
            if (scoreStr.isEmpty())
                continue;

            result.add(new String[] { roundId, participant, scoreStr });
        }

        return result;
    }

    static String extractValue(String chunk, String key) {
        String searchKey = "\"" + key + "\"";
        int idx = chunk.indexOf(searchKey);
        if (idx == -1)
            return null;

        int colonIdx = chunk.indexOf(":", idx + searchKey.length());
        if (colonIdx == -1)
            return null;

        String rest = chunk.substring(colonIdx + 1).trim();

        if (rest.startsWith("\"")) {
            // string value
            int end = rest.indexOf("\"", 1);
            if (end == -1)
                return null;
            return rest.substring(1, end);
        } else {

            StringBuilder num = new StringBuilder();
            for (char c : rest.toCharArray()) {
                if (Character.isDigit(c))
                    num.append(c);
                else if (num.length() > 0)
                    break;
            }
            return num.length() > 0 ? num.toString() : null;
        }
    }
}
