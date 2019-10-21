package com.rigor.teamcity.optimization;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunner;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.jetbrains.annotations.NotNull;

public class OptimizationAgent implements AgentBuildRunner {

    private AgentBuildRunnerInfo runnerInfo;
    private OptimizationBuildProcess optimizationBuildProcess;
    private BuildAgent build;
    private ArtifactsWatcher artifacts;

    public OptimizationAgent(BuildAgent build, @NotNull final ArtifactsWatcher artifacts) {
        this.build = build;
        this.artifacts = artifacts;
    }

    @Override
    @NotNull
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild agent, @NotNull BuildRunnerContext context) throws RunBuildException {
        try {
            optimizationBuildProcess = new OptimizationBuildProcess(agent, context, this.artifacts);
        } catch (RuntimeException e) {
            throw new RunBuildException("Failed to create Rigor Optimization build.", e);
        }

        return optimizationBuildProcess;
    }

    @Override
    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo() {
        runnerInfo = new AgentBuildRunnerInfo() {

            @Override
            @NotNull
            public String getType() {
                return OptimizationConstants.RUNNER_TYPE;
            }

            @Override
            public boolean canRun(@NotNull BuildAgentConfiguration buildAgentConfig) {
                return true;
            }
        };

        return runnerInfo;
    }
}