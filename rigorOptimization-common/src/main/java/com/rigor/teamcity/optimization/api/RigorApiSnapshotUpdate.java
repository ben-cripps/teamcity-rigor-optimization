package com.rigor.teamcity.optimization.api;

import java.util.ArrayList;

public class RigorApiSnapshotUpdate {

    /**
     * The update tag value.
     */
    public String tag_update;

    /**
     * Array list of snapshot ID values.
     */
    public ArrayList<Integer> snapshot_ids;

    /**
     * Array list of Rigor API tags.
     */
    public ArrayList<RigorApiTag> tags;

    public RigorApiSnapshotUpdate() {
        this.tags = new ArrayList<RigorApiTag>();
        this.tag_update = "AddTags";
        this.snapshot_ids = new ArrayList<Integer>();
    }
}
