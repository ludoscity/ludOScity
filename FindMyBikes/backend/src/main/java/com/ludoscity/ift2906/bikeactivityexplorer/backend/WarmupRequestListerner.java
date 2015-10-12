package com.ludoscity.ift2906.bikeactivityexplorer.backend;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by F8Full on 2015-02-14.
 * Used to automatically import all GPX FILES
 * ONLY WORKS ON LOCAL ENVIRONMENT : TAKES MORE THAN 60s on deployed env. hence killed
 */
public class WarmupRequestListerner implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {

//        //Parse and load data into the datastore
//
//        List<Track> allTracks = new ArrayList<>();
//        //list all files in folder
//
//        final String FOLDER_PATH = "WEB-INF/TracksGPXFiles/";
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
//            allTracks.add(parser.readFromFile(FOLDER_PATH + file.getName()));
//
//        }
//
//        PersistenceManager pm = PMF.get().getPersistenceManager();
//
//       try
//       {
//          /* Track machin = new Track();
//           machin.setTimeUTC("blaev5");
//
//           TrackPoint truc = new TrackPoint();
//
//           machin.getPoints().add(truc);
//
//
//           truc.setTimeUTC(/*machin.getJODKey(),"BIDON5");*/
//
//
//
//
//
//           //pm.makePersistent(machin);
//           pm.makePersistentAll(allTracks);
//       }catch (Exception e)
//       {
//           e.printStackTrace();
//       }
//        finally {
//           pm.close();
//       }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        //Not called by GAE
    }
}
