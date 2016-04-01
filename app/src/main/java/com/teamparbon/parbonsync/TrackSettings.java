package com.teamparbon.parbonsync;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Martin on 3/31/2016.
 */
public class TrackSettings
{
    public boolean mTrackHighKills;
    public boolean mTrackNoDeaths;
    public boolean mTrackPentakills;
    public boolean mTrackQuadrakills;
    public boolean mTrackInGame;
    public boolean mTrackTierChanges;
    public boolean mTrackDivisionChanges;
    public String mKey;

    public TrackSettings()
    {
        mTrackHighKills       = true;
        mTrackNoDeaths        = true;
        mTrackPentakills      = true;
        mTrackQuadrakills     = true;
        mTrackInGame          = true;
        mTrackTierChanges     = true;
        mTrackDivisionChanges = true;
        mKey = "";
    }

    public TrackSettings(JSONObject jsonObject) throws JSONException
    {
        mTrackHighKills = jsonObject.getBoolean("trackHighKills");
        mTrackNoDeaths  = jsonObject.getBoolean("trackNoDeaths");
        mTrackPentakills = jsonObject.getBoolean("trackPentakills");
        mTrackQuadrakills = jsonObject.getBoolean("trackQuadrakills");
        mTrackInGame = jsonObject.getBoolean("trackInGame");
        mTrackTierChanges = jsonObject.getBoolean("trackTierChanges");
        mTrackDivisionChanges = jsonObject.getBoolean("trackDivisionChanges");
        mKey = jsonObject.getString("apiKey");
    }

    public JSONObject toJSON()
    {
        JSONObject obj = new JSONObject();

        try
        {
            obj.put("trackHighKills", mTrackHighKills);
            obj.put("trackNoDeaths", mTrackNoDeaths);
            obj.put("trackPentakills", mTrackPentakills);
            obj.put("trackQuadrakills", mTrackQuadrakills);
            obj.put("trackInGame", mTrackInGame);
            obj.put("trackTierChanges", mTrackTierChanges);
            obj.put("trackDivisionChanges", mTrackDivisionChanges);
            obj.put("apiKey", mKey);
        }
        catch (Exception ex)
        {
            Log.e("ParbonSync", ex.toString());
        }

        return obj;
    }
}
