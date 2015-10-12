/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.ludoscity.ift2906.bikeactivityexplorer.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.BixiTrackXMLParser;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.MyBean;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.PMF;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.TrackPoint;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Responses.GetTrackFromTimeUTCKeyStringResponse;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Responses.ListTracksResponse;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Responses.TestMetaResponse;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Utils.Utils;
import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.Track;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

/**
 * An endpoint class we are exposing
 */
@Api(name = "bixiTracksExplorerAPI", version = "v3", namespace = @ApiNamespace(ownerDomain = "backend.bixitracksexplorer.ift2906.udem.com", ownerName = "backend.bixitracksexplorer.ift2906.udem.com", packagePath = ""))
public class BixiTracksExplorerEndpoint {

    /**
     * A simple endpoint method that takes a name and says Hi back
     */
    @ApiMethod(name = "sayHi")
    public MyBean sayHi(@Named("name") String _name) {
        MyBean response = new MyBean();
        response.setData("Hi, " + _name);

        return response;
    }

    /**
     * TEST Meta
     */
    @ApiMethod(name = "testMeta")
    public TestMetaResponse testMeta() {

        TestMetaResponse response = new TestMetaResponse();

        Utils.ResultMeta.addLicense(response.meta);

        //For a rapid demo of the JSONObject interface
        response.meta.put("testMetaString", "testMeta");
        response.meta.put("testMetaBool", true);
        response.meta.put("testMetaInt", 666);

        return response;
    }
    //TODO: Types for results (ie : ListTracksResponse, GetTrackFromTimeUTCKeyDateResult, ...)
    //This retrieves all tracks omitting points
    //Use it to get to Track(s) key(s) which are timeUTC
    @SuppressWarnings("unchecked")  //responseList = (List<Track>) q.execute();
    @ApiMethod(name = "listTracks")
    public ListTracksResponse listTracks()
    {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        //Starting to get a grasp of fetchgroup
        //http://db.apache.org/jdo/fetchgroups.html
        //pm.getFetchPlan().addGroup("pointskey");


        Query q = pm.newQuery(Track.class);


        q.setDatastoreReadTimeoutMillis(10000);

        //q.setFilter("lastName == lastNameParam");
        q.setOrdering("DATE_timeUTC asc");
        //q.declareParameters("String lastNameParam");

        List<Track> responseList = new ArrayList<>();

        try {

            responseList = (List<Track>) q.execute();
        }finally {
            pm.close();
        }

        ListTracksResponse response = new ListTracksResponse(responseList);

        Utils.ResultMeta.addLicense(response.meta);

        return response;
    }

    @ApiMethod(name = "DebugOneTrack")
    public Track DebugOneTrack() throws ParseException {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //TimeZone utc = TimeZone.getTimeZone("UTC");
        //format.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true

        Date asDate = format.parse("2012-04-02T18:40:46Z");

        //String dateAsString = format.format(asDate);

        return new Track();//GetTrackFromTimeUTCKeyDate(asDate);


    }

    @ApiMethod(name = "DebugDateRange")
    public List<Track> DebugDateRange() throws ParseException {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //TimeZone utc = TimeZone.getTimeZone("UTC");
        //format.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true

        Date startDate =
                format.parse("2012-04-02T20:30:58Z");
        Date endDate =
                format.parse("2012-04-04T01:02:48Z");


        return GetTracksForDateRange(startDate, endDate);
    }

    @SuppressWarnings({"unchecked",//response = (List<Track>)q.execute(_startDate, _endDate);
            "unused"})  //float touch = point.getLat();
    @ApiMethod(name = "getTracksForDateRange")
    public List<Track> GetTracksForDateRange(@Named("StartDate") Date _startDate, @Named("EndDate") Date _endDate)
    {
        //TODO : check in end date is posterior to start one
        List<Track> response = new ArrayList<>();

        PersistenceManager pm = PMF.get().getPersistenceManager();

        /*Simply declare the dateField as java.util.Date, then use

query.setFilter("dateField < dateParam");
query.declareParameters("java.util.Date dateParam");
List<...> results = (List<...>) query.execute(new java.util.Date());*/

        //Starting to get a grasp of fetchgroup
        //http://db.apache.org/jdo/fetchgroups.html
        //pm.getFetchPlan().addGroup("pointskey");
        //pm.getFetchPlan.addGroup("pointsData");


        Query q = pm.newQuery(Track.class);
        q.setFilter("DATE_timeUTC >= _startDate && DATE_timeUTC <= _endDate");
        q.declareParameters("java.util.Date _startDate, java.util.Date _endDate");
        q.setOrdering("DATE_timeUTC asc");

        q.setDatastoreReadTimeoutMillis(10000);

        try
        {
            response = (List<Track>)q.execute(_startDate, _endDate);

            for (Track track : response)
            {
                for (TrackPoint point:track.getPoints())
                {
                    //So that datastore loads ALL trackPoint data fields
                    float touch = point.getLat();
                }
            }
        }
        finally {
            pm.close();
        }


        //q.setFilter("lastName == lastNameParam");

        return response;
    }

    //Accepts Date object directly as parameter
    //
    /*@ApiMethod(name = "getTrackFromTimeUTCKeyDate")
    public Track GetTrackFromTimeUTCKeyDate(@Named("TimeUTCDate") Date timeUTCDate) {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //TimeZone utc = TimeZone.getTimeZone("UTC");
        //format.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true

        //String debug = format.format(timeUTCDate);

        return GetTrackFromTimeUTCKeyString(format.format(timeUTCDate));
    }*/

    //Retrieve by timeUTC key string
    //This function retrieves one track by key
    //TWO OPTIONS :
    // --Give the full points data
    // --Give only a partial reading (points keys ??) IDEA : points lat/long

