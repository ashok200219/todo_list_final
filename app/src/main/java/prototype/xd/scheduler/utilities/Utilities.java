package prototype.xd.scheduler.utilities;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences_service;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getTodoListEntriesFromCalendars;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.settingsEntries.CompoundCustomizationEntry;
import prototype.xd.scheduler.views.Switch;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

@SuppressWarnings({"unchecked"})
public class Utilities {
    
    public static String getRootDir() {
        return preferences.getString(ROOT_DIR, "");
    }
    
    public static File getFile(String filename) {
        return new File(getRootDir(), filename);
    }
    
    public static boolean isVerticalOrientation(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    
    public static ArrayList<TodoListEntry> loadTodoEntries(Context context, long day_start, long day_end, ArrayList<Group> groups) {
        
        ArrayList<TodoListEntry> readEntries = new ArrayList<>();
        try {
            ArrayList<String[]> entryParams = loadObject("list");
            ArrayList<String> entryGroupNames = loadObject("list_groupData");
            
            if (!(entryParams.size() == entryGroupNames.size())) {
                log(WARN, "Utilities", "entryParams length: " + entryParams.size() + " entryGroupNames length: " + entryGroupNames.size());
            }
            
            for (int i = 0; i < entryParams.size(); i++) {
                readEntries.add(new TodoListEntry(context, entryParams.get(i), entryGroupNames.get(i), groups));
            }
            
            log(INFO, "Utilities", "read todo list: " + readEntries.size());
        } catch (Exception e) {
            log(INFO, "Utilities", "no todo list");
            logException("Utilities", e);
        }
        
        readEntries.addAll(getTodoListEntriesFromCalendars(context, day_start, day_end));
        return readEntries;
    }
    
    public static void saveEntries(ArrayList<TodoListEntry> entries) {
        try {
            ArrayList<String[]> entryParams = new ArrayList<>();
            ArrayList<String> entryGroupNames = new ArrayList<>();
            
            for (int i = 0; i < entries.size(); i++) {
                TodoListEntry entry = entries.get(i);
                if (!entry.fromSystemCalendar) {
                    entryParams.add(entry.params);
                    entryGroupNames.add(entry.getGroupName());
                }
            }
            
            saveObject("list", entryParams);
            saveObject("list_groupData", entryGroupNames);
            log(INFO, "Utilities", "saved todo list");
        } catch (Exception e) {
            log(ERROR, "Utilities", "missing permission, failed to save todo list");
        }
    }
    
    public static <T> T loadObject(String fileName) throws IOException, ClassNotFoundException {
        ObjectInputStream s = new ObjectInputStream(new FileInputStream(getFile(fileName)));
        Object object = s.readObject();
        s.close();
        return (T) object;
    }
    
    public static void saveObject(String fileName, Object object) throws IOException {
        ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(getFile(fileName)));
        s.writeObject(object);
        s.close();
    }
    
    public static void callImageFileChooser(Activity activity, int requestCode) {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("image/*");
        intent = Intent.createChooser(chooseFile, "Choose an image");
        activity.startActivityForResult(intent, requestCode);
    }
    
