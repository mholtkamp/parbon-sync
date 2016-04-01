package com.teamparbon.parbonsync;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;


public class SummonerActivity extends AppCompatActivity {

    // Constants
    public static final int MAX_SUMMONERS = 10;
    public static final String SUMMONER_FILE_NAME = "summoners.json";
    public static final String SETTINGS_FILE_NAME = "settings.json";

    // Members
    public Button mAddSummonerButton;
    public Button mRemoveSummonerButton;
    public EditText mAddSummonerEditText;
    public LinearLayout mSummonerList;
    public Summoner[] mSummoners;
    public TextView[] mSummonerTexts;
    public int mSelectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summoner);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup listeners
        mAddSummonerButton = (Button) findViewById(R.id.button_add_summoner);

        mAddSummonerButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                boolean success = false;

                // Add the summoner name to SUMMONER_FILE_NAME
                success = SummonerActivity.this.AddSummonerToList(SummonerActivity.this.mAddSummonerEditText.getText().toString());

                if (success) {
                    // Refresh the summoner list
                    refreshSummonerList();

                    Toast.makeText(SummonerActivity.this, "Summoner Added", Toast.LENGTH_SHORT).show();
                    SummonerActivity.this.mAddSummonerEditText.setText("");
                }
                else
                {
                    Toast.makeText(SummonerActivity.this, "Failed to add summoner", Toast.LENGTH_SHORT).show();
                }

            }
        });

        mRemoveSummonerButton = (Button) findViewById(R.id.button_remove_summoner);
        mRemoveSummonerButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                removeSummonerFromList();
            }
        });

        mAddSummonerEditText = (EditText) findViewById(R.id.field_summoner_name);
        mSummonerList = (LinearLayout) findViewById(R.id.title_tracked_summoners);

        // Create arrays to be populated below
        mSummoners = new Summoner[MAX_SUMMONERS];
        mSummonerTexts = new TextView[MAX_SUMMONERS];

        mSelectedIndex = -1;

        SummonerActivity.readSummonersFile(this, mSummoners);

        // Create TextViews for summoner names
        for (int i = 0; i < MAX_SUMMONERS; i++)
        {
            mSummonerTexts[i] = new TextView(this);
            mSummonerTexts[i].setTextSize(16.0f);
            mSummonerTexts[i].setId(5000 + i);
            mSummonerTexts[i].setText("");
            //mSummonerTexts[i].setSaveEnabled(false);
            mSummonerTexts[i].setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    SummonerActivity.this.highlightSummoner(v.getId());
                }
            });

            // Add view to the list
            mSummonerList.addView(mSummonerTexts[i]);
        }

        // Refresh the summoner list
        refreshSummonerList();

        StatisticsService.setServiceAlarm(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_summoner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent i = new Intent(this, TrackSettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean AddSummonerToList(String name)
    {
        FileOutputStream out = null;
        String str = "";

        // Add a new summoner to the mSummoner array
        int i = 0;
        for (i = 0; i < MAX_SUMMONERS; i++)
        {
            if (mSummoners[i] == null)
            {
                // Empty summoner slot found.
                mSummoners[i] = new Summoner();
                mSummoners[i].mName = name;
                break;
            }
        }

        // Check if summoner array was full
        if (i == MAX_SUMMONERS)
        {
            // Yup, return false;
            return false;
        }

        writeSummonersFile(this, mSummoners);
        return true;
    }

    public void refreshSummonerList()
    {
        for (int i = 0; i < MAX_SUMMONERS; i++)
        {
            if (mSummoners[i] != null) {
                mSummonerTexts[i].setText(mSummoners[i].mName);
            }
            else
            {
                mSummonerTexts[i].setText("");
            }
        }
    }

    public void highlightSummoner(int id)
    {
        for (int i = 0; i < MAX_SUMMONERS; i++)
        {
            if (mSummonerTexts[i].getId() == id)
            {
                if (i == mSelectedIndex ||
                    mSummoners[i] == null)
                {
                    // This summoner name is already selected,
                    // So selecting it again will deselect it
                    // or, the summoner slot is just empty
                    mSummonerTexts[i].setBackgroundColor(Color.argb(0, 0, 0, 0));
                    mSummonerTexts[i].setTextColor(Color.rgb(0,0,0));
                    mSelectedIndex = -1;
                    showRemoveButton(false);
                }
                else
                {
                    // It wasn't already selected, so select it
                    mSummonerTexts[i].setBackgroundColor(Color.rgb(10, 30, 140));
                    mSummonerTexts[i].setTextColor(Color.rgb(255, 255, 255));
                    mSelectedIndex = i;
                    showRemoveButton(true);
                }
            }
            else
            {
                mSummonerTexts[i].setBackgroundColor(Color.argb(0, 0, 0, 0));
                mSummonerTexts[i].setTextColor(Color.rgb(0, 0, 0));
            }
        }
    }

    public void showRemoveButton(boolean visible)
    {
        if (visible)
            mRemoveSummonerButton.setVisibility(View.VISIBLE);
        else
            mRemoveSummonerButton.setVisibility(View.INVISIBLE);
    }

    public void removeSummonerFromList()
    {
        if (mSelectedIndex == -1)
            return;

        // Remove summoner data
        mSummoners[mSelectedIndex] = null;

        // Clear the summoner data in mSummoner array
        // First find the first null
        int nullIndex = 0;
        for (int i = 0; i < MAX_SUMMONERS; i++)
        {
            if (mSummoners[i] == null) {
                nullIndex = i;
                break;
            }
        }

        // And now iterate through remaining indices.
        // If a non-null summoner is found, move its position to nullIndex.
        // And then increment nullIndex by 1
        for (int i = nullIndex + 1; i < MAX_SUMMONERS; i++)
        {
            if (mSummoners[i] != null)
            {
                mSummoners[nullIndex] = mSummoners[i];
                mSummoners[i] = null;
                nullIndex++;
            }
        }

        // Output to json file
        writeSummonersFile(this, mSummoners);

        // (un)highlight the selected index
        highlightSummoner(mSummonerTexts[mSelectedIndex].getId());

        // Repopulate the text views in list
        refreshSummonerList();
    }

    public static void readSummonersFile(Context context, Summoner[] summoners)
    {
        BufferedReader reader = null;
        // Open the summoner json file
        try
        {
            InputStream in = context.openFileInput(SUMMONER_FILE_NAME);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonString = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null)
            {
                jsonString.append(line);
            }

            JSONArray jsonArray = (JSONArray) new JSONTokener(jsonString.toString()).nextValue();
            for (int i = 0; i < jsonArray.length(); i++)
            {
                summoners[i] = new Summoner(jsonArray.getJSONObject(i));
            }
        }
        catch (Exception ex)
        {
            Log.e("ParbonSync", ex.toString());
        }
        finally
        {
            if (reader != null)
            {
                try {
                    reader.close();
                }
                catch (Exception ex)
                {

                }
            }
        }
    }

    public static void writeSummonersFile(Context context, Summoner[] summoners)
    {
        FileOutputStream out = null;

        // Now write to the summoners.json file
        try
        {
            out = context.openFileOutput(SUMMONER_FILE_NAME, Context.MODE_PRIVATE);
            JSONArray jsonArray = new JSONArray();

            for (int n = 0; n < MAX_SUMMONERS; n++)
            {
                if (summoners[n] != null)
                {
                    jsonArray.put(summoners[n].toJSON());
                }
            }

            out.write(jsonArray.toString().getBytes());
        }
        catch(Exception ex)
        {
            Log.e("Parbon", "Could not open " + SUMMONER_FILE_NAME +  "or something");
            Log.e("Parbon", ex.toString());
        }
        finally
        {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                    Log.e("ParbonSync", ex.toString());
                }
            }
        }
    }

    public static TrackSettings readSettingsFile(Context context)
    {
        BufferedReader reader = null;
        TrackSettings ret = null;

        // Open the settings json file
        try
        {
            InputStream in = context.openFileInput(SETTINGS_FILE_NAME);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonString = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null)
            {
                jsonString.append(line);
            }

            JSONObject jsonObject = (JSONObject) new JSONTokener(jsonString.toString()).nextValue();
            ret = new TrackSettings(jsonObject);
        }
        catch (Exception ex)
        {
            Log.e("ParbonSync", ex.toString());
        }
        finally
        {
            if (reader != null)
            {
                try {
                    reader.close();
                }
                catch (Exception ex)
                {

                }
            }
        }

        return ret;
    }

    public static void writeSettingsFile(Context context, TrackSettings settings)
    {
        FileOutputStream out = null;

        // Now write to the summoners.json file
        try
        {
            out = context.openFileOutput(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
            out.write(settings.toJSON().toString().getBytes());
            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show();
        }
        catch(Exception ex)
        {
            Log.e("Parbon", "Could not open " + SETTINGS_FILE_NAME +  "or something");
            Log.e("Parbon", ex.toString());
        }
        finally
        {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                    Log.e("ParbonSync", ex.toString());
                }
            }
        }
    }
}
