package com.usi.malu2.maquantolousi;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Google Fit - Exercises to extend limits
 *
 * @author Luca Costa
 *         Based on this boilerplate https://github.com/googlesamples/android-fit/tree/master/BasicHistoryApi
 */


public class ExerciseActivity extends AppCompatActivity {

    public static final String TAG = "MaQuantoLoUSI";
    // Identifier to identify the sign in activity.
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    public int challengeSteps;
    public int reward;
    public int done = 0;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
        sharedPref = getSharedPreferences("USI",MODE_PRIVATE);
        challengeSteps = sharedPref.getInt("challenge",10000);
        reward = sharedPref.getInt("reward",10);
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.

        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .build();
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            readHistoryData();

        }


        Button button  =  findViewById(R.id.reward );
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("HERE!!!");
                if (done >= challengeSteps){
                    System.out.println("AAAA!!!");
                    Toast.makeText(view.getContext(),"You made it!",Toast.LENGTH_LONG);
                    final PackageManager pm = getPackageManager();

                    //local variable
                    List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

                    // For each app extract name image and query db to see if the user set the app to be tracked
                    for (ApplicationInfo app : apps) {
                        if (pm.getLaunchIntentForPackage(app.packageName) != null && !pm.getLaunchIntentForPackage(app.packageName).equals("")) {
                            try {
                                String name = (String) pm.getApplicationLabel(pm.getApplicationInfo(app.packageName, pm.GET_META_DATA));
                                // search db for tracked info, if not found returns not tracked.
                                Boolean track = sharedPref.getBoolean(name, false);
                                if (track){ //if tracked, push to array infos
                                    Boolean block = sharedPref.getBoolean(name+"block", false);
                                    if (block){
                                        Integer minutes = sharedPref.getInt (name+"blockminutes", 0);
                                        if (minutes>0){
                                            System.out.println(name+ minutes);
                                            System.out.println(minutes+reward);
                                            SharedPreferences.Editor editor = sharedPref.edit();
                                            editor.putInt(name+"blockminutes", minutes+reward);
                                            editor.commit();
                                        }
                                    }
                                    Integer minutesSP = sharedPref.getInt (name+"blockminutes", 0);

                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("challenge",challengeSteps*2);
                    editor.putInt("reward",reward*2);
                    editor.commit();
                    reward = reward*2;
                    challengeSteps = challengeSteps*2;
                }else{
                    int todo = challengeSteps-done;
                    System.out.println("You have to perform "+todo+" more steps");
                    Toast.makeText(view.getContext(),"You have to perform "+todo+" more steps",Toast.LENGTH_LONG);
                }
            }
        });

        TextView tv = findViewById(R.id.title);
        tv.setText("To beat this challenge and unlock "+reward+" minutes extra, walk "+challengeSteps+" steps before midnight!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                readHistoryData();
            }
        }
    }


    /**
     * Asynchronous task to read the history data. When the task succeeds, it will print out the data.
     */
    private Task<DataReadResponse> readHistoryData() {
        // Begin by creating the query.
        DataReadRequest readRequest = queryFitnessData();

        // Invoke the History API to fetch the data with the query
        return Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                // For the sake of the sample, we'll print the data so we can see what we just
                                // added. In general, logging fitness information should be avoided for privacy
                                // reasons.
                                printData(dataReadResponse);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "There was a problem reading the data.", e);
                            }
                        });
    }


    /** Returns a {@link DataReadRequest} for all step count changes in the current day. */
    public static DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest =
                new DataReadRequest.Builder()
                        // The data request can specify multiple data types to return, effectively
                        // combining multiple data queries into one call.
                        // In this example, it's very unlikely that the request is for several hundred
                        // datapoints each consisting of a few steps and a timestamp.  The more likely
                        // scenario is wanting to see how many steps were walked per day, for 7 days.
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
        // [END build_read_data_request]

        return readRequest;
    }

    /**
     * Logs a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would dump
     * all the data. In this sample, logging also prints to the device screen, so we can see what the
     * query returns, but your app should not log fitness information as a privacy consideration. A
     * better option would be to dump the data you receive to a local data directory to avoid exposing
     * it to other applications.
     */
    public void printData(DataReadResponse dataReadResult) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(
                    TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));

                //update steps in activity
                TextView steps = findViewById(R.id.steps);
                steps.setText(dp.getValue(field).toString() + "/"+challengeSteps+" steps");
                ProgressBar p_bar = findViewById(R.id.stepsProgress);
                p_bar.setMax(challengeSteps);
                done = Integer.parseInt(dp.getValue(field).toString());
                p_bar.setProgress(done);
            }
        }
    }

}