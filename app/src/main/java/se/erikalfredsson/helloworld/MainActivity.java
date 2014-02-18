package se.erikalfredsson.helloworld;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity implements ActionBar.TabListener, TimerService.TimerCallback, StopwatchService.StopwatchCallback {

    public static final int NUMBER_OF_TABS = 2;
    public static final int TIMER_POSITION = 1;
    public static final int STOPWATCH_POSITION = 0;

    private static final int RESET_BUTTON_DISABLED = 0;
    private static final int RESET_BUTTON_ENABLED = 1;
    private static final String LAST_PAGE_KEY = "lastPage";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private int mSectionNumber;

    private static TimerService mTimerService;
    private TimerServiceConnection mTimerServiceConnection;
    private static PlaceholderFragment mTimerFragment;

    private StopwatchService mStopwatchService;
    private StopwatchServiceConnection mStopwatchServiceConnection;

    private PlaceholderFragment mStopwatchFragment;
    private int mLastPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mStopwatchFragment = PlaceholderFragment.newInstance(0);
        mTimerFragment = PlaceholderFragment.newInstance(1);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        if (savedInstanceState != null) {
            mLastPage = savedInstanceState.getInt(LAST_PAGE_KEY, 0);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mSectionNumber = intent.getIntExtra("sectionNumber", -1);
        Log.d("OnNewIntent", "OnNewIntent" +mSectionNumber);

        int alertDialog = intent.getIntExtra("alertDialog", -1);
        if (alertDialog > 0) {
            Log.d("OnNewIntent", " " + alertDialog);
            DialogFragment df = new AlertDialogFragment();
           df.show(getFragmentManager(), "alarmDialog");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        mTimerServiceConnection = new TimerServiceConnection();
        bindService(new Intent(this, TimerService.class),
                mTimerServiceConnection, BIND_AUTO_CREATE);

        mStopwatchServiceConnection = new StopwatchServiceConnection();
        bindService(new Intent(this, StopwatchService.class),
                mStopwatchServiceConnection, BIND_AUTO_CREATE);


    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mSectionNumber != -1) {
            mViewPager.setCurrentItem(mSectionNumber);
        }else {
            mViewPager.setCurrentItem(mLastPage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mTimerService.setTimerCallback(null);
        unbindService(mTimerServiceConnection);

        mStopwatchService.setStopwatchCallback(null);
        unbindService(mStopwatchServiceConnection);

        mLastPage = mViewPager.getCurrentItem();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mLastPage = mViewPager.getCurrentItem();
        outState.putInt(LAST_PAGE_KEY, mViewPager.getCurrentItem());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLastPage = savedInstanceState.getInt(LAST_PAGE_KEY, 0);
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }


    public void startStopStopwatch(View view) {
        if (mStopwatchService != null) {
            if (!mStopwatchService.isStopwatchIsRunning()) {
                mStopwatchService.startStopwatch();
                mStopwatchFragment.mStopwatchResetButton.setText("Lap");
                mStopwatchFragment.mStopwatchStartStopButton.setText("Stop");
                mStopwatchFragment.mStopwatchStartStopButton.setBackgroundResource(R.drawable.oval_stop_button);
            } else {
                mStopwatchService.stopStopwatch();
                mStopwatchFragment.mStopwatchStartStopButton.setText("Start");
                mStopwatchFragment.mStopwatchStartStopButton.setBackgroundResource(R.drawable.oval_start_button);
                mStopwatchFragment.mStopwatchResetButton.setText("Reset");
            }
        }

    }


    public void resetStopWatchAddLap(View view) {
        if (mStopwatchService.isStopwatchIsRunning()) {
            mStopwatchFragment.addLapToStopwatchList();
            //mStopwatchFragment.updateStopwatchLapValue(0);
            mStopwatchService.mCurrLapTime = mStopwatchService.getStopwatchValue();
        } else {
            mStopwatchService.resetStopwatch();
            mStopwatchFragment.updateStopwatchValue(0);
            mStopwatchFragment.updateStopwatchLapValue(0);
            mStopwatchService.mCurrLapTime = 0;
            mStopwatchFragment.mSavedStopwatchValues.clear();
            mStopwatchFragment.mAdapter.notifyDataSetChanged();
        }

    }


    public void startTimer(View view) {
        if (mTimerService != null && mTimerService.TIMER_START_VALUE != 0) {

            if (!mTimerService.isTimerRunning()) {
                mTimerService.startTimer();
                mTimerFragment.mTimerStartStopButton.setText("Pause");
                mTimerFragment.mTimerStartStopButton.setBackgroundResource(R.drawable.oval_stop_button);
            } else {
                mTimerService.stopTimer();
                mTimerFragment.mTimerStartStopButton.setText("Start");
                mTimerFragment.mTimerStartStopButton.setBackgroundResource(R.drawable.oval_start_button);
            }

            mTimerFragment.mTimerTimePicker.setEnabled(false);
            setResetButtonState(RESET_BUTTON_ENABLED);

        }
        if (mTimerService.TIMER_START_VALUE == 0) {
            setResetButtonState(RESET_BUTTON_DISABLED);
        }
    }

    public static void setResetButtonState(int state) {
        switch (state) {
            case RESET_BUTTON_DISABLED:
                mTimerFragment.mTimerResetButton.setBackgroundResource(R.drawable.oval_disabled_button);
                mTimerFragment.mTimerResetButton.setTextColor(Color.parseColor("#b7b7b7"));
                break;
            case RESET_BUTTON_ENABLED:
                mTimerFragment.mTimerResetButton.setBackgroundResource(R.drawable.oval_reset_lap_button);
                mTimerFragment.mTimerResetButton.setTextColor(Color.parseColor("#000000"));
                break;
        }
    }


    public void onResetTimerClicked(View view) {
        if (mTimerService != null ) {
            mTimerService.stopTimer();
            mTimerFragment.mTimerTimePicker.setEnabled(true);
            mTimerFragment.mTimerStartStopButton.setText("Start");
            mTimerService.resetTimer();
            mTimerFragment.updateTimerValue(0);
            mTimerFragment.mTimerTimePicker.setCurrentHour(0);
            mTimerFragment.mTimerTimePicker.setCurrentMinute(0);
            mTimerFragment.mTimerStartStopButton.setBackgroundResource(R.drawable.oval_start_button);
            setResetButtonState(RESET_BUTTON_DISABLED);
        }
    }


    class TimerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTimerService = ((TimerService.LocalBinder) service).getService();
            mTimerService.setTimerCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTimerService = null;
        }
    }

    class StopwatchServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mStopwatchService = ((StopwatchService.LocalBinder) service).getService();
            mStopwatchService.setStopwatchCallback(MainActivity.this);
            //mTimerFragment.setTimerButtonEnabled(true);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //mTimerFragment.setTimerButtonEnabled(false);
            mStopwatchService = null;
        }
    }

    @Override
    public void onTimerValueChanged(long timerValue) {
        mTimerFragment.updateTimerValue(timerValue);
    }

    @Override
    public void onStopwatchValueChanged(long stopwatchValue, long stopwatchLapValue) {
        mStopwatchFragment.updateStopwatchValue(stopwatchValue);
        mStopwatchFragment.updateStopwatchLapValue(stopwatchLapValue);
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
            switch (position) {
                case TIMER_POSITION:
                    return mTimerFragment;
                case STOPWATCH_POSITION:
                    return mStopwatchFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return NUMBER_OF_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);

            }
            return null;
        }
    }

    public class AlertDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Timer finished")
                    .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mTimerService.stopAlarm();
                            mTimerService.resetTimer();

                            mTimerFragment.updateTimerValue(0);
                            mTimerFragment.mTimerStartStopButton.setBackgroundResource(R.drawable.oval_start_button);
                            mTimerFragment.mTimerTimePicker.setEnabled(true);
                            mTimerFragment.mTimerStartStopButton.setText("Start");
                            setResetButtonState(RESET_BUTTON_DISABLED);

                        }
                    });

            // Create the AlertDialog object and return it
            return builder.create();
        }
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
        private static TextView mTimerLabel;
        private static TextView mStopwatchLabel;
        private static Button mStopwatchResetButton;
        private static Button mStopwatchStartStopButton;
        private static TextView mStopwatchLapLabel;
        private static TimePicker mTimerTimePicker;
        private static Button mTimerStartStopButton;
        private static Button mTimerResetButton;

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

        public static ArrayList<String> mSavedStopwatchValues;
        public static ArrayAdapter<String> mAdapter;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            Bundle args = getArguments();
            int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);

            View rootView = null;

            switch (sectionNumber) {
                case TIMER_POSITION:
                    View timerView = inflater.inflate(R.layout.timer_layout, container, false);
                    mTimerLabel = (TextView) timerView.findViewById(R.id.timer_value);
                    mTimerStartStopButton = (Button) timerView.findViewById(R.id.start_timer_button);
                    mTimerResetButton = (Button) timerView.findViewById(R.id.reset_timer_button);

                    mTimerTimePicker = (TimePicker) timerView.findViewById(R.id.timer_time_picker);
                    mTimerTimePicker.setIs24HourView(true);
                    mTimerTimePicker.setCurrentHour(0);
                    mTimerTimePicker.setCurrentMinute(0);

                    mTimerTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

                        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {

                            updateDisplay(hourOfDay, minute);
                            MainActivity.setResetButtonState(RESET_BUTTON_ENABLED);

                        }

                        private void updateDisplay(int hourOfDay, int minute) {

                            long pickedTimerValue = hourOfDay * 60 + minute; pickedTimerValue *= 60000;
                            updateTimerValue(pickedTimerValue);
                            mTimerService.TIMER_START_VALUE = pickedTimerValue;

                        }


                    });


                    return timerView;
                case STOPWATCH_POSITION:
                    mSavedStopwatchValues = new ArrayList<String>();
                    mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.stopwatch_item, R.id.stopwatch_item_value, mSavedStopwatchValues );
                    View stopwatchView = inflater.inflate(R.layout.stopwatch_layout, container, false);
                    mStopwatchLabel = (TextView) stopwatchView.findViewById(R.id.stopwatch_value);
                    mStopwatchLapLabel = (TextView) stopwatchView.findViewById(R.id.stopwatch_laptime_value);
                    mStopwatchResetButton = (Button) stopwatchView.findViewById(R.id.reset_stopwatch_button);
                    mStopwatchStartStopButton = (Button) stopwatchView.findViewById(R.id.start_stopwatch_button);
                    ListView listView = (ListView) stopwatchView.findViewById(R.id.stopwatch_listview);
                    listView.setAdapter(mAdapter);
                    return stopwatchView;
            }
            return null;
        }


        public void updateTimerValue(long timerValue) {
            if (mTimerLabel != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                CharSequence timeForTimer = simpleDateFormat.format(new Date(timerValue));
                mTimerLabel.setText(timeForTimer.subSequence(0, 8));
            }
        }

        public void updateStopwatchValue(long stopwatchValue) {
            if (mStopwatchLabel != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss.SSS", Locale.ENGLISH);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                CharSequence timeForStopwatch = simpleDateFormat.format(new Date(stopwatchValue));
                mStopwatchLabel.setText(timeForStopwatch.subSequence(0, 8));
            }
        }

        public void updateStopwatchLapValue(long stopwatchLapValue) {
            if (mStopwatchLapLabel != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss.SSS", Locale.ENGLISH);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                CharSequence timeForStopwatchLap = simpleDateFormat.format(new Date(stopwatchLapValue));
                mStopwatchLapLabel.setText(timeForStopwatchLap.subSequence(0, 8));
            }
        }

        public void addLapToStopwatchList () {
            String lapTime = mStopwatchLapLabel.getText().toString();
            int lap = (mSavedStopwatchValues.size() + 1);
            mSavedStopwatchValues.add("Lap " + lap + " " + lapTime);
            mAdapter.notifyDataSetChanged();
        }

    }

}
