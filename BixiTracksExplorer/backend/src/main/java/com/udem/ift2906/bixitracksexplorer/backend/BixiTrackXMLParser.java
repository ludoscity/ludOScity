package com.udem.ift2906.bixitracksexplorer.backend;

import com.sun.jndi.toolkit.url.Uri;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Stack;
import java.util.concurrent.CancellationException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;
import sun.rmi.runtime.Log;

/**
 * Created by F8Full on 2015-02-12.
 * This object parse one BixiTrack XML file
 */
public class BixiTrackXMLParser extends DefaultHandler {

    private Stack xmlObjectStack;


    private Track toReturn; //I'm somehow disturbed to do it like that

    public BixiTrackXMLParser()
    {
        xmlObjectStack = new Stack();
        toReturn = new Track();
    }

    //This is a one block for all stations right now, I'll probably cut it later
    //to extract the import of only a single station (which will have an associated uri)
    //It returns the stations uri if data have been read from input
    public Track readFromFile(String fileFullPath)
    {
        fileFullPath="WEB-INF/Bixi__TrajetTrack201205180952.gpx";
        int eventType;
        Uri result = null;
//     <stations>
//        <station>
//          <id>1</id>
//          <name>Notre Dame / Place Jacques Cartier</name>
//          <terminalName>6001</terminalName>
//          <lat>45.508183</lat>
//          <long>-73.554094</long>
//          <installed>true</installed>
//          <locked>false</locked>
//          <installDate>1276012920000</installDate>
//          <removalDate />
//          <temporary>false</temporary>
//          <nbBikes>14</nbBikes>
//          <nbEmptyDocks>17</nbEmptyDocks>
//        </station>
//        <station>
//          ...
//        </station>
//     </stations>

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            //factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            parser.parse(new File(fileFullPath),this);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        /////////////////////////////////////////////////////////
        Track truc = new Track();

        truc.name = "TESTWGRGDFH";
        truc.helmet = true;
        truc.rating = 10000;


        return truc;
    }

    @Override
    public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException
    {
        int i = 0;
        ++i;
    }

}
