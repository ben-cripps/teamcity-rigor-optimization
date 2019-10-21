package com.rigor.teamcity.optimization;

public class OptimizationConstants {

    public final static OptimizationConstants Instance = new OptimizationConstants();

    // These are plugin parameters set via the TeamCity user.
    public String getApiKey() { return "API_KEY"; }
    public String getTestIds() { return "TEST_IDS"; }
    public String getMaximumCriticalDefectsAllowed() { return "MAXIMUM_CRITICAL_DEFECTS_ALLOWED"; }
    public String getMinimumPerformanceScore() { return "MINIMUM_PERFORMANCE_SCORE"; }
    public String getDisallowedDefectIds() { return "DISALLOWED_DEFECT_IDS"; }
    public String getVerifyPerformanceBudgets() { return "VERIFY_PERFORMANCE_BUDGETS"; }

    // The below are final and are static for the plugin.
    public static final String API_ENDPOINT = "https://optimization-api.rigor.com/v2/";
    public static final String RUNNER_DESCRIPTION = "Rigor plugin to execute and validate against Rigor optimization test results.";
    public static final String RUNNER_NAME = "Rigor Optimization";
    public static final String RUNNER_TYPE = "optimizationRunner";
    public static final String REPORT_FOLDER = "Rigor";
    public static final String REPORT_FILENAME = "Optimization Report.html";
    public static final String PERFORMANCE_BUDGET_DEFECTS = "490,487,491,485,489,492,497,486,496,493,495,488,494,498,467,470,469,466,468,471,450,453,452,449,451,454,472";
    public static final Integer TIMEOUT_IN_SECONDS = 240;
}
