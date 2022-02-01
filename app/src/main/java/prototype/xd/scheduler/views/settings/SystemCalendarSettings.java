package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.Keys.NEED_TO_RECONSTRUCT_BITMAP;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKeyIndex;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;

public class SystemCalendarSettings extends PopupSettingsView {
    
    private final AlertDialog dialog;
    private ArrayList<String> calendarSubKeys;
    private TodoListEntry entry;
    private final TodoListEntryStorage todoListEntryStorage;
    
    public SystemCalendarSettings(final TodoListEntryStorage todoListEntryStorage, final View settingsView) {
        super(settingsView);
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        this.todoListEntryStorage = todoListEntryStorage;
        
        dialog = new AlertDialog.Builder(settingsView.getContext()).setOnDismissListener(dialog -> {
            if (todoListEntryStorage != null) {
                todoListEntryStorage.updateTodoListAdapter(preferences.getBoolean(NEED_TO_RECONSTRUCT_BITMAP, false));
            }
            preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, false).apply();
        }).setView(settingsView).create();
    }
    
    public void show(final String calendar_key) {
        initialise(calendar_key);
        dialog.show();
    }
    
    public void show(final TodoListEntry entry) {
        this.entry = entry;
        initialise(makeKey(entry.event));
        dialog.show();
    }
    
    private void initialise(final String calendarKey) {
        
        calendarSubKeys = generateSubKeysFromKey(calendarKey);
        
        updatePreviews(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        updateAllIndicators();
        
        settings_reset_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.reset_settings_prompt);
            
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                Map<String, ?> allEntries = preferences.getAll();
                SharedPreferences.Editor editor = preferences.edit();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    if (entry.getKey().startsWith(calendarKey)) {
                        editor.remove(entry.getKey());
                    }
                }
                editor.apply();
                initialise(calendarKey);
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_select.setOnClickListener(view -> invokeColorDialogue(
                fontColor_view_state, this,
                calendarKey, Keys.FONT_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR)));
        
        bgColor_select.setOnClickListener(view -> invokeColorDialogue(
                bgColor_view_state, this,
                calendarKey, Keys.BG_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR)));
        
        borderColor_select.setOnClickListener(view -> invokeColorDialogue(
                padColor_view_state, this,
                calendarKey, Keys.BORDER_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR)));
        
        addSeekBarChangeListener(
                border_thickness_description,
                border_thickness_bar, border_size_state,
                this, true, R.string.settings_border_thickness,
                calendarKey, Keys.BORDER_THICKNESS,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar, priority_state,
                this, false, R.string.settings_priority,
                calendarKey, Keys.PRIORITY,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY));
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar, adaptiveColor_bar_state,
                this, false, R.string.settings_adaptive_color_balance,
                calendarKey, Keys.ADAPTIVE_COLOR_BALANCE,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE));
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar, showDaysUpcoming_bar_state,
                this, false, R.string.settings_show_days_upcoming,
                calendarKey, Keys.UPCOMING_ITEMS_OFFSET,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET));
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar, showDaysExpired_bar_state,
                this, false, R.string.settings_show_days_expired,
                calendarKey, Keys.EXPIRED_ITEMS_OFFSET,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET));
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state, this,
                calendarKey, Keys.SHOW_ON_LOCK,
                preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK));
        
        addSwitchChangeListener(
                adaptive_color_switch,
                adaptiveColor_switch_state, this,
                calendarKey, Keys.ADAPTIVE_COLOR_ENABLED,
                preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED));
    }
    
    @Override
    public void setStateIconColor(TextView display, String parameter) {
        int keyIndex = getFirstValidKeyIndex(calendarSubKeys, parameter);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_personal));
        } else if (keyIndex >= 0) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_group));
        } else {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_default));
        }
        if (todoListEntryStorage != null) {
            for (TodoListEntry current_entry : todoListEntryStorage.getTodoListEntries()) {
                if (current_entry.fromSystemCalendar) {
                    if (current_entry.event.subKeys.equals(entry.event.subKeys)) {
                        current_entry.reloadParams();
                    }
                }
            }
        }
    }
}
