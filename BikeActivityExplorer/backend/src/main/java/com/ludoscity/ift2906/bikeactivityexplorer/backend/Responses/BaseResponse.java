package com.ludoscity.ift2906.bikeactivityexplorer.backend.Responses;

import org.json.JSONObject;

/**
 * Created by F8Full on 2015-03-14.
 * abstract base classed used to encapsulate a response metadata in an org.json JSONObject
 */
@SuppressWarnings("unused") //getMeta() Required for GAE JSON manipulation
public abstract class BaseResponse {

    //Let's go public
    //1- The life span of a result (short) : it's created on the server, quickly serialized and sent
    //2- Contains data about the processing of the request, never used as input to backend code
    public JSONObject meta;

    BaseResponse(){ meta = new JSONObject();}

    //This is so some internal jackson repackaged GAE classes don't get confused as to
    //how to serialize a org.json JSONObject type
    public String getMeta() { return meta.toString();}
}
