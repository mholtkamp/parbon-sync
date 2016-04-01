package com.teamparbon.parbonsync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TrackSettingsActivity extends AppCompatActivity
{
    private TrackSettings mSettings;

    private CheckBox mCBKills;
    private CheckBox mCBDeaths;
    private CheckBox mCBPentakills;
    private CheckBox mCBQuadrakills;
    private CheckBox mCBTier;
    private CheckBox mCBDivision;
    private CheckBox mCBIngame;
    private EditText mEditAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_settings);

        mSettings = SummonerActivity.readSettingsFile(this);

        if (mSettings == null)
        {
            mSettings = new TrackSettings();
        }

        initializeViews();
    }

    private void initializeViews()
    {
        mCBKills       = (CheckBox) findViewById(R.id.check_kills);
        mCBDeaths      = (CheckBox) findViewById(R.id.check_deaths);
        mCBPentakills  = (CheckBox) findViewById(R.id.check_pentakills);
        mCBQuadrakills = (CheckBox) findViewById(R.id.check_quadrakills);
        mCBTier        = (CheckBox) findViewById(R.id.check_tier);
        mCBDivision    = (CheckBox) findViewById(R.id.check_division);
        mCBIngame      = (CheckBox) findViewById(R.id.check_ingame);
        mEditAPI       = (EditText) findViewById(R.id.edit_api);

        mCBKills.setChecked(mSettings.mTrackHighKills);
        mCBDeaths.setChecked(mSettings.mTrackNoDeaths);
        mCBPentakills.setChecked(mSettings.mTrackPentakills);
        mCBQuadrakills.setChecked(mSettings.mTrackQuadrakills);
        mCBTier.setChecked(mSettings.mTrackTierChanges);
        mCBDivision.setChecked(mSettings.mTrackDivisionChanges);
        mCBIngame.setChecked(mSettings.mTrackInGame);
        mEditAPI.setText(mSettings.mKey);

        Button buttonSave = (Button) findViewById(R.id.button_save);
        buttonSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TrackSettingsActivity.this.updateSettingsFromViews();
                SummonerActivity.writeSettingsFile(TrackSettingsActivity.this, TrackSettingsActivity.this.mSettings);
            }
        });

    }

    public void updateSettingsFromViews()
    {
        mSettings.mTrackHighKills = mCBKills.isChecked();
        mSettings.mTrackNoDeaths = mCBDeaths.isChecked();
        mSettings.mTrackPentakills = mCBPentakills.isChecked();
        mSettings.mTrackQuadrakills = mCBQuadrakills.isChecked();
        mSettings.mTrackTierChanges = mCBTier.isChecked();
        mSettings.mTrackDivisionChanges = mCBDivision.isChecked();
        mSettings.mTrackInGame = mCBIngame.isChecked();

        mSettings.mKey = mEditAPI.getText().toString();
    }
}
