package prototype.xd.scheduler.utilities;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;

public class Keys {
    
    private Keys() {
        throw new IllegalStateException("Utility key storage class");
    }
    
    public abstract static class DefaultedValue<T> {
        
        public final String key;
        public final T defaultValue;
        
        DefaultedValue(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
        
        protected abstract T getInternal(SharedPreferences preferences, String actualKey, T actualDefaultValue);
        
        public T get(SharedPreferences preferences, @Nullable List<String> subKeys, T actualDefaultValue) {
            if (subKeys != null) {
                return getInternal(preferences, getFirstValidKey(subKeys, key), actualDefaultValue);
            }
            return getInternal(preferences, key, actualDefaultValue);
        }
        
        public T get(@Nullable List<String> subKeys) {
            return get(preferences, subKeys, defaultValue);
        }
        
        public T get(@Nullable List<String> subKeys, T defaultValueOverride) {
            return get(preferences, subKeys, defaultValueOverride);
        }
        
        public T get(SharedPreferences preferences) {
            return get(preferences, null, defaultValue);
        }
        
        public T get() {
            return get(preferences, null, defaultValue);
        }
        
        public abstract void put(T value);
        
        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof DefaultedValue<?>))
                return false;
            DefaultedValue<?> val = (DefaultedValue<?>) obj;
            return Objects.equals(val.defaultValue, defaultValue) && val.key.equals(key);
        }
        
        @NonNull
        @Override
        public String toString() {
            return "Defaulted value: " + key + " (" + defaultValue + ")";
        }
    }
    
    public static class DefaultedBoolean extends DefaultedValue<Boolean> {
        DefaultedBoolean(String key, Boolean defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected Boolean getInternal(SharedPreferences preferences, String actualKey, Boolean actualDefaultValue) {
            return preferences.getBoolean(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(Boolean value) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }
    
    public static class DefaultedInteger extends DefaultedValue<Integer> {
        DefaultedInteger(String key, Integer defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected Integer getInternal(SharedPreferences preferences, String actualKey, Integer actualDefaultValue) {
            return preferences.getInt(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(Integer value) {
            preferences.edit().putInt(key, value).apply();
        }
    }
    
    public static class DefaultedFloat extends DefaultedValue<Float> {
        DefaultedFloat(String key, Float defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected Float getInternal(SharedPreferences preferences, String actualKey, Float actualDefaultValue) {
            return preferences.getFloat(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(Float value) {
            preferences.edit().putFloat(key, value).apply();
        }
    }
    
    public static class DefaultedString extends DefaultedValue<String> {
        DefaultedString(String key, String defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected String getInternal(SharedPreferences preferences, String actualKey, String actualDefaultValue) {
            return preferences.getString(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(String value) {
            preferences.edit().putString(key, value).apply();
        }
    }
    
    public static class DefaultedEnum<T extends Enum<T>> extends DefaultedValue<T> {
        
        private final Class<T> enumClass;
        
        DefaultedEnum(String key, T defaultValue, Class<T> enumClass) {
            super(key, defaultValue);
            this.enumClass = enumClass;
        }
        
        @Override
        protected T getInternal(SharedPreferences preferences, String actualKey, T actualDefaultValue) {
            String valName = preferences.getString(actualKey, null);
            return valName == null ? actualDefaultValue : T.valueOf(enumClass, valName);
        }
        
        @Override
        public void put(T value) {
            preferences.edit().putString(key, value.name()).apply();
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue, enumClass);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) && ((DefaultedEnum<?>) obj).enumClass.equals(enumClass);
        }
    }
    
    public static void initPrefs(Context context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREFERENCES_MAIN, Context.MODE_PRIVATE);
            servicePreferences = context.getSharedPreferences(PREFERENCES_SERVICE, Context.MODE_PRIVATE);
        }
    }
    
    public static <T> void putAny(String key, T value) {
        if (value.getClass() == Integer.class) {
            preferences.edit().putInt(key, (Integer) value).apply();
        } else if (value.getClass() == String.class) {
            preferences.edit().putString(key, (String) value).apply();
        } else if (value.getClass() == Boolean.class) {
            preferences.edit().putBoolean(key, (Boolean) value).apply();
        } else if (value.getClass() == Long.class) {
            preferences.edit().putLong(key, (Long) value).apply();
        } else if (value.getClass() == Float.class) {
            preferences.edit().putFloat(key, (Float) value).apply();
        } else {
            Logger.error("Keys", "Can't put key: " + key + " with value " + value);
        }
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    public static Map<String, ?> getAll() {
        return preferences.getAll();
    }
    
    public static SharedPreferences.Editor edit() {
        return preferences.edit();
    }
    
    public static void clearAll() {
        preferences.edit().clear().apply();
    }
    
    public static int getFirstValidKeyIndex(List<String> calendarSubKeys, String parameter) {
        for (int i = calendarSubKeys.size() - 1; i >= 0; i--) {
            try {
                if (preferences.getString(calendarSubKeys.get(i) + "_" + parameter, null) != null) {
                    return i;
                }
            } catch (ClassCastException e) {
                return i;
            }
        }
        return -1;
    }
    
    public static String getFirstValidKey(List<String> calendarSubKeys, String parameter) {
        int index = getFirstValidKeyIndex(calendarSubKeys, parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index) + "_" + parameter;
    }
    
    public static void setBitmapUpdateFlag() {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL.key, true).apply();
    }
    
    public static boolean getBitmapUpdateFlag() {
        return SERVICE_UPDATE_SIGNAL.get(servicePreferences);
    }
    
    public static void clearBitmapUpdateFlag() {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL.key, false).apply();
    }
    
    private static volatile SharedPreferences preferences;
    private static volatile SharedPreferences servicePreferences;
    
    public static final float DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR = 0.75f;
    public static final float DEFAULT_CALENDAR_EVENT_BG_COLOR_MIX_FACTOR = 0.85f;
    public static final float DEFAULT_CALENDAR_EVENT_TIME_COLOR_MIX_FACTOR = 0.25f;
    public static final float DEFAULT_TITLE_FONT_SIZE_MULTIPLIER = 1.1F;
    
    public static final int DAY_FLAG_GLOBAL = -1;
    public static final String DAY_FLAG_GLOBAL_STR = "-1";
    
    public static final String VISIBLE = "visible";
    public static final boolean CALENDAR_SETTINGS_DEFAULT_VISIBLE = true;
    public static final String TEXT_VALUE = "value";
    public static final String IS_COMPLETED = "completed";
    public static final DefaultedBoolean CALENDAR_SHOW_ON_LOCK = new DefaultedBoolean("lock", true);
    public static final String START_DAY_UTC = "startDay";
    public static final String END_DAY_UTC = "endDay";
    public static final DefaultedInteger PRIORITY = new DefaultedInteger("priority", 0);
    
    public static final DefaultedInteger BG_COLOR = new DefaultedInteger("bgColor", 0xff_999999);
    
    public static final Function<Integer, Integer> SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR = eventColor ->
            mixTwoColors(Color.WHITE, eventColor, Keys.DEFAULT_CALENDAR_EVENT_BG_COLOR_MIX_FACTOR);
    
    public static final DefaultedInteger UPCOMING_BG_COLOR = new DefaultedInteger("upcomingBgColor", 0xff_CCFFCC);
    public static final DefaultedInteger EXPIRED_BG_COLOR = new DefaultedInteger("expiredBgColor", 0xff_FFCCCC);
    
    public static final DefaultedInteger BORDER_COLOR = new DefaultedInteger("bevelColor", 0xff_777777);
    public static final DefaultedInteger UPCOMING_BORDER_COLOR = new DefaultedInteger("upcomingBevelColor", 0xff_88FF88);
    public static final DefaultedInteger EXPIRED_BORDER_COLOR = new DefaultedInteger("expiredBevelColor", 0xff_FF8888);
    
    public static final DefaultedInteger BORDER_THICKNESS = new DefaultedInteger("bevelThickness", 2);
    public static final DefaultedInteger UPCOMING_BORDER_THICKNESS = new DefaultedInteger("upcomingBevelThickness", 3);
    public static final DefaultedInteger EXPIRED_BORDER_THICKNESS = new DefaultedInteger("expiredBevelThickness", 3);
    
    public static final DefaultedInteger FONT_COLOR = new DefaultedInteger("fontColor", 0xff_000000);
    public static final DefaultedInteger UPCOMING_FONT_COLOR = new DefaultedInteger("upcomingFontColor", 0xff_005500);
    public static final DefaultedInteger EXPIRED_FONT_COLOR = new DefaultedInteger("expiredFontColor", 0xff_990000);
    
    public static final DefaultedInteger FONT_SIZE = new DefaultedInteger("fontSize", 15);
    public static final DefaultedBoolean ADAPTIVE_BACKGROUND_ENABLED = new DefaultedBoolean("adaptive_background_enabled", false);
    public static final DefaultedInteger ADAPTIVE_COLOR_BALANCE = new DefaultedInteger("adaptive_color_balance", 3);
    
    public static final DefaultedInteger LOCKSCREEN_VIEW_VERTICAL_BIAS = new DefaultedInteger("lockscreen_view_vertical_bias", 50);
    
    public static final DefaultedBoolean HIDE_ENTRIES_BY_CONTENT = new DefaultedBoolean("hide_entries_by_content", false);
    public static final DefaultedString HIDE_ENTRIES_BY_CONTENT_CONTENT = new DefaultedString("hide_entries_by_content_content", "");
    
    public static final DefaultedInteger UPCOMING_ITEMS_OFFSET = new DefaultedInteger("dayOffset_upcoming", 0);
    public static final DefaultedInteger EXPIRED_ITEMS_OFFSET = new DefaultedInteger("dayOffset_expired", 0);
    public static final int SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET = 14;
    
    public static final DefaultedBoolean SHOW_UPCOMING_EXPIRED_IN_LIST = new DefaultedBoolean("upcomingExpiredVisibleInList", true);
    public static final DefaultedBoolean HIDE_EXPIRED_ENTRIES_BY_TIME = new DefaultedBoolean("hide_entries_strict", false);
    public static final DefaultedBoolean ITEM_FULL_WIDTH_LOCK = new DefaultedBoolean("force_max_RWidth_lock", true);
    
    public static final DefaultedBoolean SHOW_GLOBAL_ITEMS_LOCK = new DefaultedBoolean("show_global_tasks_lock", true);
    public static final DefaultedBoolean SHOW_GLOBAL_ITEMS_LABEL_LOCK = new DefaultedBoolean("show_global_tasks_label_lock", true);
    
    public static final DefaultedBoolean ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS = new DefaultedBoolean("allow_global_calendar_settings", false);
    
    public static final DefaultedString TODO_ITEM_VIEW_TYPE = new DefaultedString("lockScreenTodoItemViewType", LockScreenTodoItemView.TodoItemViewType.BASIC.name());
    
    public static final int APP_THEME_LIGHT = MODE_NIGHT_NO;
    public static final int APP_THEME_DARK = MODE_NIGHT_YES;
    public static final int APP_THEME_SYSTEM = MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int DEFAULT_APP_THEME = APP_THEME_SYSTEM;
    public static final List<Integer> appThemes = Collections.unmodifiableList(Arrays.asList(APP_THEME_DARK, APP_THEME_SYSTEM, APP_THEME_LIGHT));
    public static final DefaultedInteger APP_THEME = new DefaultedInteger("app_theme", DEFAULT_APP_THEME);
    
    public static final String PREFERENCES_MAIN = "prefs";
    public static final String PREFERENCES_SERVICE = "prefs_service";
    
    public static final DefaultedBoolean INTRO_SHOWN = new DefaultedBoolean("app_intro", false);
    
    public static final DefaultedBoolean SERVICE_UPDATE_SIGNAL = new DefaultedBoolean("update_lockscreen", false);
    public static final String SERVICE_KEEP_ALIVE_SIGNAL = "keep_alive";
    public static final DefaultedBoolean SERVICE_FAILED = new DefaultedBoolean("service_failed", false);
    public static final DefaultedBoolean WALLPAPER_OBTAIN_FAILED = new DefaultedBoolean("wallpaper_obtain_failed", false);
    
    public static final DefaultedInteger DISPLAY_METRICS_HEIGHT = new DefaultedInteger("metrics_H", 100);
    public static final DefaultedInteger DISPLAY_METRICS_WIDTH = new DefaultedInteger("metrics_W", 100);
    public static final DefaultedFloat DISPLAY_METRICS_DENSITY = new DefaultedFloat("metrics_D", -1f);
    
    public static final DefaultedString ROOT_DIR = new DefaultedString("root_directory", "");
    public static final String ENTRIES_FILE = "entries";
    public static final String GROUPS_FILE = "groupData";
    
    public static final String GITHUB_ISSUES = "https://github.com/dgudim/Scheduler/issues";
    public static final String GITHUB_REPO = "https://github.com/dgudim/Scheduler";
    public static final String GITHUB_RELEASES = "https://github.com/dgudim/Scheduler/releases";
    
    public static final int TODO_LIST_INITIAL_CAPACITY = 75;
}
