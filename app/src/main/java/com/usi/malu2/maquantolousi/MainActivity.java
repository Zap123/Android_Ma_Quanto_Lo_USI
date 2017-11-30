package com.usi.malu2.maquantolousi;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.charts.HorizontalBarChart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    SharedPreferences sharedPref;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPref = getSharedPreferences("USI",MODE_PRIVATE);

        drawCharts("Daily");
        requestAccessSettings();
    }

    /**
     * PACKAGE_USAGE_STATS requires a system-level permission
     * Settings > Security > Apps with usage access
     */
    public void requestAccessSettings(){
        boolean granted = false;
        Context context = this.getApplicationContext();
        AppOpsManager appOps = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        //ask User to allow the app
        if(!granted){
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    public void drawCharts(String Interval){
        final PackageManager pm = getPackageManager();

        //Calendar cal = Calendar.getInstance();
        //cal.add(Calendar.DAY_OF_WEEK, -1);
        UsageStatsManager usageStatsManager;

        //Map package_name, usage time
        Map<String, UsageStats> stats = null;
        usageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        //List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0,System.currentTimeMillis());

        //get today time at midnight
        if(Interval.equals("Daily")) {
            // today
            Calendar date = new GregorianCalendar();
            // reset hour, minutes, seconds and millis
            date.set(Calendar.HOUR_OF_DAY, 0);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);

            stats = usageStatsManager.queryAndAggregateUsageStats(date.getTimeInMillis(), System.currentTimeMillis());
        }

        ArrayList<String> names = new ArrayList<>();
        ArrayList<BarEntry> values = new ArrayList<>();

        if(stats !=null) {
            float bucket = 0f;
            for (Map.Entry<String, android.app.usage.UsageStats> entry : stats.entrySet()) {
                System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
                String name = null;
                try {
                    name = (String) pm.getApplicationLabel(pm.getApplicationInfo(entry.getKey(), PackageManager.GET_META_DATA));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (sharedPref.getBoolean(name, false)){
                    names.add(name);
                    //add and convert to minutes
                    values.add(new BarEntry(bucket,(float) entry.getValue().getTotalTimeInForeground()  / (1000 * 60)));
                    bucket++;
                }
            }

        }

        /*

        //ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        int count =0;
        for (UsageStats usage:queryUsageStats){
            try {
                String name = (String) pm.getApplicationLabel(pm.getApplicationInfo(usage.getPackageName(), pm.GET_META_DATA));
                System.out.println(name);
                System.out.println(sharedPref.getBoolean(name, false));
                if( sharedPref.getBoolean(name, false)){
                    names.add(name);

                    values.add(new BarEntry((float)usage.getTotalTimeInForeground(),count));
                    System.out.println(usage.getTotalTimeInForeground());
                    count++;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            };
        };
        */

        BarChart barChart = findViewById(R.id.barchart);
        BarDataSet barDataSet = new BarDataSet(values, "Apps");
        BarData data = new BarData(barDataSet);

        //change style
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        //dataSets.add(barDataSet);
        //System.out.println(dataSets);
        //BarDataSet barDataSet = new BarDataSet(values, "Apps");
        //BarData barData = new BarData(data);
        System.out.println(data);
        System.out.println(names);
        //set labels for x axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            ArrayList<String> labels;
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return labels.get((int) value);
            }

            private IAxisValueFormatter init(ArrayList<String> var){
                labels = var;
                return this;
            }
        }.init(names));

        barChart.setData(data);
        barChart.setTouchEnabled(true);
        barChart.setFitBars(true); // make the x-axis fit exactly all bars
        barChart.invalidate(); // refresh
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void goToSettings(MenuItem item) {

        Intent intent;
        intent = new Intent(this, AppListActivity.class);
        startActivity(intent);
    }

    public void goToBlock(MenuItem item){
        Intent intent;
        intent = new Intent(this, BlockListActivity.class);
        startActivity(intent);
    }
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//            TextView textView = rootView.findViewById(R.id.section_label);
//            textView.setText(getString(R.string.section_format, "Daily"));
            // TODO: 11/19/17 ADD THINGS HERE
            return rootView;
//            return null;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