    //This version retrieves all TrackPoints data
    @SuppressWarnings("unused")  //float touch = point.getLat();
    @ApiMethod(name = "getTrackFromTimeUTCKeyString")
    public GetTrackFromTimeUTCKeyStringResponse GetTrackFromTimeUTCKeyString(@Named("TimeUTCDate") String _timeUTCDate) {

        PersistenceManager pm = PMF.get().getPersistenceManager();


        //Build key from received data
        Key k = KeyFactory.createKey(Track.class.getSimpleName(), _timeUTCDate);//format.format(_timeUTCDate));

        pm.getFetchPlan().addGroup("pointskey");
        //pm.getFetchPlan().addGroup("pointsgeodata");

        Track responseTrack = new Track();
        int touchedPoints = 0; //meta will be filled with that information
        //Retrieves corresponding object in the datastore
        try
        {
            responseTrack = pm.getObjectById(Track.class, k);

            //Touching all track points to get them loaded (UGLY) TODO: find a better way to handle this
            //Because we have to do it, we might as well count on the way and put it in response meta object
            //int touchedPoints = 0;
            for (TrackPoint point : responseTrack.getPoints())
            {
                //So that datastore lazy loading in forced to get ALL TrackPoint data fields
                float touch = point.getLat();
                ++touchedPoints;
            }
        }
        finally {
            pm.close();
        }

        GetTrackFromTimeUTCKeyStringResponse response = new GetTrackFromTimeUTCKeyStringResponse(responseTrack);

        Utils.ResultMeta.addLicense(response.meta);
        response.meta.put("nb_touched_points",touchedPoints);


        return response;
    }

    //

    //This retrieve all tracks with associated data (Bd dump)
//    @SuppressWarnings("unchecked")  //return (List<Track>) q.execute();
//    @ApiMethod(name = "getAllFromDatastore")
//    public List<Track> GetAllFromDatastore()
//    {
//        PersistenceManager pm = PMF.get().getPersistenceManager();
//
//        //Starting to get a grasp of fetchgroup
//        //http://db.apache.org/jdo/fetchgroups.html
//        pm.getFetchPlan().addGroup("pointskey");
//        //pm.getFetchPlan.addGroup("pointsData");
//
//
//        Query q = pm.newQuery(Track.class);
//
//
//        q.setDatastoreReadTimeoutMillis(10000);
//
//        //q.setFilter("lastName == lastNameParam");
//        q.setOrdering("KEY_timeUTC desc");
//        //q.declareParameters("String lastNameParam");
//
//        try {
//            return (List<Track>) q.execute();
//        }finally {
//            pm.close();
//        }
//    }

//    @ApiMethod(name = "getAllfromXML")
//    public List<Track> getAllFromXML()
//    {
//        List<Track> toReturn = new ArrayList<>();
//        //list all files in folder
//
//        final String FOLDER_PATH = "WEB-INF/";
//
//        FilenameFilter filter = new FilenameFilter() {
//            @Override
//            public boolean accept(File file, String s) {
//                return s.contains(".gpx");
//            }
//        };
//
//        File folder = new File(FOLDER_PATH);
//        File[] listOfFiles = folder.listFiles(filter);
//
//
//        //for each
//        //-invoke XML parser and put result in toReturn
//
//        //BixiTrackXMLParser parser = new BixiTrackXMLParser();
//
//        for (File file : listOfFiles) {
//            //should I reuse the same parser or create one each time ?
//
//            //TEST 1 : One parser per file : 2200ms - 360 -- IMPLEMENTED
//            //TEST 2 : One parser to parse them all : 2600 - 900
//            BixiTrackXMLParser parser = new BixiTrackXMLParser();
//            toReturn.add(parser.readFromFile(FOLDER_PATH + file.getName()));
//
//        }
//
//        return toReturn;
//    }

    @ApiMethod(name = "loadTracksFromXML")
    public List<Track> loadTracksFromXML(@Named("startIdx") int _startIdx, @Named("howMany") int _howMany) throws ParseException {
        List<Track> response = new ArrayList<>();

        final String FOLDER_PATH = "WEB-INF/TracksGPXFiles/";

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(".gpx");
            }
        };

        File folder = new File(FOLDER_PATH);
        File[] listOfFiles = folder.listFiles(filter);



       for (int i=_startIdx; i<_startIdx+_howMany; ++i)
       {
           PersistenceManager pm = PMF.get().getPersistenceManager();

           Track newTrack;

           if (i < listOfFiles.length)
           {
               File trackFile = listOfFiles[i];


               BixiTrackXMLParser parser = new BixiTrackXMLParser();

               newTrack = parser.readFromFile(FOLDER_PATH + trackFile.getName());

               DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
               //TimeZone utc = TimeZone.getTimeZone("UTC");
               //format.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true

               Date startDate = format.parse(newTrack.getKEY_TimeUTC());
               Date endDate = format.parse(newTrack.getPoints().get(newTrack.getPoints().size()-1).getTimeUTC());

               newTrack.setDuration(endDate.getTime()-startDate.getTime());

               try {

                   Key k = KeyFactory.createKey(Track.class.getSimpleName(), newTrack.getKEY_TimeUTC());
                   pm.getObjectById(Track.class, k);

                   //We get there only if the Track was already in the datastore
                   //newTrack.setTimeUTC("ALREADY IN DB, NOT ADDED");
                   newTrack.setName("ALREADY IN DB, NOT ADDED");
               }
               catch (JDOObjectNotFoundException e)
               {
                   //Not in the datastore, let's add it
                   pm.makePersistent(newTrack);
               }
               finally {
                   response.add(newTrack);

                   pm.close();
               }
           }
       }

        return response;
    }
}
