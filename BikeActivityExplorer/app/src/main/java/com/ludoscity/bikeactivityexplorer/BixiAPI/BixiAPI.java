package com.ludoscity.bikeactivityexplorer.BixiAPI;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;
import com.ludoscity.bikeactivityexplorer.StationItem;
import com.ludoscity.bikeactivityexplorer.StationsNetwork;

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
            //TODO Refactor : use retrofit httpclient library
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
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        //TODO : This seems somewhat eyebrow raising : this function is called in a task, and does .execute itself,
        //launching an other task
        new addNetworkDatabase().execute();

        return stationsNetwork;
    }

    public boolean isWifiConnected(){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    //TODO Refactor : use retrofit httpclient library
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
                Log.d("BixiAPI", "Error saving network", e );
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
