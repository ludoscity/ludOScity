package com.ludoscity.ift2906.bikeactivityexplorer.backend.Data;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Created by F8Full on 2015-02-12.
 * This file is part of BixiTracksExplorer -- Backend
 * This is a data class describing a TrackPoint datastore entity
 * It is annotated in that regard and will be use with JDO
 * It describes 6 float properties (lat, long, ele, speed, accuracy, heading) and a timestamp name String
 * retrieved through a JDO Key.
 * Though they do have Keys, TrackPoint entities don't exist by themselves in the datastore and
 * can't be searched independently of their parent Track entity. (see Track class file)
 */
@PersistenceCapable
//failed attempt at children loading
//@FetchGroup(name="pointsgeodata", members={@Persistent(name="lat"),
//                                        @Persistent(name="lon"),})
@SuppressWarnings("unused") //getters are indirectly used in JSON serializing process
public class TrackPoint {

    @PrimaryKey
    private Key timeUTC;

    @Persistent//(defaultFetchGroup = "true") failed attempt at children loading
    private float lat;
    @Persistent//(defaultFetchGroup = "true") failed attempt at children loading
    private float lon;
    @Persistent
    private float ele;

    @Persistent
    private float speed;
    @Persistent
    private float accurary;
    @Persistent
    private float heading;

    public String getTimeUTC() {
        return timeUTC.getName();
    }

    //Constructed fully with parent key and timeUTC as name
    public void setTimeUTC(Key fullKey) {
        //Key key = KeyFactory.createKey(ParentKey, Track.class.getSimpleName(), timeUTC);
        //JSON serializing forbids complex setter (KeyFactory is external)
        this.timeUTC = fullKey;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getEle() {
        return ele;
    }

    public void setEle(float ele) {
        this.ele = ele;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getAccurary() {
        return accurary;
    }

    public void setAccurary(float accurary) {
        this.accurary = accurary;
    }

    public float getHeading() {
        return heading;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }
}
