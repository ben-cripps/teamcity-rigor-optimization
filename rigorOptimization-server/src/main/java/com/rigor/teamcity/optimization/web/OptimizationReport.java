package com.rigor.teamcity.optimization.web;

// Todo: implement report tab into TeamCity results - to do this, simply uncomment the below and implement logging to capture any data you want to display there via an artifact file.

import com.intellij.openapi.util.io.StreamUtil;
import com.rigor.teamcity.optimization.OptimizationConstants;
import jetbrains.buildServer.serverSide.BuildsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactHolder;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.web.openapi.BuildTab;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class OptimizationReport extends BuildTab {

    protected OptimizationReport(WebControllerManager manager, BuildsManager buildManager, PluginDescriptor descriptor) {
        super("optimizationReportTab", "Rigor Optimization", manager, buildManager,
                descriptor.getPluginResourcesPath("optimizationResults.jsp"));
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model, @NotNull SBuild build) {

        final BuildArtifacts buildArtifacts = build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT);
        final BuildArtifactHolder artifact = buildArtifacts.findArtifact(OptimizationConstants.REPORT_FOLDER + " " + OptimizationConstants.REPORT_FILENAME);

        if (artifact.isAvailable()) {
            try {
                final String text = StreamUtil.readText(artifact.getArtifact().getInputStream());
                model.put("text", text);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}