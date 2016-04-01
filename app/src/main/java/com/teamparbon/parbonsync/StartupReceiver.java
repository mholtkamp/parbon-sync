package com.teamparbon.parbonsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver
{
    public StartupReceiver()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.w("ParbonSync", "Received broadcasted intent: " + intent.getAction());
        StatisticsService.setServiceAlarm(context);
    }
}
