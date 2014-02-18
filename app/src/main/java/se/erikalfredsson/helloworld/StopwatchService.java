package se.erikalfredsson.helloworld;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

public class StopwatchService extends Service implements Handler.Callback {
    private static final int NOTIFICATION_ID = 2000;
    private static final int STOPWATCH_MSG = 3000;
    private static final int STOPWATCH_NOTIFICATION_MSG = 4000;
    private final LocalBinder mLocalBinder = new LocalBinder();
    private StopwatchCallback mStopwatchCallback;
    private NotificationCompat.Builder mNotification;
    private long mElapsedStopwatchTimer;
    private Handler mHandler;
    private long mStopwatchStartTime;
    private boolean mStopwatchIsRunning;
    private long mLastTime;
    private long mStopwatchLapTimeValue;
    public long mCurrLapTime;


    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == STOPWATCH_MSG) {
            long now = SystemClock.elapsedRealtime();
            mElapsedStopwatchTimer = now - mStopwatchStartTime;
            mStopwatchLapTimeValue = mElapsedStopwatchTimer - mCurrLapTime;
            if (mStopwatchIsRunning) {
                notifyStopwatchCallback();
                mHandler.sendEmptyMessageDelayed(STOPWATCH_MSG, 10);
            }
        }
        return false;
    }

    public class LocalBinder extends Binder {
        public StopwatchService getService() {
            return StopwatchService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(getMainLooper(), this);
    }

    public void startStopwatch() {
        startService(new Intent(this, getClass()));
        startStopwatchInForeground();
    }

    public boolean isStopwatchIsRunning() {
        if (mStopwatchIsRunning) {
            return true;
        } else {
            return false;
        }
    }

    private void startStopwatchInForeground() {

        if (!mStopwatchIsRunning) {
            mStopwatchIsRunning = true;

            mNotification = NewMessageNotification.buildNotification(this, "Stopwatch started", "The stopwatch is running", "Lap");

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("sectionNumber", 0);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent notifyIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mNotification.setContentIntent(notifyIntent);
            mNotification.setWhen(System.currentTimeMillis());

            startForeground(NOTIFICATION_ID, mNotification.build());

            if (mStopwatchStartTime == 0) {
                mStopwatchStartTime = SystemClock.elapsedRealtime();
            } else {
                mStopwatchStartTime = SystemClock.elapsedRealtime() - mLastTime;
            }

            mHandler.sendEmptyMessage(STOPWATCH_MSG);
        }
    }

    public void stopStopwatch() {

        stopForeground(true);
        mStopwatchIsRunning = false;
        mLastTime = SystemClock.elapsedRealtime() - mStopwatchStartTime;
    }

    public void resetStopwatch() {
        mStopwatchStartTime = SystemClock.elapsedRealtime();
        if (!mStopwatchIsRunning) {
            mLastTime = 0;
        }
    }

    public long getStopwatchValue() {
        return mElapsedStopwatchTimer;
    }

    private void notifyStopwatchCallback() {
        if(mStopwatchCallback != null) {
            mStopwatchCallback.onStopwatchValueChanged(mElapsedStopwatchTimer, mStopwatchLapTimeValue);
        }
    }

    public void setStopwatchCallback(MainActivity stopwatchCallback) {
        mStopwatchCallback = stopwatchCallback;
    }

    public interface StopwatchCallback {
        void onStopwatchValueChanged(long stopwatchValue, long stopwatchLapValue);
    }
}