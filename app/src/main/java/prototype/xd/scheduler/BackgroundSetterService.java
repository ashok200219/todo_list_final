package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.getCurrentTime;
import static prototype.xd.scheduler.utilities.DateManager.isDayTime;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class BackgroundSetterService extends Service {
    
    private SharedPreferences preferences;
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    private Timer refreshTimer;
    
    public static void restart(Context context) {
        context.stopService(new Intent(context, BackgroundSetterService.class));
        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // Foreground service notification =========
    private final int foregroundNotificationId = (int) (System.currentTimeMillis() % 10000);
    
    // Notification
    private NotificationCompat.Builder foregroundNotification = null;
    
    private NotificationCompat.Builder getForegroundNotification() {
        if (foregroundNotification == null) {
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), getForegroundNotificationChannelId())
                    .setSmallIcon(R.drawable.ic_settings)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSound(null)
                    .setOngoing(true)
                    .setSilent(true)
                    .setShowWhen(false)
                    .setContentText(getString(R.string.background_service_persistent_message));
        }
        return foregroundNotification;
    }
    
    // Notification channel name
    private String foregroundNotificationChannelName = null;
    
    private String getForegroundNotificationChannelName() {
        if (foregroundNotificationChannelName == null) {
            foregroundNotificationChannelName = getString(R.string.service_name);
        }
        return foregroundNotificationChannelName;
    }
    
    
    // Notification channel description
    private String foregroundNotificationChannelDescription = null;
    
    private String getForegroundNotificationChannelDescription() {
        if (foregroundNotificationChannelDescription == null) {
            foregroundNotificationChannelDescription = getString(R.string.service_description);
        }
        return foregroundNotificationChannelDescription;
    }
    
    // Notification channel id
    private String foregroundNotificationChannelId = null;
    private NotificationManager notificationManager;
    
    public String getForegroundNotificationChannelId() {
        if (foregroundNotificationChannelId == null) {
            foregroundNotificationChannelId = "BackgroundSetterService.NotificationChannel";
            
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // Not exists so we create it at first time
            if (notificationManager.getNotificationChannel(foregroundNotificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        getForegroundNotificationChannelId(),
                        getForegroundNotificationChannelName(),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                notificationManager.createNotificationChannel(nc);
                nc.setDescription(getForegroundNotificationChannelDescription());
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
        }
        return foregroundNotificationChannelId;
    }
    
    private void updateNotification() {
        if (lockScreenBitmapDrawer == null) {
            getForegroundNotification().setContentText(getString(R.string.service_sleep_mode));
        } else {
            getForegroundNotification().setContentTitle(getString(R.string.last_update_time, getCurrentTime()));
        }
        notificationManager.notify(foregroundNotificationId, getForegroundNotification().build());
    }
    
    // Lifecycle ===============================
    
    private boolean lastUpdateSucceeded = false;
    private boolean initialized = false;
    private BroadcastReceiver screenOnOffReceiver;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || initialized) {
            if (lockScreenBitmapDrawer != null) {
                lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
                updateNotification();
            }
        } else {
            initialized = true;
            preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
            //register receivers
            screenOnOffReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!lastUpdateSucceeded || preferences.getBoolean(SERVICE_UPDATE_SIGNAL, false)) {
                        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
                        preferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, false).apply();
                    }
                }
            };
            registerReceiver(screenOnOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            registerReceiver(screenOnOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
            
            startForeground(foregroundNotificationId, getForegroundNotification().build());
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(this);
            
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (isDayTime()) {
                        preferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
                        updateDate(DAY_FLAG_GLOBAL_STR, false);
                        if (lockScreenBitmapDrawer == null) {
                            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(BackgroundSetterService.this);
                            lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
                            updateNotification();
                        }
                    } else {
                        if (lockScreenBitmapDrawer != null) {
                            lockScreenBitmapDrawer = null;
                            updateNotification();
                            System.gc();
                        }
                    }
                }
            }, 5000, 1000 * 60 * 30); //approximately every 30 minutes if day
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        lockScreenBitmapDrawer = null;
        foregroundNotification = null;
        notificationManager = null;
        unregisterReceiver(screenOnOffReceiver);
        if (refreshTimer != null) refreshTimer.cancel();
    }
}