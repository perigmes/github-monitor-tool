import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class Main {

    // configs
    private static final String STATE_FILE = ".monitor_state.json";
    private static final long POLL_INTERVAL_MS = 10000; 
    
    // tools
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final DateTimeFormatter PRINTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static Instant lastCheckTime;
    private static boolean isFirstRun = true;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Main <owner/repo> <token>");
            System.exit(1);
        }
        String repo = args[0];
        String token = args[1];

        loadState();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Arrêt] Sauvegarde de l'état...");
            saveState();
        }));

        System.out.println("Monitoring started on " + repo);
        if (isFirstRun) {
            System.out.println("First run : I show only new events from now on.");
        } else {
            System.out.println("Resuming : Catching up events since " + PRINTER.format(lastCheckTime));
        }

        while (true) {
            try {
                Instant now = Instant.now();
                checkRepository(repo, token);
                lastCheckTime = now;
                saveState();
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (Exception e) {
                System.err.println("[Global Error] " + e.getMessage());
                e.printStackTrace(); 
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void checkRepository(String repo, String token) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + repo + "/actions/runs?per_page=10&sort=updated_at&direction=desc";
        String json = fetch(url, token);
        if (json == null) return;

        Models.WorkflowRunsResponse response = gson.fromJson(json, Models.WorkflowRunsResponse.class);
        if (response == null || response.runs == null) return;

        List<Models.WorkflowRun> runsReversed = new ArrayList<>(response.runs);
        Collections.reverse(runsReversed);

        for (Models.WorkflowRun run : runsReversed) {
         
            if (run.updatedAt == null || run.createdAt == null) {
                continue; 
            }

            Instant runUpdatedAt = Instant.parse(run.updatedAt);
            if (runUpdatedAt.isBefore(lastCheckTime)) continue;

            processRunDetails(run, repo, token);
        }
    }

    private static void processRunDetails(Models.WorkflowRun run, String repo, String token) throws IOException, InterruptedException {
        String context = String.format("[%s] %s", run.headBranch, run.name);
        Instant runCreatedAt = Instant.parse(run.createdAt);

        if (runCreatedAt.isAfter(lastCheckTime) && !isFirstRun) {
            log(runCreatedAt, context, "Workflow Queued (Start)");
        }

      
        if (run.url == null) return; 

        String json = fetch(run.url, token);
        if (json == null) return;
        
        Models.JobResponse jobsResponse = gson.fromJson(json, Models.JobResponse.class);
        if (jobsResponse == null || jobsResponse.jobs == null) return;

        for (Models.Job job : jobsResponse.jobs) {
            checkEvent(job.startedAt, context, "Job '" + job.name + "' Started");
            checkEvent(job.completedAt, context, "Job '" + job.name + "' Finished (" + job.conclusion + ")");

            if (job.steps != null) {
                for (Models.Step step : job.steps) {
                    checkEvent(step.startedAt, context, "  -> Step '" + step.name + "' Started");
                    checkEvent(step.completedAt, context, "  -> Step '" + step.name + "' " + step.conclusion);
                }
            }
        }
    }

    private static void checkEvent(String dateStr, String context, String message) {
        if (dateStr == null) return; 
        Instant date = Instant.parse(dateStr);

        if (date.isAfter(lastCheckTime) && !isFirstRun) {
            log(date, context, message);
        }
    }

    private static void log(Instant time, String context, String message) {
        System.out.println(String.format("[%s] %s : %s", PRINTER.format(time), context, message));
    }

    private static String fetch(String url, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        return response.body();
    }

    private static class State { String lastCheckTime; }

    private static void loadState() {
        try {
            Path path = Paths.get(STATE_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                State state = gson.fromJson(content, State.class);
                lastCheckTime = Instant.parse(state.lastCheckTime);
                isFirstRun = false; 
            } else {
                lastCheckTime = Instant.now();
                isFirstRun = true;
            }
        } catch (Exception e) {
            lastCheckTime = Instant.now();
            isFirstRun = true;
        }
    }

    private static void saveState() {
        try {
            if (lastCheckTime == null) return;
            State state = new State();
            state.lastCheckTime = lastCheckTime.toString();
            Files.writeString(Paths.get(STATE_FILE), gson.toJson(state));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}