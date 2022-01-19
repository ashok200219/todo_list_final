package prototype.xd.scheduler.calendarUtilities;

import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.QueryUtilities.getLong;
import static prototype.xd.scheduler.QueryUtilities.getString;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarEventsColumns;

import android.database.Cursor;

public class SystemCalendarEvent {
    
    final long id;
    final String title;
    final String description;
    final String location;
    final String color;
    final long start;
    final long end;
    
    SystemCalendarEvent(Cursor cursor){
        id = getLong(cursor, calendarEventsColumns, Events._ID);
        title = getString(cursor, calendarEventsColumns, Events.TITLE);
        description = getString(cursor, calendarEventsColumns, Events.DESCRIPTION);
        location = getString(cursor, calendarEventsColumns, Events.EVENT_LOCATION);
        color = getString(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
        start = getLong(cursor, calendarEventsColumns, Events.DTSTART);
        end = getLong(cursor, calendarEventsColumns, Events.DTEND);
    }
}