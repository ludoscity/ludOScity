package com.udem.ift2906.bixitracksexplorer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiAPI;


public class NearbyActivity extends ActionBarActivity {
    private GoogleMap nearbyMap;
    private BixiAPI bixiApiInstance;

    private Context mContext = this;
    private String testText;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        nearbyMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapNearby)).getMap();
        textView = (TextView) findViewById(R.id.textView);

        //TODO string
        Toast.makeText(this, "Trying download...", Toast.LENGTH_SHORT).show();
        new DownloadWebTask().execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_nearby, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class DownloadWebTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            bixiApiInstance= new BixiAPI(mContext);
            testText = bixiApiInstance.getJSonDataFromSharedPref();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            textView.setText(testText);
            //TODO R.string
            Toast.makeText(mContext, "Download Successful!", Toast.LENGTH_SHORT).show();

            bixiApiInstance.getBixiNetwork().network.setUpMarkers();
            bixiApiInstance.getBixiNetwork().network.addMarkersToMap(nearbyMap);
        }
    }

    public Context getContext(){return mContext;}
}
