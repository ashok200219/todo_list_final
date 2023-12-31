package prototype.xd.scheduler.utilities.services;

import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimeStringLocal;
import static prototype.xd.scheduler.utilities.Static.SERVICE_KEEP_ALIVE_SIGNAL;
import static prototype.xd.scheduler.utilities.Static.calendarChangedIntentFilter;
import static prototype.xd.scheduler.utilities.Static.clearBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Static.getBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Static.setBitmapUpdateFlag;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.receivers.BroadcastReceiverHolder;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.receivers.PingReceiver;

public final class BackgroundSetterService extends LifecycleService { // NOSONAR this is a service
    
    public static final String NAME = BackgroundSetterService.class.getSimpleName();
    
    @Nullable
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    @Nullable
    private TodoEntryManager todoEntryManager;
    
    public static void ping(@NonNull Context context, boolean updateInstantly) {
        Intent keepAliveIntent = new Intent(context, BackgroundSetterService.class);
        if (!updateInstantly) {
            keepAliveIntent.putExtra(SERVICE_KEEP_ALIVE_SIGNAL, 1);
        }
        context.startForegroundService(keepAliveIntent);
    }
    
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void exit(@NonNull Context context) {
        Intent intent = new Intent(context, BackgroundSetterService.class);
        context.stopService(intent);
    }
    
    // Foreground service notification =========
    private final int foregroundNotificationId = (int) (System.currentTimeMillis() % 10000);
    
    private NotificationCompat.Builder foregroundNotification;
    private NotificationManager notificationManager;
    
    @NonNull
    private NotificationCompat.Builder getForegroundNotification() {
        if (foregroundNotification == null) {
            
            notificationManager = notificationManager == null ?
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE) : notificationManager;
            
            // create notification channel if it doesn't exist
            String notificationChannelId = "BackgroundSetterService.NotificationChannel";
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        notificationChannelId,
                        getString(R.string.service_name),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                notificationManager.createNotificationChannel(nc);
                nc.setDescription(getString(R.string.service_description));
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId)
                    .setSmallIcon(R.drawable.ic_settings_45)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSound(null)
                    .setOngoing(true)
                    .setSilent(true)
                    .setShowWhen(false)
                    .setContentText(getString(R.string.background_service_persistent_message));
        }
        return foregroundNotification;
    }
    
    private void updateNotification() {
        getForegroundNotification().setContentTitle(getString(R.string.last_update_time, getCurrentTimeStringLocal()));
        notificationManager.notify(foregroundNotificationId, getForegroundNotification().build());
    }
    
    // Lifecycle ===============================
    
    private volatile boolean lastUpdateSucceeded; // NOSONAR
    @NonNull
    private final BroadcastReceiverHolder receiverHolder = new BroadcastReceiverHolder(this);
    
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        
        Static.init(this);
        DateManager.updateDate();
        
        if (lockScreenBitmapDrawer != null && todoEntryManager != null) {
            if (intent != null && intent.hasExtra(SERVICE_KEEP_ALIVE_SIGNAL)) {
                setBitmapUpdateFlag();
                Logger.info(NAME, "Received ping (keep alive job)");
            } else {
                Logger.info(NAME, "Received general ping");
                lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(this, todoEntryManager);
                updateNotification();
            }
        } else {
            Logger.info(NAME, "Received ping (initial)");
            
            receiverHolder.registerReceiver((context, brIntent) -> {
                if (!lastUpdateSucceeded || getBitmapUpdateFlag()) {
                    ping(context, true);
                    clearBitmapUpdateFlag();
                    Logger.info(NAME, "Sent ping (screen on/off)");
                }
                Logger.info(NAME, "Receiver state: " + brIntent.getAction());
            }, filter -> {
                filter.addAction(Intent.ACTION_SCREEN_ON);
                filter.addAction(Intent.ACTION_SCREEN_OFF);
                return filter;
            });
            
            receiverHolder.registerReceiver(
                    new PingReceiver("Date changed", true),
                    new IntentFilter(Intent.ACTION_DATE_CHANGED));
            
            receiverHolder.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ping(context, false);
                    Logger.info(NAME, "Calendar changed!");
                    if (todoEntryManager != null) {
                        todoEntryManager.notifyCalendarProviderChanged(context);
                    }
                }
            }, calendarChangedIntentFilter);
            
            scheduleRestartJob();
            startForeground(foregroundNotificationId, getForegroundNotification().build());
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(this);
            todoEntryManager = TodoEntryManager.getInstance(this);
        }
        return START_STICKY;
    }
    
    private void scheduleRestartJob() {
        Logger.info(NAME, "restart job scheduled");
        getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(0,
                new ComponentName(getApplicationContext(), KeepAliveService.class))
                .setPeriodic(15 * 60L * 1000, 5 * 60L * 1000).build());
    }
}
