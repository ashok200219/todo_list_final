package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences_service;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.DateManager.addTimeZoneOffset;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.dateToEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_SYSTEM;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Objects;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;

public class HomeFragment extends Fragment {
    
    private volatile TodoListEntryStorage todoListEntryStorage;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        ListView listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        
        todoListEntryStorage = new TodoListEntryStorage(container);
        listView.setAdapter(todoListEntryStorage.getTodoListViewAdapter());
        
        CalendarView calendarView = view.findViewById(R.id.calendar);
        TextView statusText = view.findViewById(R.id.status_text);
        long epoch;
        if ((epoch = preferences_service.getLong(Keys.PREVIOUSLY_SELECTED_DATE, 0)) != 0) {
            calendarView.setDate(addTimeZoneOffset(epoch, timeZone_SYSTEM));
        }
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            preferences_service.edit().putLong(Keys.PREVIOUSLY_SELECTED_DATE, dateToEpoch(year, month + 1, dayOfMonth)).apply();
            updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
            todoListEntryStorage.lazyLoadEntries(view1.getContext());
            updateStatusText(statusText);
        });
        
        view.findViewById(R.id.to_current_date_button).setOnClickListener(v -> {
            calendarView.setDate(addTimeZoneOffset(currentTimestamp, timeZone_SYSTEM));
            currentlySelectedDay = currentDay;
            preferences_service.edit().remove(Keys.PREVIOUSLY_SELECTED_DATE).apply();
            todoListEntryStorage.updateTodoListAdapter(false);
            updateStatusText(statusText);
        });
        
        view.<FloatingActionButton>findViewById(R.id.fab).setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view1.getContext());
            View addView = inflater.inflate(R.layout.edit_text_spinner_dialogue, container, false);
            builder.setView(addView);
            AlertDialog dialog = builder.create();
            
            final TextInputEditText input = addView.findViewById(R.id.entryNameEditText);
            input.setOnFocusChangeListener((v, hasFocus) -> input.postDelayed(() -> {
                InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }, 200));
            input.requestFocus();
            
            final String[] currentGroup = {""};
            
            final ArrayList<Group> groupList = new ArrayList<>();
            groupList.add(new Group(view1.getContext()));
            groupList.addAll(readGroupFile());
            
            final Spinner groupSpinner = addView.findViewById(R.id.groupSpinner);
            final ArrayAdapter<Group> arrayAdapter = new ArrayAdapter<>(view1.getContext(), android.R.layout.simple_spinner_item, groupList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
            groupSpinner.setAdapter(arrayAdapter);
            groupSpinner.setSelection(0);
            groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                    if (position >= 0) {
                        currentGroup[0] = groupList.get(position).getName();
                    } else {
                        currentGroup[0] = "";
                    }
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                
                }
            });
            
            addView.findViewById(R.id.confirm_button).setOnClickListener(v -> {
                if (input.getText() != null) {
                    TodoListEntry newEntry = new TodoListEntry(v.getContext(), new String[]{
                            TEXT_VALUE, input.getText().toString().trim(),
                            ASSOCIATED_DAY, String.valueOf(currentlySelectedDay),
                            IS_COMPLETED, "false"}, currentGroup[0]);
                    todoListEntryStorage.addEntry(newEntry);
                    todoListEntryStorage.saveEntries();
                    todoListEntryStorage.updateTodoListAdapter(newEntry.getLockViewState());
                    dialog.dismiss();
                }
            });
            
            addView.findViewById(R.id.secondary_action_button).setOnClickListener(v -> {
                if (input.getText() != null) {
                    TodoListEntry newEntry = new TodoListEntry(v.getContext(), new String[]{
                            TEXT_VALUE, input.getText().toString().trim(),
                            ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR,
                            IS_COMPLETED, "false"}, currentGroup[0]);
                    todoListEntryStorage.addEntry(newEntry);
                    todoListEntryStorage.saveEntries();
                    todoListEntryStorage.updateTodoListAdapter(newEntry.getLockViewState());
                    dialog.dismiss();
                }
            });
            
            addView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
        });
        
        view.findViewById(R.id.openSettingsButton).setOnClickListener(v ->
                ((NavHostFragment) Objects.requireNonNull(requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
                        .getNavController().navigate(R.id.action_HomeFragment_to_SettingsFragment));
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        updateDate(DAY_FLAG_GLOBAL_STR, true);
        
        long epoch;
        if ((epoch = preferences_service.getLong(Keys.PREVIOUSLY_SELECTED_DATE, 0)) != 0) {
            currentlySelectedDay = daysFromEpoch(epoch, timeZone_SYSTEM);
        }
        todoListEntryStorage.lazyLoadEntries(view.getContext());
        
        updateStatusText(view.findViewById(R.id.status_text));
    }
    
    private void updateStatusText(TextView statusText) {
        statusText.setText(getString(R.string.status, dateFromEpoch(currentlySelectedDay * 86400000),
                todoListEntryStorage.getCurrentlyVisibleEntries()));
    }
    
    @Override
    public void onDestroy() {
        todoListEntryStorage = null;
        preferences_service.edit().remove(Keys.PREVIOUSLY_SELECTED_DATE).apply();
        super.onDestroy();
    }
}