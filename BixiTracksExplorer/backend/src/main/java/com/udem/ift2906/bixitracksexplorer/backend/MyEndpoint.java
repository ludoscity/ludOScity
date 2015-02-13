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

/**
 * An endpoint class we are exposing
 */
@Api(name = "bixiTracksAPI", version = "v1", namespace = @ApiNamespace(ownerDomain = "backend.bixitracksexplorer.ift2906.udem.com", ownerName = "backend.bixitracksexplorer.ift2906.udem.com", packagePath = ""))
public class MyEndpoint {

    /**
     * A simple endpoint method that takes a name and says Hi back
     */
    @ApiMethod(name = "sayHi")
    public MyBean sayHi(@Named("name") String name) {
        MyBean response = new MyBean();
        response.setData("Hi, " + name);

        return response;
    }

    @ApiMethod(name = "getAll")
    public List<Track> getAll()
    {
        List<Track> toReturn = new ArrayList<Track>();
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

        for (int i=0; i < listOfFiles.length; ++i)
        {
            //should I reuse the same parser or create one each time ?

            //TEST 1 : One parser per file : 2200ms - 360 -- IMPLEMENTED
            //TEST 2 : One parser to parse them all : 2600 - 900
            BixiTrackXMLParser parser = new BixiTrackXMLParser();
            toReturn.add( parser.readFromFile(FOLDER_PATH + listOfFiles[i].getName()));

        }

        return toReturn;
    }

    @ApiMethod(name = "getTrack")
    public Track getTrack()
    {

        BixiTrackXMLParser parser = new BixiTrackXMLParser();
        return parser.readFromFile("pouet");

        /*Track truc = new Track();

        truc.name = "TEST";
        truc.helmet = true;
        truc.rating = 10;


        return truc;*/
    }

}
