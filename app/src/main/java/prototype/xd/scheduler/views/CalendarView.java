package prototype.xd.scheduler.views;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAY_OF_WEEK;
import static prototype.xd.scheduler.utilities.DateManager.getEndOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.getStartOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.systemLocale;
import static prototype.xd.scheduler.utilities.Utilities.datesEqual;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarDayLayoutBinding;
import prototype.xd.scheduler.databinding.CalendarHeaderBinding;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.TodoEntryManager;

public class CalendarView {
    
    public static final String NAME = CalendarView.class.getSimpleName();
    
    static class CalendarDayViewContainer extends ViewContainer {
        
        private static final int MAX_INDICATORS = 4;
        
        private final CalendarDayLayoutBinding binding;
        private final Context context;
        private LocalDate date;
        
        public CalendarDayViewContainer(@NonNull CalendarDayLayoutBinding bnd, CalendarView container) {
            super(bnd.getRoot());
            binding = bnd;
            context = bnd.getRoot().getContext();
            bnd.root.setOnClickListener(v -> container.selectDate(date));
        }
        
        private void setEventIndicator(int color, int index, boolean visiblePosition, boolean inCalendar) {
            // first index is day text itself
            View eventIndicator = binding.root.getChildAt(index + 1);
            eventIndicator.setVisibility(visiblePosition ? View.VISIBLE : View.INVISIBLE);
            if (!visiblePosition) {
                return;
            }
            
            if (!inCalendar) {
                // wix with the surface color dimming the color
                color = mixTwoColors(color, MaterialColors.getColor(context, R.attr.colorSurface, Color.GRAY), 0.8);
            }
            eventIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        
        // for month dates
        private void setEventIndicatorInCalendar(int color, int index, boolean visiblePosition) {
            setEventIndicator(color, index, visiblePosition, true);
        }
        
        // for in and out dates
        private void setEventIndicatorOffCalendar(int color, int index, boolean visiblePosition) {
            setEventIndicator(color, index, visiblePosition, false);
        }
        
        public void bindTo(CalendarDay elementDay, CalendarView calendarView, TodoEntryManager todoEntryManager) {
            date = elementDay.getDate();
            binding.calendarDayText.setText(String.format(Locale.getDefault(), "%d", date.getDayOfMonth()));
            
            DayPosition dayPosition = elementDay.component2();
            
            int textColor;
            
            if (dayPosition == DayPosition.MonthDate) {
                if (datesEqual(date, DateManager.currentDate)) {
                    textColor = MaterialColors.getColor(context, R.attr.colorPrimary, Color.WHITE);
                } else {
                    textColor = MaterialColors.getColor(context, R.attr.colorOnSurface, Color.WHITE);
                }
                todoEntryManager.processEventIndicators(date.toEpochDay(), MAX_INDICATORS, this::setEventIndicatorInCalendar);
            } else {
                textColor = context.getColor(R.color.gray_harmonized);
                todoEntryManager.processEventIndicators(date.toEpochDay(), MAX_INDICATORS, this::setEventIndicatorOffCalendar);
            }
            
            binding.calendarDayText.setTextColor(textColor);
            
            // highlight current date
            if (datesEqual(date, calendarView.selectedDate) && dayPosition == DayPosition.MonthDate) {
                binding.root.setBackgroundResource(R.drawable.round_bg_calendar_selection);
            } else {
                binding.root.setBackgroundResource(0);
            }
        }
        
    }
    
    static class CalendarHeaderContainer extends ViewContainer {
        
        private final CalendarHeaderBinding binding;
        
        public CalendarHeaderContainer(@NonNull CalendarHeaderBinding bnd) {
            super(bnd.getRoot());
            binding = bnd;
        }
        
        public void bindTo(CalendarMonth calendarMonth, List<DayOfWeek> daysOfWeek) {
            if (binding.weekdayTitlesContainer.getTag() == null) {
                binding.weekdayTitlesContainer.setTag(calendarMonth.getYearMonth());
                for (int i = 0; i < daysOfWeek.size(); i++) {
                    ((TextView) binding.weekdayTitlesContainer.getChildAt(i)).setText(daysOfWeek.get(i).getDisplayName(TextStyle.SHORT, systemLocale));
                }
            }
            binding.monthTitle.setText(String.format(systemLocale, "%s %d",
                    calendarMonth.getYearMonth().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, systemLocale),
                    calendarMonth.getYearMonth().getYear()));
        }
    }
    
