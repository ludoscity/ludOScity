/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.udem.ift2906.bixitracksexplorer.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

/**
 * An endpoint class we are exposing
 */
@Api(name = "bixiTracksExplorerAPI", version = "v1", namespace = @ApiNamespace(ownerDomain = "backend.bixitracksexplorer.ift2906.udem.com", ownerName = "backend.bixitracksexplorer.ift2906.udem.com", packagePath = ""))
public class BixiTracksExplorerEndpoint {

    /**
     * A simple endpoint method that takes a name and says Hi back
     */
    @ApiMethod(name = "sayHi")
    public MyBean sayHi(@Named("name") String name) {
        MyBean response = new MyBean();
        response.setData("Hi, " + name);

        return response;
    }
    //This retrieves all tracks ommiting points
    //Use it to get to Track(s) key(s) which are timeUTC
    @SuppressWarnings("unchecked")  //return (List<Track>) q.execute();
    @ApiMethod(name = "listTracks")
    public List<Track> listTracks()
    {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        //Starting to get a grasp of fetchgroup
        //http://db.apache.org/jdo/fetchgroups.html
        //pm.getFetchPlan().addGroup("pointskey");


        Query q = pm.newQuery(Track.class);


        q.setDatastoreReadTimeoutMillis(10000);

        //q.setFilter("lastName == lastNameParam");
        q.setOrdering("KEY_timeUTC desc");
        //q.declareParameters("String lastNameParam");

        try {
            return (List<Track>) q.execute();
        }finally {
            pm.close();
        }
    }

    //This retrieve all tracks with associated data (Bd dump)
    @SuppressWarnings("unchecked")  //return (List<Track>) q.execute();
    @ApiMethod(name = "getAllFromDatastore")
    public List<Track> GetAllFromDatastore()
    {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        //Starting to get a grasp of fetchgroup
        //http://db.apache.org/jdo/fetchgroups.html
        pm.getFetchPlan().addGroup("pointskey");
        //pm.getFetchPlan.addGroup("pointsData");


        Query q = pm.newQuery(Track.class);


        q.setDatastoreReadTimeoutMillis(10000);

        //q.setFilter("lastName == lastNameParam");
        q.setOrdering("KEY_timeUTC desc");
        //q.declareParameters("String lastNameParam");

        try {
            return (List<Track>) q.execute();
        }finally {
            pm.close();
        }
    }

    @ApiMethod(name = "getAllfromXML")
    public List<Track> getAllFromXML()
    {
        List<Track> toReturn = new ArrayList<>();
        //list all files in folder

        final String FOLDER_PATH = "WEB-INF/";

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(".gpx");
            }
        };

        File folder = new File(FOLDER_PATH);
        File[] listOfFiles = folder.listFiles(filter);


        //for each
        //-invoke XML parser and put result in toReturn

        //BixiTrackXMLParser parser = new BixiTrackXMLParser();

        for (File file : listOfFiles) {
            //should I reuse the same parser or create one each time ?

            //TEST 1 : One parser per file : 2200ms - 360 -- IMPLEMENTED
            //TEST 2 : One parser to parse them all : 2600 - 900
            BixiTrackXMLParser parser = new BixiTrackXMLParser();
            toReturn.add(parser.readFromFile(FOLDER_PATH + file.getName()));

        }

        return toReturn;
    }
}
