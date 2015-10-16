package com.ludoscity.findmybikes.Citybik_esAPITMP;

import com.ludoscity.findmybikes.Citybik_esAPITMP.model.ListNetworksAnswerRoot;
import com.ludoscity.findmybikes.Citybik_esAPITMP.model.NetworkStatusAnswerRoot;

import java.util.Map;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.QueryMap;

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


