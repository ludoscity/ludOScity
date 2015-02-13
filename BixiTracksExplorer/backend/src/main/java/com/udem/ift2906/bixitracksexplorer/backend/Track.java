package com.udem.ift2906.bixitracksexplorer.backend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by F8Full on 2015-02-12.
 */
public class Track {
    public String name;
    public String timeUTC;

    public boolean helmet;
    public String startReason;
    public String endReason;
    public String startStationName;
    public String endStationName;
    public int rating;

    public List<TrackPoint> points = new ArrayList<TrackPoint>();
}
