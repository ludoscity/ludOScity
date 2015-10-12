package com.ludoscity.ift2906.bikeactivityexplorer.backend.Data;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Created by F8Full on 2015-02-12.
 * This file is part of BixiTracksExplorer -- Backend
 * This is a data class describing a Track datastore entity
 * It is annotated in that regard and will be use with JDO
 * It describes
 *      - one boolean property (helmet)
 *      - one int property (rating)
 *      - 5 String properties (name, startReason, endReason, startStation, endStation)
 *      - 1 JDO Key constructed from a time UTC String formatted as "yyyy-MM-dd'T'HH:mm:ss'Z'"
 *
 *      - 1 tentative Date field for java.utils.date (future) use
 *      - A ONE TO MANY relationship between itself and a List<TrackPoint> points property
 *          --There might be more option here than 'dependent'
 *
 */
@SuppressWarnings("unused") //For getters required in JSON / JDO serialization process
@PersistenceCapable
@FetchGroup(name="pointskey", members={@Persistent(name="points")})
public class Track {

    //Used by datastore to Id entity
    //The getName method returns the timeUTC string
    //TODO: try using Java DATE object to manipulate timeUTC
    @PrimaryKey
    private Key KEY_timeUTC ;

    //This field is only intended as an internal way to execute queries with time filter
    //directly supported by JDO
    /*Simply declare the dateField as java.util.Date, then use

query.setFilter("dateField < dateParam");
query.declareParameters("java.util.Date dateParam");
List<...> results = (List<...>) query.execute(new java.util.Date());*/
    //http://stackoverflow.com/questions/3600779/google-app-engine-jdo-use-date-in-filter
    @Persistent
    private Date DATE_timeUTC;

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
    @Persistent
    private long duration;

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

        //2012-05-18T13:52:26Z
        //public static final SimpleDateFormat ZULU_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //TimeZone utc = TimeZone.getTimeZone("UTC");
        //format.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
        try {
            DATE_timeUTC = format.parse(timeUTC);
        } catch (ParseException e) {
            e.printStackTrace();
        }
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
