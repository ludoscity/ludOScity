package com.udem.ift2906.bixitracksexplorer.backend;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Created by F8Full on 2015-02-12.
 */
@PersistenceCapable
@FetchGroup(name="pointskey", members={@Persistent(name="points")})
public class Track {

    //Used by datastore to Id entity
    //The getName method returns the timeUTC string
    //TODO: try using Java DATE object to manipulate timeUTC
    @PrimaryKey
    private Key KEY_timeUTC ;

    @Persistent
    private String name;

    @Persistent
    private boolean helmet;
    @Persistent
    private String startReason;
    @Persistent
    private String endReason;
    @Persistent
    private String startStationName;
    @Persistent
    private String endStationName;
    @Persistent
    private int rating;

    @Persistent//(defaultFetchGroup = "true") //So that children get fetch when retrieving parent
    @Element(dependent = "true")    //Children can't exist by themselves in datastore
    private List<TrackPoint> points = new ArrayList<>();

    public Key builChildKey(String childKind, String childKeyName)
    {
        return KeyFactory.createKey(KEY_timeUTC, childKind, childKeyName);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKEY_TimeUTC() {
        return KEY_timeUTC.getName();
    }

    public void setTimeUTC(String timeUTC) {

        this.KEY_timeUTC = KeyFactory.createKey(Track.class.getSimpleName(), timeUTC);
    }

    public boolean isHelmet() {
        return helmet;
    }

    public void setHelmet(boolean helmet) {
        this.helmet = helmet;
    }

    public String getStartReason() {
        return startReason;
    }

    public void setStartReason(String startReason) {
        this.startReason = startReason;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public String getStartStationName() {
        return startStationName;
    }

    public void setStartStationName(String startStationName) {
        this.startStationName = startStationName;
    }

    public String getEndStationName() {
        return endStationName;
    }

    public void setEndStationName(String endStationName) {
        this.endStationName = endStationName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public List<TrackPoint> getPoints() {
        return points;
    }

    public void setPoints(List<TrackPoint> points) {
        this.points = points;
    }
}
