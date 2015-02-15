package com.udem.ift2906.bixitracksexplorer.backend;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Created by F8Full on 2015-02-12.
 */
@PersistenceCapable
public class TrackPoint {

    @PrimaryKey
    private Key timeUTC;

    @Persistent
    private float lat;
    @Persistent
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
