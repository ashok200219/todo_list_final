package prototype.xd.scheduler.utilities.services;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimeString;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES_SERVICE;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_KEEP_ALIVE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.PreferencesStore;

public class BackgroundSetterService extends Service {
    
    private static final String NAME = "Bg setter service";
    
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    public static void ping(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
    }
    
    public static void keepAlive(Context context) {
        Intent keepAliveIntent = new Intent(context, BackgroundSetterService.class);
        keepAliveIntent.putExtra(SERVICE_KEEP_ALIVE_SIGNAL, 1);
        ContextCompat.startForegroundService(context, keepAliveIntent);
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
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), getNotificationChannelId())
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
    private String notificationChannelName = null;
    
    private String getNotificationChannelName() {
        if (notificationChannelName == null) {
            notificationChannelName = getString(R.string.service_name);
        }
        return notificationChannelName;
    }
    
    
    // Notification channel description
    private String notificationChannelDescription = null;
    
    private String getNotificationChannelDescription() {
        if (notificationChannelDescription == null) {
            notificationChannelDescription = getString(R.string.service_description);
        }
        return notificationChannelDescription;
    }
    
    // Notification channel id
    private String notificationChannelId = null;
    private NotificationManager notificationManager;
    
    public String getNotificationChannelId() {
        if (notificationChannelId == null) {
            notificationChannelId = "BackgroundSetterService.NotificationChannel";
            
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        getNotificationChannelId(),
                        getNotificationChannelName(),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                notificationManager.createNotificationChannel(nc);
                nc.setDescription(getNotificationChannelDescription());
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
        }
        return notificationChannelId;
    }
    
    private void updateNotification() {
        servicePreferences.edit().putLong(Keys.LAST_UPDATE_TIME, getCurrentTimestamp()).apply();
        getForegroundNotification().setContentTitle(getString(R.string.last_update_time, getCurrentTimeString()));
        notificationManager.notify(foregroundNotificationId, getForegroundNotification().build());
    }
    
    // Lifecycle ===============================
    
    private volatile boolean lastUpdateSucceeded = false;
    private boolean initialized = false;
    private BroadcastReceiver screenOnOffReceiver;
    private BroadcastReceiver pingReceiver;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PreferencesStore.init(this);
        if (intent != null && initialized) {
            if (intent.hasExtra(SERVICE_KEEP_ALIVE_SIGNAL)) {
                servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
                log(DEBUG, NAME, "received ping (keep alive job)");
            } else {
                log(DEBUG, NAME, "received general ping");
                updateDate(DAY_FLAG_GLOBAL, false);
                lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
                updateNotification();
            }
        } else {
            initialized = true;
            log(DEBUG, NAME, "received ping (initial)");
            
            servicePreferences = getSharedPreferences(PREFERENCES_SERVICE, Context.MODE_PRIVATE);
            
            screenOnOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!lastUpdateSucceeded || servicePreferences.getBoolean(SERVICE_UPDATE_SIGNAL, false)) {
                        ping(context);
                        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, false).apply();
                        log(DEBUG, NAME, "sent ping (on - off receiver)");
                    }
                    log(DEBUG, NAME, "receiver state: " + intent.getAction());
                }
            };
            pingReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ping(context);
                    log(DEBUG, NAME, "sent ping (date changed receiver)");
                }
            };
            
            IntentFilter onOffFilter = new IntentFilter();
            onOffFilter.addAction(Intent.ACTION_SCREEN_ON);
            onOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
            
            registerReceiver(screenOnOffReceiver, onOffFilter);
            registerReceiver(pingReceiver, new IntentFilter(Intent.ACTION_DATE_CHANGED));
            scheduleRestartJob();
            startForeground(foregroundNotificationId, getForegroundNotification().build());
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(this);
        }
        return START_STICKY;
    }
    
    private void scheduleRestartJob() {
        log(INFO, NAME, "restart job scheduled");
        getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(0,
                new ComponentName(getApplicationContext(), KeepAliveService.class))
                .setPeriodic(15 * 60L * 1000, 5 * 60L * 1000).build());
    }
    
    @Override
    public void onDestroy() {
        if (screenOnOffReceiver != null) {
            unregisterReceiver(screenOnOffReceiver);
        }
        if (pingReceiver != null) {
            unregisterReceiver(pingReceiver);
        }
        // unregister receivers
        lockScreenBitmapDrawer = null;
    }
}