    public static final int DAYS_ON_ONE_PANEL = 7 * 6;
    public static final int CACHED_PANELS = 2;
    public static final int POTENTIALLY_VISIBLE_DAYS = DAYS_ON_ONE_PANEL * CACHED_PANELS;
    
    private DayOfWeek firstDayOfWeek;
    private List<DayOfWeek> daysOfWeek;
    
    @Nullable
    LocalDate selectedDate;
    @Nullable
    YearMonth selectedMonth;
    
    private final Set<YearMonth> loadedMonths = new HashSet<>();
    
    private long firstSelectedMonthDayUTC = 0;
    private long lastSelectedMonthDayUTC = 0;
    
    private long firstVisibleDayUTC = 0;
    private long lastVisibleDayUTC = 0;
    
    private long minVisibleDayUTC = 0;
    private long maxVisibleDayUTC = 0;
    
    final com.kizitonwose.calendar.view.CalendarView rootCalendarView;
    @Nullable
    DateChangeListener dateChangeListener;
    @Nullable
    MonthBindListener newMonthBindListener;
    
    public CalendarView(com.kizitonwose.calendar.view.CalendarView rootCalendarView, TodoEntryManager todoEntryManager) {
        this.rootCalendarView = rootCalendarView;
        
        rootCalendarView.setDayBinder(new MonthDayBinder<CalendarDayViewContainer>() {
            @NonNull
            @Override
            public CalendarDayViewContainer create(@NonNull View view) {
                return new CalendarDayViewContainer(CalendarDayLayoutBinding.bind(view), CalendarView.this);
            }
            
            @Override
            public void bind(@NonNull CalendarDayViewContainer container, CalendarDay calendarDay) {
                container.bindTo(calendarDay, CalendarView.this, todoEntryManager);
            }
        });
        
        rootCalendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<CalendarHeaderContainer>() {
            @NonNull
            @Override
            public CalendarHeaderContainer create(@NonNull View view) {
                return new CalendarHeaderContainer(CalendarHeaderBinding.bind(view));
            }
            
            @Override
            public void bind(@NonNull CalendarHeaderContainer container, CalendarMonth calendarMonth) {
                container.bindTo(calendarMonth, daysOfWeek);
                
                YearMonth calendarYearMonth = calendarMonth.getYearMonth();
                
                // new month was loaded
                if (!loadedMonths.contains(calendarYearMonth) && newMonthBindListener != null) {
                    loadedMonths.add(calendarYearMonth);
                    Logger.debug(NAME, "New month loaded: " + calendarYearMonth);
                    newMonthBindListener.onMonthLoaded(calendarYearMonth);
                }
            }
        });
        
        rootCalendarView.setMonthScrollListener(calendarMonth -> {
            // update currently visible day range and selected month
            setSelectedMonth(calendarMonth.getYearMonth(), true);
            return null;
        });
        
