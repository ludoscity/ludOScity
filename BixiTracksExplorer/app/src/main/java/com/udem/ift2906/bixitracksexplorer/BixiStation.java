package com.udem.ift2906.bixitracksexplorer;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Gevrai on 15-03-26.
 *
 * Classe d'informations sur une stations, couramment n'ayant que les attributs utiles pour les tests.
 *
 */
public class BixiStation {
    private String name;
    private LatLng position;
    private MarkerOptions markerOptions;

    public BixiStation(String _name, LatLng _position){
        name = _name;
        position = _position;
        markerOptions = new MarkerOptions()
                .position(position)
                .title(name);
    }

    public MarkerOptions getMarkerOptions(){return markerOptions;}


}
