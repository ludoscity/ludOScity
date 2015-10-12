package com.ludoscity.findmybikes.Budget;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by F8Full on 2015-04-07.
 * Data class used to create a custom listView in BudgetInfoFragment.
 * It implements Parcelable to be able to pass it around between fragments
 */
public class BudgetInfoItem implements Parcelable {
    private String mTrackID;  //This is the date
    private float mCost;
    private String mStartStationName;
    private String mEndStationName;
    private long mDuration;

    public BudgetInfoItem(String _trackID, float _cost, String _startStationName, String _endStationName, long _duration) {
        this.mTrackID = _trackID;
        this.mCost = _cost;
        this.mStartStationName = _startStationName;
        this.mEndStationName = _endStationName;
        this.mDuration = _duration;
    }

    public BudgetInfoItem(Parcel in){
        mTrackID = in.readString();
        mCost = in.readFloat();
        mStartStationName = in.readString();
        mEndStationName = in.readString();
        mDuration = in.readLong();
    }

    public long getIDAsLong(){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //TimeZone utc = TimeZone.getTimeZone("UTC");
        //format.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true

        try {
            Date startDate = format.parse(mTrackID);
            return startDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public String getIDAsString(){
        return mTrackID;
    }

    public Date getTimestampAsDate(){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Date toReturn = null;

        try {
            toReturn = format.parse(mTrackID);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public float getCost(){
        return mCost;
    }

    public String getStartStationName() {
        return mStartStationName;
    }

    public String getEndStationName() {
        return mEndStationName;
    }

    @Override   //From Parcelable interface
    public int describeContents() {
        return 0;
    }

    @Override   //From Parcelable interface
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTrackID);
        dest.writeFloat(mCost);
        dest.writeString(mStartStationName);
        dest.writeString(mEndStationName);
        dest.writeLong(mDuration);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public BudgetInfoItem createFromParcel(Parcel source) {
            return new BudgetInfoItem(source);
        }

        @Override
        public BudgetInfoItem[] newArray(int size) {
            return new BudgetInfoItem[size];
        }
    };

    public int getDurationInMinutes() {
        return (int)((mDuration/1000L)/60L);
    }
}
