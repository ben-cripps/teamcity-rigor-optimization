package com.rigor.teamcity.optimization.api;

public class RigorApiError {

    /* Flag denoting whether an error occurred. */
    public boolean IsError;

    /* Flag denoting success or failure. */
    public boolean IsSuccess;

    /* The error response result. */
    public String Result;

    /* The error response message. */
    public String Message;
}