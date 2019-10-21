package com.rigor.teamcity.optimization;

import com.rigor.teamcity.optimization.api.*;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static jetbrains.buildServer.BuildProblemTypes.TC_ERROR_MESSAGE_TYPE;

public class OptimizationProcess implements Callable<BuildFinishedStatus> {

    private AgentRunningBuild build;
    private BuildRunnerContext context;
    private ArtifactsWatcher artifacts;
    private BuildProgressLogger logger;
    private BuildFinishedStatus buildFinishedStatus = BuildFinishedStatus.FINISHED_SUCCESS;

    private RigorApiClient rigorApiClient;

    final OptimizationConstants Constants = OptimizationConstants.Instance;

    private String ApiKey;
    private String TestIds;
    private String MinimumPerformanceScore;
    private String MaximumCriticalDefectsAllowed;
    private String DisallowedDefectIds;
    private String BuildNumber;
    private String VerifyPerformanceBudgets;

    private ArrayList<RigorApiTag> buildTags;

    protected OptimizationProcess(@NotNull AgentRunningBuild build, @NotNull BuildRunnerContext context, ArtifactsWatcher artifacts) {

        this.context = context;
        this.logger = build.getBuildLogger();

        final Map<String, String> parameters = getContext().getRunnerParameters();

        this.ApiKey = parameters.get("API_KEY");
        this.DisallowedDefectIds = parameters.get("DISALLOWED_DEFECT_IDS");
        this.MaximumCriticalDefectsAllowed = parameters.get("MAXIMUM_CRITICAL_DEFECTS_ALLOWED");
        this.MinimumPerformanceScore = parameters.get("MINIMUM_PERFORMANCE_SCORE");
        this.TestIds = parameters.get("TEST_IDS");
        this.VerifyPerformanceBudgets = parameters.get("VERIFY_PERFORMANCE_BUDGETS");

        this.BuildNumber = context.getConfigParameters().get("build.number");
    }

