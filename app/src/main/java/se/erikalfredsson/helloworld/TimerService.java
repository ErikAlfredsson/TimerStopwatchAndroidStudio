package se.erikalfredsson.helloworld;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class TimerService extends Service implements Handler.Callback {
    private static final int NOTIFICATION_ID = 1000;
    private static final int COUNTDOWN_TIMER_MSG = 2000;
    private static final int ALARM_TIMER_MSG = 3000;
    public static long TIMER_START_VALUE = 3000;
    private final LocalBinder mLocalBinder = new LocalBinder();
    private TimerCallback mTimerCallback;
    private NotificationCompat.Builder mNotification;
    private long mTimerMsLeft;
    private Handler mHandler;
    private long mStartTime;
    private boolean mTimerIsRunning;
    private Ringtone mRingtone;


    @Override
    public boolean handleMessage(Message msg) {
        long now = SystemClock.elapsedRealtime();
        if(msg.what == COUNTDOWN_TIMER_MSG) {
            mTimerMsLeft = TIMER_START_VALUE - (now - mStartTime);
            if (mTimerMsLeft >= 0) {
                notifyTimerCallback();
                mHandler.sendEmptyMessageDelayed(COUNTDOWN_TIMER_MSG, 10);
            } else {
                if (mTimerIsRunning) {
                    triggerAlarm();
                }
            }
        }
        if (msg.what == ALARM_TIMER_MSG) {
            if (mRingtone.isPlaying()){
                mRingtone.stop();
            }
        }
        return false;
    }

    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
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

    public void startTimer() {
        startService(new Intent(this, getClass()));
        startTimeInForeground();
    }

    private void startTimeInForeground() {

        mNotification = NewMessageNotification.buildNotification(this, "Timer started", "The timer is running", "Desc");

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("sectionNumber", 1);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

// Creates the PendingIntent
        PendingIntent notifyIntent =
                PendingIntent.getActivity(
                        this,
                        1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mNotification.setContentIntent(notifyIntent);
        mNotification.setWhen(System.currentTimeMillis() + TIMER_START_VALUE);

        startForeground(NOTIFICATION_ID, mNotification.build());

        mTimerIsRunning = true;

        mStartTime = SystemClock.elapsedRealtime();
        mTimerMsLeft = TIMER_START_VALUE;
        mHandler.sendEmptyMessage(COUNTDOWN_TIMER_MSG);
    }

    public void stopTimer() {

        stopForeground(true);
        mTimerIsRunning = false;

        TIMER_START_VALUE = mTimerMsLeft;
    }

    public void resetTimer() {
        TIMER_START_VALUE = 0;
    }

    public boolean isTimerRunning() {
        if (mTimerIsRunning) {
            return true;
        } else {
            return false;
        }
    }

    public void triggerAlarm() {
        mTimerIsRunning = false;
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mRingtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        mRingtone.play();
        mHandler.sendEmptyMessageDelayed(ALARM_TIMER_MSG, 30000);
        //TODO alertdialog där man kan stänga av
        //MainActivity.
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("alertDialog", 20);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    public void stopAlarm() {
        mRingtone.stop();
    }

    public long getTimerValue() {
        return mTimerMsLeft;
    }

    private void notifyTimerCallback() {
        if (mTimerMsLeft < 0) {
            mTimerMsLeft = 0;
        }
        if(mTimerCallback != null) {
            mTimerCallback.onTimerValueChanged(mTimerMsLeft);
        }
    }

    public void setTimerCallback(TimerCallback timerCallback) {
        mTimerCallback = timerCallback;
    }

    public interface TimerCallback {
        void onTimerValueChanged(long timerValue);
    }
}