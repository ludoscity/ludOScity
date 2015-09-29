package com.ludoscity.bikeactivityexplorer.Citybik_esAPI;

import com.ludoscity.bikeactivityexplorer.Citybik_esAPI.model.StatusAnswerRoot;

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

    @GET("{href}")
    Call<StatusAnswerRoot> getStatusForNetwork(@Path("href") String href, @QueryMap Map<String, String> options);
}

//http://api.citybik.es
//http://api.citybik.es/v2/networks/bixi-montreal?fields=stations