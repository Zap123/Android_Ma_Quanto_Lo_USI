package com.usi.malu2.maquantolousi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by snipierzz on 11/19/17.
 */

public class AppListActivity extends AppCompatActivity {
    //Variables for list view
    ArrayList<Drawable> images;
    ArrayList<String> names;
    ArrayList<Boolean> checks;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applist);
        fetchInstalled();

        //Select activity and create it with custom adapter.
        ListView listView = findViewById(R.id.list_item);
        CustomAdapter customAdapter = new CustomAdapter();
        listView.setAdapter(customAdapter);
    }

    /**
     * Fetch all the installed apps and save them into images, names, checks arrays.
     */
    public void fetchInstalled(){

        //Initialize sharedPreferences
        sharedPref = getSharedPreferences("USI",MODE_PRIVATE);

        //Initialize ArrayLists
        images = new ArrayList<Drawable>();
        names = new ArrayList<String>();
        checks = new ArrayList<Boolean>();

        //Get package manager for the list of installed apps
        final PackageManager pm = getPackageManager();

        //local variable
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // For each app extract name image and query db to see if the user set the app to be tracked
        for (ApplicationInfo app : apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null && !pm.getLaunchIntentForPackage(app.packageName).equals("")) {
                try {
                    String name = (String) pm.getApplicationLabel(pm.getApplicationInfo(app.packageName, pm.GET_META_DATA));
                    names.add(name);
                    images.add(pm.getApplicationIcon(app.packageName));
                    // search db for tracked info, if not found returns not tracked.
                    Boolean track = sharedPref.getBoolean(name, false);
                    checks.add(track);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Function to go to the mainActivity (save feature implemented on the change of state of buttons)
     * @param v View
     */
    public void saveAndGoHome(View v) {
        Intent intent;
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * CustomAdapter class to customize list view.
     */

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            //System.out.println("size:" + images.size());
            return images.size();
        }

        @Override
        public Object getItem(int i) {
            return names.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            //get view items: image, text and button
            view = getLayoutInflater().inflate(R.layout.customlayout, null);
            ImageView imageView = view.findViewById(R.id.imageView);
            TextView textView = view.findViewById(R.id.textView_name);
            ToggleButton toggleButton = view.findViewById(R.id.toggleButton);

            // query db to see if it is tracked to display correct label
            toggleButton.setChecked(checks.get(i));

            //set custom on click listener
            toggleButton.setOnClickListener(new myClickListener(names.get(i)));

            //set image and text
            imageView.setImageDrawable(images.get(i));
            textView.setText(names.get(i));
            return view;
        }
    }

    /**
     * Custom click listener, gets name of the App in constructor
     * and set the db state to be the opposite of the current state
     */
    public class myClickListener implements View.OnClickListener {

        String appName;

        /**
         * @param name Name of the App
         */
        public myClickListener(String name) {
            this.appName = name;
        }

        @Override
        public void onClick(View view) {
            //get from db current state (if not present default to false)
            Boolean track = sharedPref.getBoolean(appName, false);

            //set db state to be the opposite of previous state
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(appName, !track);
            editor.commit();
        }
    }
}
