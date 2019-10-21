package com.rigor.teamcity.optimization;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class OptimizationBuildProcess implements BuildProcess {

    private AgentRunningBuild build;
    private BuildRunnerContext context;
    private ArtifactsWatcher artifacts;
    private BuildProgressLogger logger;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private OptimizationProcess optimizationProcess;
    private Future<BuildFinishedStatus> processFuture;

    public OptimizationBuildProcess(AgentRunningBuild build, BuildRunnerContext context, ArtifactsWatcher artifacts) {
        this.build = build;
        this.context = context;
        this.artifacts = artifacts;
        this.logger = build.getBuildLogger();
        this.optimizationProcess = new OptimizationProcess(build, context, artifacts);
    }

    @Override
    public void start() throws RunBuildException {
        processFuture = executor.submit(this.optimizationProcess);
    }

    @Override
    public void interrupt() {
        logger.message("Interrupting build request.");

        if (processFuture != null) {
            processFuture.cancel(true);
        }
    }

    @Override
    public boolean isFinished() {
        logger.message("Build step finished.");
        return processFuture.isDone();
    }

    @Override
    public boolean isInterrupted() {
        logger.message("Build interrupt request completed.");
        return processFuture.isCancelled() && isFinished();
    }

    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {
        try {
            return processFuture.get();
        } catch (final InterruptedException | CancellationException e) {
            logger.message("The wait for finish has been interrupted: " + e.getMessage());
            return BuildFinishedStatus.INTERRUPTED;
        } catch (final ExecutionException e) {
            logger.message("An exception has been caught while waiting for the build to complete: " + e.getMessage());
            return BuildFinishedStatus.FINISHED_FAILED;
        } finally {
            executor.shutdown();
        }
    }
}