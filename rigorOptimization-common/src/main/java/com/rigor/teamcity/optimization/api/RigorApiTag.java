package com.rigor.teamcity.optimization.api;

public class RigorApiTag {

    /**
     * The maximum tag string length.
     */
    public static final int MaxTagLength = 80;

    public String name;

    /**
     * Support Rigor values are:
     *
     * Low
     * Medium
     * High
     */
    public String priority;

    public RigorApiTag() {
        this.priority = "Medium";
    }
}
