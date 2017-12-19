package com.usi.malu2.maquantolousi;

import android.app.IntentService;
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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public class BackgroundService extends IntentService {
    SharedPreferences sharedPref;
    UsageStatsManager usageStatsManager;

    public BackgroundService() {
        super("MaQuantoLoService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.v("MaQuantoLoUSI","EXECUTE COMMANDDD");
        //put code to execute here
        final PackageManager pm = getPackageManager();
        Map<String, UsageStats> stats = null;
        sharedPref = getSharedPreferences("USI",MODE_PRIVATE);
        Calendar c = Calendar.getInstance();
        int hours = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);

        if(hours*3600 + minutes*60 + seconds < 1800){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("challenge",10000);
            editor.putInt("reward",10);
            editor.commit();
        }
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
                    if (minutesSet!=0){
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

        }
        Toast.makeText(getApplicationContext(),"service",Toast.LENGTH_LONG);
    }
}
