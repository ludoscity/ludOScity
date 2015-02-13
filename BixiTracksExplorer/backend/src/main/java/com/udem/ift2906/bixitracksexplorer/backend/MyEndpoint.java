/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.udem.ift2906.bixitracksexplorer.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

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
