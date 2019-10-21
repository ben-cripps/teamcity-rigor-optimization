package com.rigor.teamcity.optimization;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimizationRunner extends RunType {

    private final PluginDescriptor descriptor;

    public OptimizationRunner(RunTypeRegistry registry, PluginDescriptor descriptor) {
        this.descriptor = descriptor;
        registry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return OptimizationConstants.RUNNER_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return OptimizationConstants.RUNNER_NAME;
    }

    @NotNull
    @Override
    public String getDescription() {
        return OptimizationConstants.RUNNER_DESCRIPTION;
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {

        OptimizationConstants constants = new OptimizationConstants();

        return properties -> {
            final List<InvalidProperty> invalidProperties = new ArrayList<>();

            // Validate the required API key property.
            final String apiKey = properties.get(constants.getApiKey());
            if (apiKey == null) {
                invalidProperties.add(new InvalidProperty(constants.getApiKey(), "A valid Rigor optimization API key is required."));
            }

            // Validate the required optimization test IDs.
            final String testIds = properties.get(constants.getTestIds());
            if (testIds == null) {
                invalidProperties.add(new InvalidProperty(constants.getTestIds(), "One or more valid Rigor optimization test ID values are required."));
            }

            return invalidProperties;
        };
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return descriptor.getPluginResourcesPath("optimizationEditParameters.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return descriptor.getPluginResourcesPath("optimizationViewParameters.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<>();
    }
}