    @Override
    public BuildFinishedStatus call() {

        BuildFinishedStatus buildFinishedStatus = BuildFinishedStatus.FINISHED_SUCCESS;

        this.rigorApiClient = new RigorApiClient(this.ApiKey);

        logger.message("Validating user defined plugin parameters.");

        RigorApiResponse testResponse = this.rigorApiClient.TestConnection();

        if (testResponse.HttpStatusCode != 200) {
            // Note: this can also occur for an invalid base URL value, or outbound connection issue.
            BuildProblemData buildProblem = CreateBuildProblem("Invalid API key, please check it against your API key within the Rigor Optimization website.");
            logger.logBuildProblem(buildProblem);

            // Mark the build gets marked as failed.
            return BuildFinishedStatus.FINISHED_FAILED;
        }

        try {
            ArrayList<RigorApiSnapshotResult> snapshots = InitiateSnapshots();

            snapshots = GetSnapshotResults(snapshots);

            buildFinishedStatus = AnalyzeSnapshotResults(snapshots, buildFinishedStatus);

        } catch (Exception e) {
            BuildProblemData buildProblem = CreateBuildProblem("Exception occurred: " + e.getMessage());
            logger.logBuildProblem(buildProblem);

            // Set the success flag to false, so that the build gets marked as failed.
            buildFinishedStatus = BuildFinishedStatus.FINISHED_FAILED;
        }

        if (buildFinishedStatus == BuildFinishedStatus.FINISHED_SUCCESS) {
            logger.message("Tests successfully verified.");
        } else {
            // Log and mark the build as failed.
            BuildProblemData buildProblem = CreateBuildProblem("One or more tests failed, marking the build as failed.");
            logger.logBuildProblem(buildProblem);

            return BuildFinishedStatus.FINISHED_FAILED;
        }

        // Todo: future improvement to generate embedded report into TeamCity - for now users need to refer to the build log.
        // PublishArtifacts();

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    protected BuildRunnerContext getContext() {
        return context;
    }

    private void appendStringToFile(File file, String content) throws IOException {
        Files.write(Paths.get(file.toURI()), content.getBytes(), StandardOpenOption.APPEND);
    }

    private ArrayList<RigorApiSnapshotResult> InitiateSnapshots() {

        // Create new snapshots for each configured test ID.
        ArrayList<RigorApiSnapshotResult> results = new ArrayList<RigorApiSnapshotResult>();

        List<String> testIds = Arrays.asList(this.TestIds.split("\\s*,\\s*"));

        // Loop through the defined test ID values, and trigger off the requested optimization snapshot requests.
        for (String testId : testIds) {
            try {
                logger.message("Creating new snapshot request for test " + testId + ".");

                RigorApiSnapshotResult result = this.rigorApiClient.StartSnapshot(testId, "Build " + this.BuildNumber);

                results.add(result);

                logger.message("New snapshot " + result.snapshot_id + " request created: " + result.snapshot_url_guest);
            } catch (Exception e) {
                // Todo: cancel and mark the build as failed, as there is no point completing any remaining tests.
                logger.message("An error occurred while requesting the new snapshot: " + e.getMessage());
            }

            try {
                // Tag the current test as getting triggered by TeamCity.
                ArrayList<RigorApiTag> tags = new ArrayList<RigorApiTag>();
                RigorApiTag tag = new RigorApiTag();
                tag.name = "TeamCity";
                tag.priority = "Low";
                tags.add(tag);

                // Submit the tags to Rigor.
                this.rigorApiClient.UpdateTestWithTags(testId, tags);
            } catch (Exception e) {
                // Note: Failing to add tags to a Rigor optimization test should not result in failing of the tests themselves.
                logger.message("Failed to tag test " + testId.toString() + ", the build will however continue: " + e.getMessage());
            }
        }

        return results;
    }

    private ArrayList<RigorApiSnapshotResult> GetSnapshotResults(ArrayList<RigorApiSnapshotResult> snapshotRequests) throws Exception {

        ArrayList<RigorApiSnapshotResult> completedSnapshots = new ArrayList<>();

        int remainingSnapshotCount = snapshotRequests.size();

        long currentTime = System.currentTimeMillis();
        long timeoutTimeMS = currentTime + (Constants.TIMEOUT_IN_SECONDS * 1000);

        // Start with 10 seconds between polling loop.
        int sleepTime = 10000;

        if (remainingSnapshotCount > 1)
        {
            logger.message("Waiting for completion of " + remainingSnapshotCount + " snapshot(s), timeout set at " + Constants.TIMEOUT_IN_SECONDS + " seconds.");
        } else {
            logger.message("Waiting for completion of the requested snapshot, timeout set at " + Constants.TIMEOUT_IN_SECONDS + " seconds.");
        }

        while ((remainingSnapshotCount > 0) && (currentTime < timeoutTimeMS)) {
            if (currentTime > (currentTime + 120000)) {
                // Two minutes have passed, set poll wait time to 30 seconds.
                sleepTime = 30000;
            } else if (currentTime > (currentTime + 300000)) {
                // Five minutes have passed, set poll wait time to 20 seconds.
                sleepTime = 20000;
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new Exception("Abort signal received, exiting.");
            }

            if (remainingSnapshotCount > 1)
            {
                logger.message("Polling status of " + remainingSnapshotCount + " remaining snapshot(s)...");
            } else {
                logger.message("Polling status of " + remainingSnapshotCount + " remaining snapshot...");
            }

            // Loop through all test requests and check for completed test results.
            for (int i = snapshotRequests.size() - 1; i >= 0; --i) {
                RigorApiSnapshotResult snapshot = snapshotRequests.get(i);
                RigorApiSnapshotResult result = this.rigorApiClient.GetSnapshot(snapshot.test_id, snapshot.snapshot_id);

                if (result.IsFailedScan()) {
                    // Failed scan request.
                    throw new Exception("Test " + snapshot.test_id + ", snapshot " + snapshot.snapshot_id + " failed.");
                } else if (result.IsScanComplete()) {
                    // Scan request successfully completed, remove it from the poll list.
                    completedSnapshots.add(result);
                    snapshotRequests.remove(i);

                    --remainingSnapshotCount;

                    logger.message("Snapshot " + snapshot.snapshot_id + " for test " + snapshot.test_id + " complete, " + remainingSnapshotCount + " tests remain.");
                }
            }

            currentTime = System.currentTimeMillis();
        }

        if (remainingSnapshotCount > 0) {
            throw new Exception("Timeout exceeded, aborting remaining tests.");
        }

        return completedSnapshots;
    }

    private BuildFinishedStatus AnalyzeSnapshotResults(ArrayList<RigorApiSnapshotResult> snapshotResults, BuildFinishedStatus buildFinishedStatus) {

        logger.message("Analyzing captured test results.");

        boolean firstLoop = true;

        // Look through each result.
        for (RigorApiSnapshotResult snapshot : snapshotResults) {

            logger.message("Analyzing test " + snapshot.test_id + ", snapshot " + snapshot.snapshot_id + ": " + snapshot.snapshot_url_guest);

            // Reset the build tags for this individual test.
            buildTags = new ArrayList<>();

            // Verify the performance score threshold.
            if (this.MinimumPerformanceScore != null) {

                Integer minimumPerformanceScore = Integer.parseInt(this.MinimumPerformanceScore);

                if (snapshot.zoompf_score < minimumPerformanceScore) {
                    // Log the failure and tag it in Rigor.
                    AddBuildFailTag("Score less than " + this.MinimumPerformanceScore);

                    BuildProblemData buildProblem = CreateBuildProblem("Performance score failed: " + snapshot.zoompf_score + " (limit " + this.MinimumPerformanceScore + ").");
                    logger.logBuildProblem(buildProblem);

                    buildFinishedStatus = BuildFinishedStatus.FINISHED_FAILED;

                } else {
                    logger.message("Performance score passed: " + snapshot.zoompf_score + " (limit " + this.MinimumPerformanceScore + ").");
                }
            }

            // Verify the critical defect threshold.
            if (this.MaximumCriticalDefectsAllowed != null) {

                Integer maximumCriticalDefectsAllowed = Integer.parseInt(this.MaximumCriticalDefectsAllowed);

                if (snapshot.defect_count_critical_1pc > maximumCriticalDefectsAllowed) {
                    // Log the failure and tag it in Rigor.
                    BuildProblemData buildProblem = CreateBuildProblem("Critical defect check failed: " + snapshot.defect_count_critical_1pc + " (limit " + this.MaximumCriticalDefectsAllowed + ").");
                    logger.logBuildProblem(buildProblem);

                    AddBuildFailTag("Critical defects greater than " + this.MaximumCriticalDefectsAllowed + ".");

                    // Add extra detail about what failed.
                    LogCriticalDefects(snapshot);

                    buildFinishedStatus = BuildFinishedStatus.FINISHED_FAILED;

                } else {
                    logger.message("Critical defect check passed: " + snapshot.defect_count_critical_1pc + " (limit " + this.MaximumCriticalDefectsAllowed + ").");
                }
            }

            ValidateDisallowedDefectIds(snapshot);

            AnalyzePerformanceBudgets(snapshot);

            TagOutcome(buildFinishedStatus);

            try {
                this.rigorApiClient.UpdateSnapshotWithTags(snapshot.test_id, snapshot.snapshot_id, buildTags);
                logger.message("Updated build with post analysis tags.");
            } catch (Exception e) {
                logger.message("Failed to save build tags: " + e.getMessage());
            }
        }

        return buildFinishedStatus;
    }

    protected void ValidateDisallowedDefectIds(RigorApiSnapshotResult snapshot) {
        if (this.DisallowedDefectIds != null)
        {
            List<String> disallowedDefectIds = Arrays.asList(this.DisallowedDefectIds.split("\\s*,\\s*"));

            // Verify against specific defect IDs.
            if (disallowedDefectIds.size() > 0) {

                ArrayList<Integer> defectIds = new ArrayList<>();
                for (String defectId : disallowedDefectIds) {
                    defectIds.add(Integer.valueOf(defectId));
                }

                AnalyzeFoundDefects(snapshot, defectIds);
            }
        }
    }

    protected void TagOutcome(BuildFinishedStatus buildFinishedStatus) {

        RigorApiTag tag = new RigorApiTag();

        logger.message("Tagging the test result in Rigor.");

        if (buildFinishedStatus != BuildFinishedStatus.FINISHED_SUCCESS) {
            tag.name = "Failed";
            tag.priority = "High";
        } else {
            tag.name = "Success";
            tag.priority = "Low";
        }

        buildTags.add(tag);
    }

    protected void AddBuildFailTag(String message) {
        RigorApiTag tag = new RigorApiTag();
        tag.name = message;
        if (tag.name.length() > RigorApiTag.MaxTagLength) {
            tag.name = tag.name.substring(0, RigorApiTag.MaxTagLength);
        }
        tag.priority = "High";

        buildTags.add(tag);
    }

    protected void LogCriticalDefects(RigorApiSnapshotResult snapshot) {
        try {
            RigorApiDefectResultList criticalDefects = this.rigorApiClient.GetCriticalDefects(snapshot.test_id, snapshot.snapshot_id);

            Integer count = 0;

            for (RigorApiDefectResult defect : criticalDefects.defects) {
                ++count;
                LogDefect(count, defect);
            }
        } catch (Exception e) {
            logger.message("Failed to load critical defect details, build will continue: " + e.getMessage());
        }
    }

    // Look for specific defects in the results, returning true if none are found (e.g. all passed).
    protected void AnalyzeFoundDefects(RigorApiSnapshotResult snapshot, ArrayList<Integer> defectIds)
    {
        // Look for these specific defects in the results.
        RigorApiDefectResultList defectList = null;

        try {
            defectList = this.rigorApiClient.GetSpecificDefects(snapshot.test_id, snapshot.snapshot_id, defectIds);
        } catch (Exception e) {
            BuildProblemData buildProblem = CreateBuildProblem("Failed to get specific detail details: " + e.getMessage());
            logger.logBuildProblem(buildProblem);

            buildFinishedStatus = BuildFinishedStatus.FINISHED_FAILED;
        }

        if (defectList.defects.size() == 0) {
            logger.message("No disallowed defects found.");
        }

        logger.message("Defect(s) in your disallowed defect list were found: (" + defectList.defects.size() + ")");

        int count = 0;

        for (RigorApiDefectResult defect : defectList.defects) {
            ++count;
            LogDefect(count, defect);
        }

        // Tag the analyzed defect outcome.
        if (count == 1) {
            AddBuildFailTag("One failed defect found.");
        } else {
            AddBuildFailTag(count + " failed defects found.");
        }

        BuildProblemData buildProblem = CreateBuildProblem("One or more disallowed defects found.");
        logger.logBuildProblem(buildProblem);

        buildFinishedStatus = BuildFinishedStatus.FINISHED_FAILED;
    }

    protected void AnalyzePerformanceBudgets(RigorApiSnapshotResult snapshot) {

        boolean verifyPerformanceBudgets = Boolean.parseBoolean(this.VerifyPerformanceBudgets);

        if (verifyPerformanceBudgets) {

            logger.message("Verifying performance budget thresholds.");

            if (OptimizationConstants.PERFORMANCE_BUDGET_DEFECTS == null) {
                List<String> performanceDefectIds = Arrays.asList(OptimizationConstants.PERFORMANCE_BUDGET_DEFECTS.split("\\s*,\\s*"));

                // Verify against specific performance specific defect IDs.
                if (performanceDefectIds.size() > 0) {

                    ArrayList<Integer> defectIds = new ArrayList<>();
                    for (String defectId : performanceDefectIds) {
                        defectIds.add(Integer.valueOf(defectId));
                    }

                    AnalyzeFoundDefects(snapshot, defectIds);
                }
            }
        }
    }

    /**
     * Log the defect into the TeamCity build log.
     *
     * @param count
     * @param defect
     */
    protected void LogDefect(Integer count, RigorApiDefectResult defect) {
        String message = count + ". " + defect.severity + " severity defect '" + defect.name + "' (" + defect.defect_id.toString() + "): ";
        message += defect.defect_url_guest;
        logger.message(message);
    }

    private void PublishArtifacts() {
        File buildDirectory = new File(build.getBuildTempDirectory() + "/" + build.getProjectName() + "/" + build.getBuildTypeName() + "/" + build.getBuildNumber() + "/" + Constants.REPORT_FOLDER);
        File file = new File(buildDirectory, Constants.REPORT_FILENAME);

        try {
            // Todo: finish and implement this for future version of the TeamCity plugin.
            FileUtils.touch(file);
            appendStringToFile(file, logger.toString());
        } catch (IOException e) {
            logger.warning("Failed to generate Rigor Optimization build report: " + e.getMessage());
            return;
        }

        artifacts.addNewArtifactsPath(file + "=>" + Constants.RUNNER_NAME);
    }

    private BuildFinishedStatus StartOptimization(@NotNull AgentRunningBuild build, @NotNull BuildRunnerContext context, ArtifactsWatcher artifacts) {

        String buildNumber = context.getConfigParameters().get("build.number");

        final OptimizationConstants constants = OptimizationConstants.Instance;
        final Map<String, String> parameters = getContext().getRunnerParameters();

        parameters.forEach((key, value) -> logger.message(key + " : " + value));

        //

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    private BuildProblemData CreateBuildProblem(@NotNull String description) {
        String error = format("%s (Step: %s)", description, OptimizationConstants.RUNNER_NAME);
        return BuildProblemData.createBuildProblem(valueOf(description.hashCode()), TC_ERROR_MESSAGE_TYPE, error);
    }
}