package com.teamparbon.parbonsync;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StatisticsService extends IntentService {

    public static final String ACTION_UPDATE_STATISTICS = "com.teamparbon.parbonsync.action.UPDATE_STATISTICS";
    private static final int UPDATE_INTERVAL = 1000 * 30; // 30 seconds
    private static final int SUMMONER_UPDATE_DELAY = 1000 * 60 * 10; //10 minutes
    private static final int RECENT_GAMES_UPDATE_TIME = 1000 * 60 * 60; // 60 minutes


    // API Requests
    private static final String API_SUMMONER = "https://na.api.pvp.net/api/lol/na/v1.4/summoner/by-name/";
    private static final String API_CURRENT_GAME = "https://na.api.pvp.net/observer-mode/rest/consumer/getSpectatorGameInfo/NA1/";
    private static final String API_RECENT_GAMES = "https://na.api.pvp.net/api/lol/na/v1.3/game/by-summoner/";
    private static final String API_LEAGUE = "https://na.api.pvp.net/api/lol/na/v2.5/league/by-summoner/";
    private static final String API_PREFIX = "?api_key=";

    // Division constants
    private static final int DIVISION_1 = 1;
    private static final int DIVISION_2 = 2;
    private static final int DIVISION_3 = 3;
    private static final int DIVISION_4 = 4;
    private static final int DIVISION_5 = 5;

    // Tier constants
    private static final int TIER_BRONZE     = 7;
    private static final int TIER_SILVER     = 6;
    private static final int TIER_GOLD       = 5;
    private static final int TIER_PLATINUM   = 4;
    private static final int TIER_DIAMOND    = 3;
    private static final int TIER_MASTER     = 2;
    private static final int TIER_CHALLENGER = 1;

    private static int notificationID = 0;

    public Summoner[] mSummoners;

    private TrackSettings mSettings;


    public StatisticsService()
    {
        super("StatisticsService");
    }

    /**
     * Starts the statistics collection and notification process
     */
    public static void startActionUpdateStatistics(Context context)
    {
        Intent intent = new Intent(context, StatisticsService.class);
        intent.setAction(ACTION_UPDATE_STATISTICS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null)
        {
            final String action = intent.getAction();
            if (ACTION_UPDATE_STATISTICS.equals(action))
            {
                updateStatistics();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void updateStatistics()
    {
        Log.d("ParbonSync", "Updating summoner statistics.");

        mSummoners = new Summoner[SummonerActivity.MAX_SUMMONERS];
        SummonerActivity.readSummonersFile(this, mSummoners);
        mSettings  = SummonerActivity.readSettingsFile(this);
        if (mSettings == null)
        {
            mSettings = new TrackSettings();
        }

        if (!mSettings.mKey.equals(""))
        {
            for (int i = 0; i < SummonerActivity.MAX_SUMMONERS; i++)
            {
                if (mSummoners[i] != null)
                {
                    if (mSummoners[i].mLastUpdate <= System.currentTimeMillis() - SUMMONER_UPDATE_DELAY)
                    {
                        // If the summoner ID is missing, get it because it
                        // is required for all other API calls.
                        if (mSummoners[i].mID == 0)
                        {
                            querySummoner(mSummoners[i]);
                        }

                        // Always query current game to optimize calls
                        // to get recent game data.
                        queryCurrentGame(mSummoners[i]);

                        // Check stats from latest games
                        if (mSettings.mTrackNoDeaths ||
                                mSettings.mTrackHighKills ||
                                mSettings.mTrackPentakills ||
                                mSettings.mTrackQuadrakills)
                        {

                            // Only update if a game was recently played to
                            // reduce data usage
                            if (System.currentTimeMillis() - mSummoners[i].mLastInGame < RECENT_GAMES_UPDATE_TIME)
                            {
                                queryRecentGames(mSummoners[i]);
                            }
                        }

                        // Check league stats for promotions/demotions
                        if (mSettings.mTrackTierChanges ||
                                mSettings.mTrackDivisionChanges)
                        {
                            queryLeague(mSummoners[i]);
                        }

                        // Save the most recent update time
                        mSummoners[i].mLastUpdate = System.currentTimeMillis();

                        // Only update one summoner at a time for now.
                        break;
                    }
                }
            }
        }

        SummonerActivity.writeSummonersFile(this, mSummoners);
    }

    public void querySummoner(Summoner summoner)
    {
        String urlString = API_SUMMONER + summoner.mName.toLowerCase().replace(" ", "") + API_PREFIX + mSettings.mKey;
        String response = sendRequest(urlString);

        if (!response.equals(""))
        {
            try
            {
                JSONObject map = (JSONObject) new JSONTokener(response).nextValue();
                JSONObject summonerObj = map.getJSONObject(summoner.mName.toLowerCase().replace(" ", ""));
                summoner.mID = summonerObj.getLong("id");
                Log.d("ParbonSync", "" + summoner.mID);
            }
            catch (Exception ex)
            {
                Log.e("ParbonSync", ex.toString());
            }
        }
    }

    public void queryRecentGames(Summoner summoner)
    {
        int numPentakills = 0;
        int numQuadrakills = 0;
        int numDeaths = 0;
        int numKills = 0;

        String urlString = API_RECENT_GAMES + summoner.mID + "/recent" + API_PREFIX + mSettings.mKey;
        String response = sendRequest(urlString);

        if (response.equals(""))
        {
            Log.d("ParbonSync", "Error retrieving recent games for " + summoner.mName);
        }
        else
        {
            try
            {
                JSONObject dataObj = (JSONObject) new JSONTokener(response).nextValue();
                JSONArray gamesArray = dataObj.getJSONArray("games");
                JSONObject game1Obj = gamesArray.getJSONObject(0);
                JSONObject stats1Obj = game1Obj.getJSONObject("stats");

                // Test if this game has already been inspected.
                if (summoner.mLastGameID == game1Obj.getLong("gameId"))
                {
                    // Already examined
                    return;
                }
                else
                {
                    // Set this new gameId as the last game Id inspected
                    summoner.mLastGameID = game1Obj.getLong("gameId");
                }

                if (stats1Obj.has("pentaKills"))
                {
                    numPentakills = stats1Obj.getInt("pentaKills");
                }

                if (stats1Obj.has("quadraKills"))
                {
                    numQuadrakills = stats1Obj.getInt("quadraKills");
                }

                if (stats1Obj.has("numDeaths"))
                {
                    numDeaths = stats1Obj.getInt("numDeaths");
                }

                if (stats1Obj.has("championsKilled"))
                {
                    numKills = stats1Obj.getInt("championsKilled");
                }


                // Send Notifications
                if (mSettings.mTrackPentakills &&
                        numPentakills >= 1)
                {
                    pushNotification("" + summoner.mName + " just scored a pentakill!");
                }

                if (mSettings.mTrackQuadrakills &&
                        numQuadrakills >= 1     &&
                        numPentakills  == 0)
                {
                    pushNotification("" + summoner.mName + " just scored a quadrakill!");
                }

                if (mSettings.mTrackNoDeaths &&
                        numDeaths == 0)
                {
                    pushNotification("" + summoner.mName + " finished a game without dying!");
                }

                // Testing over 10 kills or something
                if (mSettings.mTrackHighKills &&
                        numKills >= 10)
                {
                    pushNotification("" + summoner.mName + " just made " + numKills + " kills in one game!");
                }
            }
            catch (Exception ex)
            {
                Log.e("ParbonSync", ex.toString());
            }
        }

    }

    public void queryCurrentGame(Summoner summoner)
    {
        String urlString = API_CURRENT_GAME + summoner.mID + API_PREFIX + mSettings.mKey;
        String response = sendRequest(urlString);

        if (response.equals(""))
        {
            Log.d("ParbonSync", "" + summoner.mName + " is not in game.");
            summoner.mCurrentGameID = 0;
        }
        else
        {
            Log.d("ParbonSync", "" + summoner.mName + " is IN GAME!!");

            // Record last in game time
            summoner.mLastInGame = System.currentTimeMillis();

            if (mSettings.mTrackInGame == false)
            {
                // Latest game time has been recorded, but the user has indicated
                // that he/she does not want notifications about tracked summoners in game.
                return;
            }

            try
            {
                JSONObject gameObj = (JSONObject) new JSONTokener(response).nextValue();
                JSONArray playersArray = (JSONArray) gameObj.getJSONArray("participants");

                // Check to see if the current game has already been recorded/notified.
                if (summoner.mCurrentGameID == gameObj.getLong("gameId"))
                {
                    // Do nothing
                    Log.d("ParbonSync", "Current game already notified.");
                }
                else
                {
                    summoner.mCurrentGameID = gameObj.getLong("gameId");
                    for (int i = 0; i < playersArray.length(); i++)
                    {
                        JSONObject playerObj = playersArray.getJSONObject(i);

                        if (playerObj.getLong("summonerId") == summoner.mID)
                        {
                            //Log.d("ParbonSync", summoner.mName + "is playing as " + playerObj.getInt("championId"));
                            pushNotification(summoner.mName + " is playing LoL now!");
                            break;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Log.e("ParbonSync", "" + ex);
            }
        }
    }

    public void queryLeague(Summoner summoner)
    {
        String urlString = API_LEAGUE + summoner.mID + "/entry" + API_PREFIX + mSettings.mKey;
        String response = sendRequest(urlString);

        String division = "";
        String tier     = "";
        int divisionInt = 0;
        int tierInt     = 0;

        if (response.equals(""))
        {
            Log.d("ParbonSync", "Error getting league info for " + summoner.mName);
        }
        else
        {
            try
            {
                JSONObject mapObj = (JSONObject) new JSONTokener(response).nextValue();
                JSONArray leagueArray = mapObj.getJSONArray("" + summoner.mID);

                for (int i = 0; i < leagueArray.length(); i++)
                {
                    JSONObject leagueObj = leagueArray.getJSONObject(i);

                    // Only update solo queue ranking
                    if (leagueObj.getString("queue").equals("RANKED_SOLO_5x5"))
                    {
                        tier = leagueObj.getString("tier");
                        tierInt = tierToInt(leagueObj.getString("tier"));

                        JSONArray entriesArray = leagueObj.getJSONArray("entries");
                        JSONObject entryObj = entriesArray.getJSONObject(0);

                        division = entryObj.getString("division");
                        divisionInt = divisionToInt(entryObj.getString("division"));

                        break;
                    }
                }

                if (mSettings.mTrackTierChanges)
                {
                    if (tierInt != 0 &&
                            tierInt < tierToInt(summoner.mLeagueTier))
                    {
                        pushNotification("" + summoner.mName + " has been promoted to " + tier);
                        summoner.mLeagueTier = tier;
                        summoner.mLeagueDivision = division;

                        // Return, no need to notify for division change
                        return;
                    }
                    else if (tierInt != 0 &&
                            tierInt > tierToInt(summoner.mLeagueTier))
                    {
                        pushNotification("" + summoner.mName + " has been demoted to " + tier);
                        summoner.mLeagueTier = tier;
                        summoner.mLeagueDivision = division;

                        // Return now. Do not check division change.
                        return;
                    }
                }

                if (mSettings.mTrackDivisionChanges)
                {
                    if (divisionInt != 0 &&
                            divisionInt < divisionToInt(summoner.mLeagueDivision))
                    {
                        pushNotification("" + summoner.mName + " has been promoted to " + summoner.mLeagueTier + " " + division);
                        summoner.mLeagueDivision = division;
                    }
                    else if (divisionInt != 0 &&
                             divisionInt > divisionToInt(summoner.mLeagueDivision))
                    {
                        pushNotification("" + summoner.mName + " has been demoted to " + summoner.mLeagueTier + " " + division);
                        summoner.mLeagueDivision = division;
                    }
                }
            }
            catch (Exception ex)
            {
                Log.e("ParbonSync", "" + ex);
            }
        }
    }

    public void pushNotification(String msg)
    {
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Parbon Sync")
                .setContentText(msg)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, notification);
        notificationID++;
    }

    public String sendRequest(String urlString)
    {
        String response = "";
        int responseCode = 0;

        // TEST: Get summoner ID from name of summoner.
        try
        {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            try
            {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                responseCode = urlConnection.getResponseCode();
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null)
                {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                response = stringBuilder.toString();

                // if response code isn't 200, set response to null string.
                if (responseCode != 200)
                {
                    response = "";
                }
            }
            catch (Exception ex)
            {
                //Log.e("ParbonSync", ex.toString());
                Log.e("ParbonSync", "Response Code Error: " + responseCode);
                Log.e("ParbonSync", response);
                response = "";
            }
            finally
            {
                urlConnection.disconnect();
            }
        }
        catch(Exception ex)
        {
            Log.e("ParbonSync", ex.toString());
        }

        return response;
    }

    public static void setServiceAlarm(Context context)
    {
        Intent intent = new Intent(context, StatisticsService.class);
        intent.setAction(ACTION_UPDATE_STATISTICS);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + UPDATE_INTERVAL, UPDATE_INTERVAL, pendingIntent);
    }

    public int divisionToInt(String division)
    {
        if (division.equals("I"))
            return DIVISION_1;
        if (division.equals("II"))
            return DIVISION_2;
        if (division.equals("III"))
            return DIVISION_3;
        if (division.equals("IV"))
            return DIVISION_4;
        if (division.equals("V"))
            return DIVISION_5;

        return 0;
    }

    public int tierToInt(String tier)
    {
        if (tier.equals("BRONZE"))
            return TIER_BRONZE;
        if (tier.equals("SILVER"))
            return TIER_SILVER;
        if (tier.equals("GOLD"))
            return TIER_GOLD;
        if (tier.equals("PLATINUM"))
            return TIER_PLATINUM;
        if (tier.equals("DIAMOND"))
            return TIER_DIAMOND;
        if (tier.equals("MASTER"))
            return TIER_MASTER;
        if (tier.equals("CHALLENGER"))
            return TIER_CHALLENGER;

        return 0;
    }
}
