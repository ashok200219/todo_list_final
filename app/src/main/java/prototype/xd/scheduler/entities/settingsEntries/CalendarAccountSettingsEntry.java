package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.CALENDAR_ACCOUNT;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;

public class CalendarAccountSettingsEntry extends SettingsEntry {
    
    private final SettingsFragment fragment;
    private final String accountName;
    private final String accountType;
    
    public CalendarAccountSettingsEntry(final SettingsFragment fragment, SystemCalendar calendar) {
        super(R.layout.account_entry);
        this.fragment = fragment;
        this.accountName = calendar.account_name;
        this.accountType = calendar.account_type;
        entryType = CALENDAR_ACCOUNT;
    }
    
    private int getIconFromAccountType() {
        if (accountType.toLowerCase(Locale.ROOT).contains("google")) {
            return R.drawable.ic_google;
        } else if (accountType.toLowerCase(Locale.ROOT).contains("local")) {
            return R.drawable.ic_mobile;
        }
        return R.drawable.ic_account_circle;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((ImageView) rootView.findViewById(R.id.account_icon)).setImageResource(getIconFromAccountType());
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(accountName);
        ((TextView) rootView.findViewById(R.id.account_type)).setText(accountType);
        rootView.findViewById(R.id.edit_button).setOnClickListener(v -> fragment.calendarSettingsDialogue.show(accountName));
        return super.InitInnerViews(rootView);
    }
}
