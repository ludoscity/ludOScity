package com.udem.ift2906.bixitracksexplorer.BixiAPI;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.udem.ift2906.bixitracksexplorer.DBHelper.DBHelper;
import com.udem.ift2906.bixitracksexplorer.StationItem;
import com.udem.ift2906.bixitracksexplorer.StationsNetwork;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

//import android.widget.Toast;

/**
 * Created by Gevrai on 15-03-26.
 *
 */

public class BixiAPI{
    private String url = "http://api.citybik.es/v2/networks/bixi-montreal?fields=stations";
    Context context;
    BixiNetwork bixiNetwork = null;
    StationsNetwork stationsNetwork = null;

    public BixiAPI(Context _context){
        context = _context;
    }

    public StationsNetwork downloadBixiNetwork() {
        try {
            //TODO Remove: comments on isWifiConnected, Emulator don't have wifi but still have internet
            //if (isWifiConnected()) {
                String data = EntityUtils.toString(getHttp(url), HTTP.UTF_8);

                Gson gson = new GsonBuilder().create();

                bixiNetwork = gson.fromJson(data, BixiNetwork.class);

                stationsNetwork = new StationsNetwork();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss");
                String dateNow = simpleDateFormat.format(new Date());

                for (BixiStation station : bixiNetwork.network.stations) {
                    StationItem stationItem = new StationItem(station, DBHelper.isFavorite(station.extra.uid), dateNow);
                    stationsNetwork.stations.add(stationItem);
                }
            //}
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        new addNetworkDatabase().execute();

        return stationsNetwork;
    }

    public boolean isWifiConnected(){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public HttpEntity getHttp(String url) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet http = new HttpGet(url);
        HttpResponse response = httpClient.execute(http);
        return response.getEntity();
    }

    public class addNetworkDatabase extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                DBHelper.addNetwork(stationsNetwork);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        //Should happen or not on settings fragment/prefs ?
        /*@Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Toast.makeText(context, "DatabaseUpdate Successful!", Toast.LENGTH_LONG).show();
        }*/
    }
}
