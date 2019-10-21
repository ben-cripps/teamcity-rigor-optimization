package com.rigor.teamcity.optimization.api;

import java.util.ArrayList;

public class RigorApiTestUpdate {

    /**
     * The tag update value.
     */
    public String tag_update;

    /**
     * Array list of Rigor API tag objects.
     */
    public ArrayList<RigorApiTag> tags;

    public RigorApiTestUpdate() {
        this.tags = new ArrayList<RigorApiTag>();
        this.tag_update = "AddTags";
    }
}
