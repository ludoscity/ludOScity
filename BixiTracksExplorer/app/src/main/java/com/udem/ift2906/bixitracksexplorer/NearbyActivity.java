package com.udem.ift2906.bixitracksexplorer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;


public class NearbyActivity extends ActionBarActivity {
    private GoogleMap nearbyMap;
    private List<BixiStation> testListStation= new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        nearbyMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapNearby)).getMap();

        //Quelques stations de tests...
        testListStation.add(new BixiStation("Old Orchard/Sherbrooke", new LatLng(45.471765,-73.613843)));
        testListStation.add(new BixiStation("Bossuet/Pierre de coubertain", new LatLng(45.573906,-73.539659)));

        setUpMarkers();
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

    private void setUpMarkers(){
        for (BixiStation station: testListStation){
            nearbyMap.addMarker(station.getMarkerOptions());
        }
    }
}
