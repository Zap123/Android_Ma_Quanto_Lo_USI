package com.usi.malu2.maquantolousi;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by snipierzz on 11/30/17.
 */

public class BlockListActivity extends AppCompatActivity {
    //Variables for list view
    ArrayList<Drawable> images;
    ArrayList<String> names;
    ArrayList<Boolean> checks;
    ArrayList<Integer> minutes;
    SharedPreferences sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_block);
        fetchInstalled();

        //Select activity and create it with custom adapter.
        ListView listView = findViewById(R.id.list_item_block );
        BlockListActivity.CustomAdapter customAdapter = new BlockListActivity.CustomAdapter();
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
        minutes = new ArrayList<Integer>();

        //Get package manager for the list of installed apps
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
                        names.add(name);
                        images.add(pm.getApplicationIcon(app.packageName));
                        Boolean block = sharedPref.getBoolean(name+"block", false);
                        Integer minutesSP = sharedPref.getInt (name+"blockminutes", 0);
                        minutes.add(minutesSP);
                        checks.add(block);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
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
            view = getLayoutInflater().inflate(R.layout.element_list_block, null);
            ImageView imageView = view.findViewById(R.id.imageViewBlock);
            TextView textView = view.findViewById(R.id.textViewBlock);
            Switch switchButton = view.findViewById(R.id.switch1);


//            // query db to see if it is tracked to display correct label
            switchButton.setChecked(checks.get(i));
//            toggleButton.setChecked(checks.get(i));
            EditText editText = view.findViewById(R.id.editText2);

            Integer mins = minutes.get(i);
            if (mins < 60){
                if (mins==0){
                    editText.setText("");
                }else if (mins<10){
                    editText.setText("0:0"+mins.toString());
                }else{
                editText.setText("0:"+mins.toString());
            }

            }else{
                Integer h;
                h = mins / 60;
                mins = mins % 60;
                if (mins<10){
                    editText.setText(h.toString()+":0"+mins.toString());
                }else{
                    editText.setText(h.toString()+":"+mins.toString());
                }
            }
            editText.addTextChangedListener(new BlockListActivity.TextChanged(names.get(i),i));
//            //set custom on click listener
            switchButton.setOnClickListener(new BlockListActivity.SwitchClickListener(names.get(i),i));
//
//            //set image and text
            imageView.setImageDrawable(images.get(i));
            textView.setText(names.get(i));
            return view;
        }
    }

    /**
     * Custom click listener, gets name of the App in constructor
     * and set the db state to be the opposite of the current state
     */
    public class SwitchClickListener implements View.OnClickListener {

        String appName;
        Integer i;
        /**
         * @param name Name of the App
         */
        public SwitchClickListener(String name, Integer i) {
            this.appName = name;
            this.i = i;
        }

        @Override
        public void onClick(View view) {
//            //get from db current state (if not present default to false)
            Boolean track = sharedPref.getBoolean(appName+"block", false);

//            //set db state to be the opposite of previous state
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(appName+"block", !track);
            checks.add(i,!track);
            editor.commit();
        }
    }

    public class TextChanged implements TextWatcher {

        String appName;
        Integer i;
        /**
         * @param name Name of the App
         */
        public TextChanged(String name, Integer i) {
            this.appName = name;
            this.i = i;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String string = editable.toString();
            System.out.println(string);
            DateFormat formatter;
            if (string.contains(":")){
                if( string.charAt(string.length() - 1)!=':'){
                    formatter = new SimpleDateFormat("hh:mm");
                }else{
                    formatter = new SimpleDateFormat("hh:");
                }
            }else{
                formatter = new SimpleDateFormat("mm");
            }

            Date date = null;
            if (string.length()>0){
                try {
                    date = formatter.parse(string);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Integer min = date.getMinutes();
                Integer hours = date.getHours();
                if ( min == null) {
                    min = 0;
                }
                if (hours == null){
                    hours = 0;
                }
//            //set db state to be the opposite of previous state
                min = min+(hours*60);
                minutes.add(i,min);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(appName+"blockminutes", min);
                editor.commit();
            }else{
                minutes.add(i,0);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(appName+"blockminutes", 0);
                editor.commit();
            }



        }
    }
}