        init(FIRST_DAY_OF_WEEK.get());
    }
    
    private void setSelectedMonth(@NonNull YearMonth month, boolean extend) {
        selectedMonth = month;
        
        firstSelectedMonthDayUTC = getStartOfMonthDayUTC(selectedMonth);
        lastSelectedMonthDayUTC = getEndOfMonthDayUTC(selectedMonth);
        
        CalendarDay firstVisibleCalendarDay = rootCalendarView.findFirstVisibleDay();
        CalendarDay lastVisibleCalendarDay = rootCalendarView.findLastVisibleDay();
        
        firstVisibleDayUTC = firstVisibleCalendarDay != null ? firstVisibleCalendarDay.getDate().toEpochDay() : firstSelectedMonthDayUTC;
        lastVisibleDayUTC = lastVisibleCalendarDay != null ? lastVisibleCalendarDay.getDate().toEpochDay() : lastSelectedMonthDayUTC;
    
        if (extend) {
            minVisibleDayUTC = min(minVisibleDayUTC, firstVisibleDayUTC);
            maxVisibleDayUTC = max(maxVisibleDayUTC, lastVisibleDayUTC);
        } else {
            minVisibleDayUTC = firstVisibleDayUTC;
            maxVisibleDayUTC = lastVisibleDayUTC;
        }
    }
    
    private void init(DayOfWeek newFirstDayOfWeek) {
        YearMonth currentMonth = YearMonth.now();
        
        firstDayOfWeek = newFirstDayOfWeek;
        daysOfWeek = daysOfWeek(newFirstDayOfWeek);
        
        rootCalendarView.setup(currentMonth.minusMonths(100), currentMonth.plusMonths(100), daysOfWeek.get(0));
        selectDate(DateManager.currentDate);
        rootCalendarView.scrollToMonth(currentMonth);
        
        setSelectedMonth(currentMonth, false);
    }
    
    public long getFirstLoadedDayUTC() {
        return min(firstVisibleDayUTC - POTENTIALLY_VISIBLE_DAYS, minVisibleDayUTC);
    }
    
    public long getLastLoadedDayUTC() {
        return min(lastVisibleDayUTC + POTENTIALLY_VISIBLE_DAYS, maxVisibleDayUTC);
    }
    
    public void selectDate(LocalDate targetDate) {
        
        rootCalendarView.scrollToDate(targetDate);
        
        // we selected another date
        if (!datesEqual(targetDate, selectedDate)) {
            Logger.debug(NAME, "Date selected: " + targetDate);
            if (selectedDate != null) {
                rootCalendarView.notifyDateChanged(selectedDate);
            }
            rootCalendarView.notifyDateChanged(targetDate);
            selectedDate = targetDate;
            if (dateChangeListener != null) {
                dateChangeListener.onDateChanged(targetDate, rootCalendarView.getContext());
            }
        }
    }
    
    public void setOnDateChangeListener(DateChangeListener dateChangeListener) {
        this.dateChangeListener = dateChangeListener;
    }
    
    public void notifyDayChanged(long targetDayUTC) {
        if (targetDayUTC < getFirstLoadedDayUTC() || targetDayUTC > getLastLoadedDayUTC()) {
            // day is not loaded, skip it
            return;
        }
        DayPosition dayPosition = DayPosition.MonthDate;
        if (targetDayUTC < firstSelectedMonthDayUTC) {
            dayPosition = DayPosition.InDate;
        } else if (targetDayUTC > lastSelectedMonthDayUTC) {
            dayPosition = DayPosition.OutDate;
        }
        LocalDate date = LocalDate.ofEpochDay(targetDayUTC);
        if (dayPosition != DayPosition.MonthDate) {
            rootCalendarView.notifyDateChanged(date, dayPosition);
        } else {
            rootCalendarView.notifyDateChanged(date, DayPosition.InDate);
            rootCalendarView.notifyDateChanged(date, DayPosition.OutDate);
        }
        rootCalendarView.notifyDateChanged(date, DayPosition.MonthDate);
    }
    
    public void notifyDaysChanged(Set<Long> days) {
        days.forEach(this::notifyDayChanged);
    }
    
    public void notifyCurrentMonthChanged() {
        if (selectedMonth != null) {
            // internally will rebind all visible dates
            rootCalendarView.notifyMonthChanged(selectedMonth);
        }
    }
    
    public void notifyCalendarChanged() {
        DayOfWeek newFirstDayOfWeek = FIRST_DAY_OF_WEEK.get();
        if (!newFirstDayOfWeek.equals(firstDayOfWeek)) {
            init(newFirstDayOfWeek);
        } else {
            rootCalendarView.notifyCalendarChanged();
        }
    }
    
    public void setNewMonthBindListener(@Nullable MonthBindListener newMonthBindListener) {
        this.newMonthBindListener = newMonthBindListener;
    }
    
    @FunctionalInterface
    public interface DateChangeListener {
        void onDateChanged(LocalDate selectedDate, Context context);
    }
    
    @FunctionalInterface
    public interface MonthBindListener {
        void onMonthLoaded(YearMonth yearMonth);
    }
}

