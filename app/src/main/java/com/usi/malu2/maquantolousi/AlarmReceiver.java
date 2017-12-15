package com.usi.malu2.maquantolousi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by lucacosta on 15.12.17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("MaQuantoLoUSI","ALARMMMM");
        Intent i = new Intent(context, BackgroundService.class);
        context.startService(i);
    }
}
