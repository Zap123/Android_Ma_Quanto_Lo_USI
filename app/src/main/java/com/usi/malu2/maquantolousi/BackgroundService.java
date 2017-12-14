package com.usi.malu2.maquantolousi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public class BackgroundService extends Service {
    SharedPreferences sharedPref;
    UsageStatsManager usageStatsManager;
    public BackgroundService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //put code to execute here
        final PackageManager pm = getPackageManager();
        Map<String, UsageStats> stats = null;
        sharedPref = getSharedPreferences("USI",MODE_PRIVATE);
        usageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar date = new GregorianCalendar();
        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        stats = usageStatsManager.queryAndAggregateUsageStats(date.getTimeInMillis(), System.currentTimeMillis());

        if(stats !=null) {
            for (Map.Entry<String, android.app.usage.UsageStats> entry : stats.entrySet()) {
                String name = null;
                try {
                    name = (String) pm.getApplicationLabel(pm.getApplicationInfo(entry.getKey(), PackageManager.GET_META_DATA));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                Boolean track = sharedPref.getBoolean(name, false);
                if (track){ //if tracked, push to array infos
                    // name is app name
                    int minutesSet = sharedPref.getInt (name+"blockminutes", 0);
                    float value = entry.getValue().getTotalTimeInForeground()  / (1000 * 60);
                    if (value > minutesSet){
                        String notify = "you used app:"+name+ " for more than: "+ minutesSet+ " minutes";
                        NotificationCompat.Builder builder =  new NotificationCompat.Builder(getApplicationContext());
                        builder.setSmallIcon(R.mipmap.ic_launcher);
                        builder.setContentTitle("MA QUANTO LO USI?");
                        builder.setContentText(notify);
                        NotificationManager NM = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE    );
                        NM.notify(0, builder.build());
                    }

                }

            }

        }
        onTaskRemoved(intent);
        Toast.makeText(getApplicationContext(),"service",Toast.LENGTH_LONG);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restarted =  new Intent(getApplicationContext(),this.getClass());
        restarted.setPackage(getPackageName());
        startService(restarted);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
