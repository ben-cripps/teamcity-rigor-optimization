package com.rigor.teamcity.optimization.api;

public class RigorApiResponse {

    /**
     * The HTTP response status code (defaults to value zero).
     */
    public int HttpStatusCode = 0;

    /**
     * The HTTP status message.
     */
    public String HttpStatusMessage = "";

    /**
     * The HTTP response body.
     */
    public String ResponseBody = "";

    /**
     * The Rigor API error object.
     */
    public RigorApiError RigorError = null;

    /**
     * Flag that determines if the completed Rigor API response was successful or not.
     * @return
     */
    public boolean Success() {
        return (this.HttpStatusCode == 200);
    }

    /**
     * Formats the error message (if any).
     * @return
     */
    public String FormatError() {

        String message = "Server returned status code " + this.HttpStatusCode;

        if (this.HttpStatusMessage.length() > 0) {
            message += " (" + this.HttpStatusMessage + ")";
        }

        if (this.RigorError != null) {
            message += ": " + this.RigorError.Message;
        }

        return message;
    }
}
