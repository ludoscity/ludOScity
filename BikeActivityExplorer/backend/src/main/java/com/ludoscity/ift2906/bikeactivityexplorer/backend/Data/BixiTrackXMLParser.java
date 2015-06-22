package com.ludoscity.ift2906.bikeactivityexplorer.backend.Data;

import com.google.appengine.api.datastore.Key;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by F8Full on 2015-02-12.
 * This object parse one BixiTrack XML file
 */
public class BixiTrackXMLParser extends DefaultHandler {

    //To work with Sax way of handling things
    private StringBuffer mBufferedString;

    //There is a duplicate of <time> tag in XML source
    //in <metadata>, for a track, and in each <trkpt>
    private boolean mParsingTrkpt = false;

    //We accumulate data before adding it to toReturn on <trkpt> end element event
    private TrackPoint mTempTrackPoint;

    //The whole object from the parsing
    private Track mTrackToReturn; //I'm somehow disturbed to do it like that

    public BixiTrackXMLParser()
    {
        mTrackToReturn = new Track();
    }

    //This is a one block for all stations right now, I'll probably cut it later
    //to extract the import of only a single station (which will have an associated uri)
    //It returns the stations uri if data have been read from input
    public Track readFromFile(String _fileFullPath)
    {
/*
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<gpx version="1.1" creator="nl.sogeti.android.gpstracker" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/gpx/1/1/gpx.xsd" xmlns="http://www.topografix.com/GPX/1/1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:gpx10="http://www.topografix.com/GPX/1/0" xmlns:ogt10="http://gpstracker.android.sogeti.nl/GPX/1/0" xmlns:f8f10="http://bixitracker.android.f8full.net/GPX/1/0">
<metadata>
<time>2012-05-18T13:52:26Z</time>
</metadata>
<trk>
<name>Bixi__Trajet|Track 2012-05-18 09:52</name>
<extensions>
<f8f10:helmet>0</f8f10:helmet>
<f8f10:startReason>Home</f8f10:startReason>
<f8f10:endReason>Work</f8f10:endReason>
<f8f10:startStationName>Brébeuf / Laurier</f8f10:startStationName>
<f8f10:endStationName>Saint-Dominique / Saint-Viateur</f8f10:endStationName>
<f8f10:rating>5</f8f10:rating>
</extensions>
<trkseg>
<trkpt lat="45.53264526" lon="-73.58529616">
<ele>20.5</ele>
<time>2012-05-18T13:52:28Z</time>
<extensions>
<gpx10:speed>0.5</gpx10:speed>
<ogt10:accuracy>16.970561981201172</ogt10:accuracy>
<gpx10:course>114.5</gpx10:course></extensions>
</trkpt>
<trkpt lat="45.53258407" lon="-73.58525394">
<ele>25.100000381469727</ele>
<time>2012-05-18T13:52:46Z</time>
<extensions>
<gpx10:speed>0.75</gpx10:speed>
<ogt10:accuracy>7.211102485656738</ogt10:accuracy>
<gpx10:course>146.0</gpx10:course></extensions>
</trkpt>
<!-- UN CERTAIN NOMBRE DE FOIS DÉPENDEMENT DE LA LONGUEUR DE LA TRACK
UN SEUL trkseg par track garanti dans ce cas là. -->
</trkseg>
</trk>
</gpx>
*/


        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            //factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            parser.parse(new File(_fileFullPath),this);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }


        return mTrackToReturn;
    }

    @Override
    public void startElement(String _s, String _s1, String _elementName, Attributes _attributes) throws SAXException
    {
        mBufferedString = new StringBuffer();

        if (_elementName.equalsIgnoreCase("trkpt"))
        {
            mParsingTrkpt = true;

            mTempTrackPoint = new TrackPoint();

            //extract lat lon attributes
            mTempTrackPoint.setLat(Float.parseFloat(_attributes.getValue("lat")));
            mTempTrackPoint.setLon(Float.parseFloat(_attributes.getValue("lon")));

        }
    }

    @Override
    public void endElement(String _s, String _s1, String _element) throws SAXException
    {
        if (mParsingTrkpt)
        {
            if (_element.equalsIgnoreCase("ele"))
            {
                mTempTrackPoint.setEle(Float.parseFloat(mBufferedString.toString()));

            }
            else if (_element.equalsIgnoreCase("time"))
            {
                Key key = mTrackToReturn.builChildKey(TrackPoint.class.getSimpleName(), mBufferedString.toString());
                mTempTrackPoint.setTimeUTC(key);
                //mTempTrackPoint.setTimeUTC(mBufferedString.toString());

            }
            else if (_element.equalsIgnoreCase("gpx10:speed"))
            {
                mTempTrackPoint.setSpeed(Float.parseFloat(mBufferedString.toString()));

            }
            else if (_element.equalsIgnoreCase("ogt10:accuracy"))
            {
                mTempTrackPoint.setAccurary(Float.parseFloat(mBufferedString.toString()));

            }
            else if (_element.equalsIgnoreCase("gpx10:course"))
            {
                mTempTrackPoint.setHeading(Float.parseFloat(mBufferedString.toString()));

            }
            else if (_element.equalsIgnoreCase("trkpt"))
            {
                mTrackToReturn.getPoints().add(mTempTrackPoint);
                mParsingTrkpt = false;
            }

        }
        else if (_element.equalsIgnoreCase("name"))
        {
            mTrackToReturn.setName(mBufferedString.toString());
        }
        else if (_element.equalsIgnoreCase("time"))
        {
           mTrackToReturn.setTimeUTC(mBufferedString.toString());
        }
        else if (_element.equalsIgnoreCase("f8f10:helmet"))
        {
            String value = mBufferedString.toString();

            mTrackToReturn.setHelmet(!value.equalsIgnoreCase("0"));
        }
        else if (_element.equalsIgnoreCase("f8f10:startreason"))
        {
            mTrackToReturn.setStartReason(mBufferedString.toString());
        }
        else if (_element.equalsIgnoreCase("f8f10:endreason"))
        {
            mTrackToReturn.setEndReason(mBufferedString.toString());
        }
        else if (_element.equalsIgnoreCase("f8f10:startstationname"))
        {
            mTrackToReturn.setStartStationName(mBufferedString.toString());
        }
        else if (_element.equalsIgnoreCase("f8f10:endStationName"))
        {
            mTrackToReturn.setEndStationName(mBufferedString.toString());
        }
        else if (_element.equalsIgnoreCase("f8f10:rating"))
        {
            mTrackToReturn.setRating(Integer.parseInt(mBufferedString.toString()));
        }
    }

    @Override
    public void characters(char[] _ac, int i, int j) throws SAXException {

        mBufferedString.append(new String(_ac, i, j));

    }


}
