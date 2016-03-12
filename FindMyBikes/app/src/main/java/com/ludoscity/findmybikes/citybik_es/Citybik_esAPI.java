package com.ludoscity.findmybikes.citybik_es;

import com.ludoscity.findmybikes.citybik_es.model.ListNetworksAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.NetworkStatusAnswerRoot;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Created by F8Full on 2015-09-29.
 * Retrofit interface to access http://api.citybik.es/v2
 */
public interface Citybik_esAPI {

    //Endpoint : //http://api.citybik.es

    //http://api.citybik.es/v2/networks/bixi-montreal?fields=stations
    @GET("{href}")
    Call<NetworkStatusAnswerRoot> getNetworkStatus(@Path("href") String href, @QueryMap Map<String, String> options);
    //http://api.citybik.es/v2/networks/
    @GET("/v2/networks")
    Call<ListNetworksAnswerRoot> listNetworks();
}


