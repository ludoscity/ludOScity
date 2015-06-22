package com.ludoscity.ift2906.bikeactivityexplorer.backend.Utils;

import org.json.JSONObject;

/**
 * Created by F8Full on 2015-03-15.
 * Class with static utilities
 */
public class Utils {
    /**
     * Created by F8Full on 2015-03-15.
     * Used to manipulate request result metadata and avoid repetitive code
     */
    public static class ResultMeta {

        public static void addLicense(JSONObject _targetMeta)
        {
            _targetMeta.put("license", "https://creativecommons.org/licenses/by/4.0/");
            _targetMeta.put("license_creator_handle", "@F8Full");
        }
    }
}
