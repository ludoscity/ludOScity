package com.udem.ift2906.bixitracksexplorer.BixiAPI;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by Gevrai on 15-03-26.
 *
 * API simpliste qui download le fichier JSON de bixi-montreal sur citybik.es et l'enregistre dans
 * les sharedPreferences pour accès hors ligne
 *
 * TODO Create Bixi class from json.
 *
 */

public class BixiAPI{
    private String url = "http://api.citybik.es/v2/networks/bixi-montreal?fields=stations";
    private String dataName = "DATA";
    private String preferenceName = "BixiStationsData";
    private BixiNetwork bixiNetwork;
    Context context;

    public BixiAPI(Context _context){
        context = _context;
        if (isWifiConnected())
            tryDownloadingDataFromApiIntoSharedPref();
        buildNetworkFromJSON();
    }

    public BixiNetwork getBixiNetwork() {
        return bixiNetwork;
    }

    public String getJSonDataFromSharedPref(){
        return context.getSharedPreferences(dataName,Context.MODE_PRIVATE).getString(preferenceName,null);
    }

    private void buildNetworkFromJSON() {

        String jsonData = getJSonDataFromSharedPref();
        Gson gson = new GsonBuilder().create();
        bixiNetwork = gson.fromJson(jsonData, BixiNetwork.class);
    }

    public boolean isWifiConnected(){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public boolean tryDownloadingDataFromApiIntoSharedPref(){

        try {
            HttpEntity page = getHttp(url);
            String data = EntityUtils.toString(page, HTTP.UTF_8);
            SharedPreferences preferences = context.getSharedPreferences(dataName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(preferenceName, data);
            editor.commit();
            return true;

        } catch (ClientProtocolException e1) {
            e1.printStackTrace();
            return false;
        }  catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public HttpEntity getHttp(String url) throws ClientProtocolException, IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet http = new HttpGet(url);
        HttpResponse response = httpClient.execute(http);
        return response.getEntity();
    }

}
