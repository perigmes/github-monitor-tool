import java.util.List;

import com.google.gson.annotations.SerializedName;
public class Models {
    public static class WorkflowRunsResponse{
        @SerializedName("workflow_runs")
        public List<WorkflowRun> runs;
    }
    public static class WorkflowRun {
        public long id;
        public String name;
        public String status;
        public String conclusion;

        @SerializedName("head_branch")
        public String headBranch;

        @SerializedName("head_sha")
        public String headSha;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("updated_at")
        public String updatedAt;

        @SerializedName("jobs_url")
        public String url;

    }
    public static class JobResponse {
        @SerializedName("jobs")
        public List<Job> jobs;
    }
    public static class Job {
        public long id;
        public String name;
        public String status;
        public String conclusion;

        @SerializedName("started_at")
        public String startedAt;

        @SerializedName("completed_at")
        public String completedAt;

        public List<Step> steps;
    }
    public static class Step {
        public String name;
        public String status;
        public String conclusion;

        @SerializedName("number")
        public int stepNumber;

        @SerializedName("started_at")
        public String startedAt;

        @SerializedName("completed_at")
        public String completedAt;
    }
}