    public static ArrayList<TodoListEntry> sortEntries(ArrayList<TodoListEntry> entries, long day) {
        ArrayList<TodoListEntry> newEntries = new ArrayList<>();
        ArrayList<TodoListEntry> oldEntries = new ArrayList<>();
        ArrayList<TodoListEntry> todayEntries = new ArrayList<>();
        ArrayList<TodoListEntry> globalEntries = new ArrayList<>();
        ArrayList<TodoListEntry> otherEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isTodayEntry) {
                todayEntries.add(entries.get(i));
            } else if (entries.get(i).isExpiredEntry) {
                oldEntries.add(entries.get(i));
            } else if (entries.get(i).isUpcomingEntry) {
                newEntries.add(entries.get(i));
            } else if (entries.get(i).isGlobalEntry) {
                globalEntries.add(entries.get(i));
            } else {
                otherEntries.add(entries.get(i));
            }
        }
        
        ArrayList<TodoListEntry> merged = new ArrayList<>();
        merged.addAll(todayEntries);
        merged.addAll(globalEntries);
        merged.addAll(newEntries);
        merged.addAll(oldEntries);
        merged.addAll(otherEntries);
        merged.sort(new TodoListEntryGroupComparator(day));
        merged.sort(new TodoListEntryPriorityComparator());
        
        return merged;
    }
    
    //listener for general settings
    public static void addSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               @Nullable final CompoundCustomizationEntry customizationEntry,
                                               final int stringResource,
                                               final String key,
                                               final int defaultValue) {
        displayTo.setText(displayTo.getContext().getString(stringResource, preferences.getInt(key, defaultValue)));
        slider.setValue(preferences.getInt(key, defaultValue));
        slider.addOnChangeListener((listener_slider, progress, fromUser) -> {
            if (fromUser) {
                displayTo.setText(displayTo.getContext().getString(stringResource, (int) progress));
                if (customizationEntry != null) {
                    switch (key) {
                        case UPCOMING_BORDER_THICKNESS:
                            customizationEntry.updateUpcomingPreviewBorderThickness((int) progress);
                            break;
                        case BORDER_THICKNESS:
                            customizationEntry.updateCurrentPreviewBorderThickness((int) progress);
                            break;
                        case EXPIRED_BORDER_THICKNESS:
                            customizationEntry.updateExpiredPreviewBorderThickness((int) progress);
                            break;
                    }
                }
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                preferences.edit().putInt(key, (int) slider.getValue()).apply();
                preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
            }
        });
    }
    
    //listener for entry settings
    public static void addSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               final TextView stateIcon,
                                               final EntrySettings entrySettings,
                                               final boolean mapToBorderPreview,
                                               final int stringResource,
                                               final String parameter,
                                               final int initialValue) {
        displayTo.setText(displayTo.getContext().getString(stringResource, initialValue));
        slider.setValue(initialValue);
        slider.addOnChangeListener((slider1, progress, fromUser) -> {
            if (fromUser) {
                displayTo.setText(displayTo.getContext().getString(stringResource, (int) progress));
                if (mapToBorderPreview) {
                    //TODO: think about switching to floats
                    entrySettings.preview_border.setPadding((int) progress, (int) progress, (int) progress, 0);
                }
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                entrySettings.changeEntryParameter(stateIcon, parameter, String.valueOf((int) slider.getValue()));
                entrySettings.todoListEntryStorage.saveEntries();
                preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
            }
        });
    }
    
    //listener for calendar settings
    public static void addSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               final TextView stateIcon,
                                               final SystemCalendarSettings systemCalendarSettings,
                                               final boolean mapToBorderPreview,
                                               final int stringResource,
                                               final String calendarKey,
                                               final ArrayList<String> calendarSubKeys,
                                               final String parameter,
                                               final int defaultValue,
                                               final Slider.OnChangeListener customProgressListener,
                                               final Slider.OnSliderTouchListener customTouchListener) {
        int initialValue = preferences.getInt(getFirstValidKey(calendarSubKeys, parameter), defaultValue);
        displayTo.setText(displayTo.getContext().getString(stringResource, initialValue));
        slider.setValue(initialValue);
        slider.addOnChangeListener((slider1, progress, fromUser) -> {
            if (customProgressListener != null)
                customProgressListener.onValueChange(slider1, progress, fromUser);
            if (fromUser) {
                displayTo.setText(displayTo.getContext().getString(stringResource, (int) progress));
                if (mapToBorderPreview) {
                    systemCalendarSettings.preview_border.setPadding((int) progress, (int) progress, (int) progress, 0);
                }
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                if (customTouchListener != null) customTouchListener.onStartTrackingTouch(slider);
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (customTouchListener != null) customTouchListener.onStopTrackingTouch(slider);
                preferences.edit().putInt(calendarKey + "_" + parameter, (int) slider.getValue()).apply();
                systemCalendarSettings.setStateIconColor(stateIcon, parameter);
                preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
            }
        });
    }
    
    public static void addSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               final TextView stateIcon,
                                               final SystemCalendarSettings systemCalendarSettings,
                                               final boolean mapToBorderPreview,
                                               final int stringResource,
                                               final String calendarKey,
                                               final ArrayList<String> calendarSubKeys,
                                               final String parameter,
                                               final int defaultValue) {
        addSliderChangeListener(
                displayTo,
                slider,
                stateIcon,
                systemCalendarSettings,
                mapToBorderPreview,
                stringResource,
                calendarKey,
                calendarSubKeys,
                parameter,
                defaultValue,
                null, null);
    }
    
    //switch listener for regular settings
    public static void addSwitchChangeListener(final Switch tSwitch, final String key, boolean defaultValue) {
        tSwitch.setCheckedSilent(preferences.getBoolean(key, defaultValue));
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(key, isChecked).apply();
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        });
    }
    
    //switch listener for entry settings
    public static void addSwitchChangeListener(final Switch tSwitch,
                                               final TextView stateIcon,
                                               final TodoListEntryStorage todoListEntryStorage,
                                               final TodoListEntry entry,
                                               final String parameter,
                                               final boolean initialValue) {
        tSwitch.setCheckedSilent(initialValue);
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            entry.changeParameter(parameter, String.valueOf(isChecked));
            entry.setStateIconColor(stateIcon, parameter);
            todoListEntryStorage.saveEntries();
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        });
    }
    
    //switch listener for calendar settings
    public static void addSwitchChangeListener(final Switch tSwitch,
                                               final TextView stateIcon,
                                               final SystemCalendarSettings systemCalendarSettings,
                                               final String calendarKey,
                                               final ArrayList<String> calendarSubKeys,
                                               final String parameter,
                                               final boolean defaultValue) {
        tSwitch.setCheckedSilent(preferences.getBoolean(getFirstValidKey(calendarSubKeys, parameter), defaultValue));
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(calendarKey + "_" + parameter, isChecked).apply();
            systemCalendarSettings.setStateIconColor(stateIcon, parameter);
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        });
    }
    
    //color dialogue for general settings
    public static void invokeColorDialogue(final CardView target,
                                           final CompoundCustomizationEntry customizationEntry,
                                           final String key,
                                           final int defaultValue) {
        invokeColorDialogue(target.getContext(), preferences.getInt(key, defaultValue), (dialog, selectedColor, allColors) -> {
            preferences.edit().putInt(key, selectedColor).apply();
            switch (key) {
                case Keys.UPCOMING_BG_COLOR:
                case Keys.BG_COLOR:
                case Keys.EXPIRED_BG_COLOR:
                    customizationEntry.updatePreviewBgs();
                    break;
                case Keys.UPCOMING_FONT_COLOR:
                case Keys.FONT_COLOR:
                case Keys.EXPIRED_FONT_COLOR:
                    customizationEntry.updatePreviewFonts();
                    break;
                case Keys.UPCOMING_BORDER_COLOR:
                case Keys.BORDER_COLOR:
                case Keys.EXPIRED_BORDER_COLOR:
                    customizationEntry.updatePreviewBorders();
                    break;
            }
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        });
    }
    
    //color dialogue for entry settings
    public static void invokeColorDialogue(final TextView stateIcon,
                                           final EntrySettings settings,
                                           final TodoListEntryStorage todoListEntryStorage,
                                           final TodoListEntry todoListEntry,
                                           final String parameter,
                                           final int initialValue) {
        invokeColorDialogue(stateIcon.getContext(), initialValue, (dialog, selectedColor, allColors) -> {
            todoListEntry.changeParameter(parameter, String.valueOf(selectedColor));
            todoListEntryStorage.saveEntries();
            switch (parameter) {
                case FONT_COLOR:
                    settings.updatePreviewFont(selectedColor);
                    break;
                case BG_COLOR:
                    settings.updatePreviewBg(selectedColor);
                    break;
                case BORDER_COLOR:
                    settings.updatePreviewBorder(selectedColor);
                    break;
            }
            todoListEntry.setStateIconColor(stateIcon, parameter);
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        });
    }
    
    //color dialogue for calendar settings
    public static void invokeColorDialogue(final TextView stateIcon,
                                           final SystemCalendarSettings systemCalendarSettings,
                                           final String calendarKey,
                                           final ArrayList<String> calendarSubKeys,
                                           final String parameter,
                                           final int defaultValue) {
        invokeColorDialogue(stateIcon.getContext(), preferences.getInt(getFirstValidKey(calendarSubKeys, parameter), defaultValue), (dialog, selectedColor, allColors) -> {
            preferences.edit().putInt(calendarKey + "_" + parameter, selectedColor).apply();
            switch (parameter) {
                case FONT_COLOR:
                    systemCalendarSettings.updatePreviewFont(selectedColor);
                    break;
                case BG_COLOR:
                    systemCalendarSettings.updatePreviewBg(selectedColor);
                    break;
                case BORDER_COLOR:
                    systemCalendarSettings.updatePreviewBorder(selectedColor);
                    break;
            }
            systemCalendarSettings.setStateIconColor(stateIcon, parameter);
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        });
    }
    
    public static void invokeColorDialogue(final Context context, final int initialValue, ColorPickerClickListener listener) {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.choose_color))
                .initialColor(initialValue)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(context.getString(R.string.apply), listener)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                }).build().show();
    }
    
    /**
     * Colorize a specific substring in a string for TextView. Use it like this: <pre>
     * textView.setText(
     *     Strings.colorized("This word is black.","black", Color.BLACK),
     *     TextView.BufferType.SPANNABLE
     * );
     * </pre>
     *
     * @param text Text that contains a substring to colorize
     * @param word Substring to colorize
     * @param argb Color
     * @return Spannable
     */
    public static Spannable colorize(final String text, final String word, final int argb) {
        final Spannable spannable = new SpannableString(text);
        int substringStart = 0;
        int start;
        while ((start = text.indexOf(word, substringStart)) >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(argb), start, start + word.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            substringStart = start + word.length();
        }
        return spannable;
    }
    
    public static long RFC2445ToMilliseconds(String str) {
        if (str == null || str.isEmpty())
            throw new IllegalArgumentException("Null or empty RFC string");
        
        int sign = 1;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        
        int len = str.length();
        int index = 0;
        char c;
        
        c = str.charAt(0);
        
        if (c == '-') {
            sign = -1;
            index++;
        } else if (c == '+')
            index++;
        
        c = str.charAt(index);
        
        if (c != 'P')
            throw new IllegalArgumentException("Duration.parse(str='" + str + "') expected 'P' at index=" + index);
        
        index++;
        c = str.charAt(index);
        if (c == 'T')
            index++;
        
        int n = 0;
        for (; index < len; index++) {
            c = str.charAt(index);
            
            if (c >= '0' && c <= '9') {
                n *= 10;
                n += c - '0';
            } else if (c == 'W') {
                weeks = n;
                n = 0;
            } else if (c == 'H') {
                hours = n;
                n = 0;
            } else if (c == 'M') {
                minutes = n;
                n = 0;
            } else if (c == 'S') {
                seconds = n;
                n = 0;
            } else if (c == 'D') {
                days = n;
                n = 0;
            } else if (c != 'T') {
                throw new IllegalArgumentException("Duration.parse(str='" + str + "') unexpected char '" + c + "' at index=" + index);
            }
        }
        
        return 1000 * sign * ((7L * 24 * 60 * 60 * weeks)
                + (24L * 60 * 60 * days)
                + (60L * 60 * hours)
                + (60L * minutes)
                + seconds);
    }
}

class TodoListEntryPriorityComparator implements Comparator<TodoListEntry> {
    @Override
    public int compare(TodoListEntry o1, TodoListEntry o2) {
        return Integer.compare(o2.priority, o1.priority);
    }
}

class TodoListEntryGroupComparator implements Comparator<TodoListEntry> {
    
    final long day;
    
    public TodoListEntryGroupComparator(long day) {
        this.day = day;
    }
    
    @Override
    public int compare(TodoListEntry o1, TodoListEntry o2) {
        if (o1.fromSystemCalendar || o2.fromSystemCalendar) {
            return Long.compare(o1.getNearestEventTimestamp(day), o2.getNearestEventTimestamp(day));
        }
        return Integer.compare(o1.getGroupName().hashCode(), o2.getGroupName().hashCode());
    }
}
