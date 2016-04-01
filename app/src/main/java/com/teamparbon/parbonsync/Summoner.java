package com.teamparbon.parbonsync;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Martin on 3/24/2016.
 */
public class Summoner
{
    public String mName;
    public long mID;
    public long mCurrentGameID;
    public long mLastGameID;

    public String mLeagueTier;
    public String mLeagueDivision;
    public long mLastUpdate;
    public long mLastInGame;


    public Summoner()
    {
        mName           = "";
        mID             = 0;
        mCurrentGameID  = 0;
        mLastGameID     = 0;
        mLeagueTier     = "";
        mLeagueDivision = "";
        mLastUpdate     = 0;
        mLastInGame     = 0;
    }

    public Summoner(JSONObject jsonObj) throws JSONException
    {
        mName           = jsonObj.getString("name");
        mID             = jsonObj.getLong("id");
        mCurrentGameID  = jsonObj.getLong("currentgameid");
        mLastGameID     = jsonObj.getLong("lastgameid");
        mLeagueTier     = jsonObj.getString("leaguetier");
        mLeagueDivision = jsonObj.getString("leaguedivision");
        mLastUpdate     = jsonObj.getLong("lastupdate");
        mLastInGame     = jsonObj.getLong("lastingame");
    }

    public JSONObject toJSON()
    {
        JSONObject obj = new JSONObject();

        try
        {
            obj.put("name", mName);
            obj.put("id", mID);
            obj.put("currentgameid", mCurrentGameID);
            obj.put("lastgameid", mLastGameID);
            obj.put("leaguetier", mLeagueTier);
            obj.put("leaguedivision", mLeagueDivision);
            obj.put("lastupdate", mLastUpdate);
            obj.put("lastingame", mLastInGame);
        }
        catch (Exception ex)
        {
            Log.e("ParbonSync", ex.toString());
        }

        return obj;
    }
}
