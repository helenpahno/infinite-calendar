package com.helenpahno.infinitecalendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private String sideTabStatus = "closed"; // A String to track the state of the sidebar
    float density;
    DisplayMetrics displayMetrics;
    int screenHeight;
    int screenWidth;
    public List<FloatingEvent> floatingEvents = new ArrayList<FloatingEvent>();
    public HashMap<String, List<BoundEvent>> allBoundEvents = new HashMap<String, List<BoundEvent>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

        // Set-up the notification channel for the app
        createNotificationChannel();

        // Sending essential central information
        InformationCentral.mainApplicationContext = getApplicationContext();
        InformationCentral.l = getResources().getConfiguration().getLocales().get(0);
        InformationCentral.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        InformationCentral.upcomingEventHUD = (View) findViewById(R.id.upcoming_event_HUD);
        InformationCentral.upcomingHUDstatus = "collapsed";

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
        density = getApplicationContext().getResources().getDisplayMetrics().density;

        InformationCentral.density = density;

        // Makes sure the events and displays are up-to-date and consistent with saved files
        InformationCentral.loadBoundEventLog();
        InformationCentral.loadCategories();

        loadFloatingEvents();
        refreshSideTab();
        InformationCentral.visuallyUpdateUpcomingEventHUD();

        // Open and close tab functionality below
        ImageButton tabButton = (ImageButton) findViewById(R.id.side_tab_button);
        tabButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                switch (sideTabStatus) {
                    case "closed":
                        setSideTabStatus("open");

                        break;
                    case "open":
                    case "minimized":
                        setSideTabStatus("closed");
                        break;
                }
            }
        });

        // Add event functionality below
        ImageButton addEventButton = (ImageButton) findViewById(R.id.add_event_button);
        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Inflate and make it look alright
                View eventForm = inflater.inflate(R.layout.new_event_form, null);
                int width = RelativeLayout.LayoutParams.MATCH_PARENT;
                int height = RelativeLayout.LayoutParams.MATCH_PARENT;

                ViewGroup e = (ViewGroup) eventForm;
                eventForm.findViewById(R.id.complete_button).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.burgundy));
                boolean f = true;
                final PopupWindow eventFormPopUp = new PopupWindow(eventForm, width, height, f);
                eventFormPopUp.showAtLocation(view, Gravity.CENTER, 0, 0);

                // Attach spinner options

                Spinner categoryDropDown = eventForm.findViewById(R.id.category_spinner); // Find category spinner view
                List<Integer> tempColors = new ArrayList<Integer>();
                for (int i = 0; i < InformationCentral.categoryLog.size(); i++) { // Create a list of colors for each event
                    Integer color = InformationCentral.categoryColorMap.get(InformationCentral.categoryLog.get(i));
                    tempColors.add(color);
                }
                EventCategoryAdapter categoryAdapter = new EventCategoryAdapter(InformationCentral.mainApplicationContext, InformationCentral.categoryLog, tempColors, (ViewGroup) eventForm);
                categoryDropDown.setAdapter(categoryAdapter);

                List<String> durationOptions = new ArrayList<String>();
                durationOptions.add("minutes");
                durationOptions.add("hours");
                Spinner durationDropDown = eventForm.findViewById(R.id.duration_type_spinner);
                GenericPinkAdapter durationAdapter = new GenericPinkAdapter(getApplicationContext(), durationOptions, (ViewGroup) eventForm);
                durationDropDown.setAdapter(durationAdapter);

                // Give the button a functionality

                EditText nameField = (EditText) eventForm.findViewById(R.id.name_box);
                EditText durationField = (EditText) eventForm.findViewById(R.id.duration_box);

                Button createButton = (Button) eventForm.findViewById(R.id.complete_button);
                createButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String eventName = nameField.getText().toString(); // Gets the event name from the pop-up text box
                        int duration = 45; // Sets a default event duration in case we can't get a duration
                        try {
                            duration = Integer.parseInt(durationField.getText().toString()); // Tries to get the duration from the pop-up box
                        } catch(Exception e) {
                            System.out.println("ERROR: Provided number unparsable; reverting to default"); // Exactly what it says in the print; trycatch fail
                        }

                        if (durationDropDown.getSelectedItem().toString().equals("hours")) {
                            duration = duration * 60;
                        }

                        String newCategory = categoryDropDown.getSelectedItem().toString(); // Gets the category from the drop-down in the box
                        eventFormPopUp.dismiss(); // Closes the pop-up window

                        System.out.println("DEBUG: New event created with the following characteristics\nName: " + eventName + "\nCategory: " + newCategory + "\nDuration: " + Integer.toString(duration)); // Printout makes sure our characteristics are correct
                        FloatingEvent event = new FloatingEvent(duration, eventName, newCategory); // Creates a FloatingEvent object with the values specified by the user
                        floatingEvents.add(event); // Adds the new event to the list of all floating events
                        saveFloatingEvents(); // Calls the method that saves the floating events to a file
                        refreshSideTab(); // Refresh the events panel in the side tab to display our new event
                    }
                });
            }
        });

        // Pre-recyclerView: generate dates
        Calendar c = Calendar.getInstance();
        Date today = new Date(c.getTimeInMillis());
        InformationCentral.recyclingDates.add(today);
        Date tomorrow = new Date(Date.from(today.toInstant().plus(1, ChronoUnit.DAYS)).getTime());
        InformationCentral.recyclingDates.add(tomorrow);
        Date overmorrow = new Date(Date.from(tomorrow.toInstant().plus(1, ChronoUnit.DAYS)).getTime());
        InformationCentral.recyclingDates.add(overmorrow);
        System.out.println(InformationCentral.recyclingDates.size());

        // RecyclerView: create and assign stuff for it
        RecyclerView scrollCalendarView = (RecyclerView) findViewById(R.id.inf_scroll_calendar);
        InformationCentral.runtimeRecyclerView = scrollCalendarView;
        LinearLayoutManager layoutMgr = new LinearLayoutManager(getApplicationContext());
        scrollCalendarView.setLayoutManager(layoutMgr);
        // Calendar adapter
        CalendarAdapter adapter = new CalendarAdapter(getApplicationContext(), layoutMgr);
        scrollCalendarView.setAdapter(adapter);
        InformationCentral.runtimeAdapter = adapter;

        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
        int currentHour = Integer.parseInt(hourFormat.format(today));

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                scrollCalendarView.smoothScrollBy(0, (int) (2880/24 * density * currentHour + 0.5f));
            }
        }, 200);

        // Post-recyclerView: set drag listeners for each text element and bind events to the clock.
        scrollCalendarView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Set the text on the HUD displaying the current day
                TextView dateHUD = (TextView) findViewById(R.id.current_date_HUD);
                int firstPos = layoutMgr.findFirstVisibleItemPosition();

                Date currentDay = InformationCentral.recyclingDates.get(firstPos);
                java.util.Date utilityDay = new java.util.Date(currentDay.getTime());
                Calendar c = Calendar.getInstance();
                c.setTime(utilityDay);
                Locale l = getResources().getConfiguration().getLocales().get(0);

                DateFormat weekFormatter = new SimpleDateFormat("EEEE", l);
                String dayOfWeek = weekFormatter.format(utilityDay);
                DateFormat monthFormatter = new SimpleDateFormat("MMMM", l);
                String month = monthFormatter.format(utilityDay);
                String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
                String year = Integer.toString(c.get(Calendar.YEAR));

                dateHUD.setText(dayOfWeek + ", " + month + " " + day);

                // Add infinite scrolling options
                int lastPos = layoutMgr.findLastVisibleItemPosition();
                if (lastPos == InformationCentral.recyclingDates.size() - 1) {
                    Date first = new Date(Date.from(InformationCentral.recyclingDates.get(lastPos).toInstant().plus(1, ChronoUnit.DAYS)).getTime());
                    Date second = new Date(Date.from(first.toInstant().plus(1, ChronoUnit.DAYS)).getTime());
                    Date third = new Date(Date.from(second.toInstant().plus(1, ChronoUnit.DAYS)).getTime());
                    InformationCentral.recyclingDates.add(first);
                    InformationCentral.recyclingDates.add(second);
                    InformationCentral.recyclingDates.add(third);
                    scrollCalendarView.post(new Runnable() { // This is to postpone view updating til the next frame cause it says so
                        @Override
                        public void run() {
                            adapter.notifyItemChanged(InformationCentral.recyclingDates.size() - 1); // THE MAGIC WORD!!!!!!
                        }
                    });
                }

                // Set the dragListeners necessary to bind events
                View currentDayView = layoutMgr.findViewByPosition(firstPos);
                if (currentDayView.getTag() == null) {
                    currentDayView.setTag("tagged");
                    for (int i = 0; i < 24; i++) {
                        int id1 = getResources().getIdentifier("hour_" + i,
                                "id", getApplicationContext().getPackageName());
                        TextView hourView = currentDayView.findViewById(id1);
                        CharSequence hourRawText = hourView.getText();
                        int hour = 0;

                        if (hourRawText.length() == 8) {
                            String append = hourRawText.subSequence(6, 8).toString();
                            if (append.equals("PM")) {
                                if (hourRawText.subSequence(0, 2).toString().equals("12")){
                                    hour = 12;
                                } else {
                                    hour = Integer.parseInt(hourRawText.subSequence(0, 2).toString()) + 12;
                                }
                            } else if (append.equals("AM")) {
                                if (hourRawText.subSequence(0, 2).toString().equals("12")){
                                    hour = 0;
                                } else {
                                    hour = Integer.parseInt(hourRawText.subSequence(0, 2).toString());
                                }
                            }
                        } else {
                            String append = hourRawText.subSequence(5, 7).toString();
                            if (append.equals("PM")) {
                                hour = Integer.parseInt(String.valueOf(hourRawText.charAt(0))) + 12;
                            } else if (append.equals("AM")) {
                                hour = Integer.parseInt(String.valueOf(hourRawText.charAt(0)));
                            }
                        }

                        final int finalHour = hour;

                        hourView.setOnDragListener(new View.OnDragListener() {

                            @Override
                            public boolean onDrag(View view, DragEvent dragEvent) {
                                if (dragEvent.getAction() == dragEvent.ACTION_DRAG_ENTERED) {
                                    setSideTabStatus("minimized");
                                    hourView.setTextSize(24f);
                                    hourView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.georgia_coral));
                                } else if (dragEvent.getAction() == dragEvent.ACTION_DRAG_EXITED) {
                                    setSideTabStatus("open");
                                    hourView.setTextSize(16f);
                                    hourView.setBackground(null);
                                } else if (dragEvent.getAction() == dragEvent.ACTION_DROP) {
                                    setSideTabStatus("closed");
                                    hourView.setTextSize(16f);
                                    hourView.setBackground(null);
                                    String parsableData = dragEvent.getClipData().getItemAt(0).coerceToText(getApplicationContext()).toString();
                                    System.out.println("ParsableData found from ClipData: " + parsableData);
                                    Scanner s = new Scanner(parsableData).useDelimiter("/");
                                    String name = s.next();
                                    String category = s.next();
                                    int duration = Integer.parseInt(s.next());

                                    View offsetPopupView = inflater.inflate(R.layout.minute_offset_form, null);
                                    int width = RelativeLayout.LayoutParams.MATCH_PARENT;
                                    int height = RelativeLayout.LayoutParams.MATCH_PARENT;
                                    ViewGroup e = (ViewGroup) offsetPopupView;
                                    offsetPopupView.findViewById(R.id.finalize_button).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.burgundy));
                                    boolean f = true;
                                    final PopupWindow offsetFormPopUp = new PopupWindow(offsetPopupView, width, height, f);
                                    offsetFormPopUp.showAtLocation(view, Gravity.CENTER, 0, 0);

                                    SeekBar seekBar = offsetPopupView.findViewById(R.id.seekBar);
                                    TextView seekHUD = offsetPopupView.findViewById(R.id.title_seekbar);
                                    Button finishOffset = offsetPopupView.findViewById(R.id.finalize_button);
                                    String outsideAppend = " PM";
                                    String outsideHourText = Integer.toString(finalHour);

                                    if (finalHour > 12) {
                                        outsideHourText = Integer.toString(finalHour - 12);
                                    } else if (finalHour == 0) {
                                        outsideHourText = "12";
                                    }

                                    if (finalHour < 12) {
                                        outsideAppend = " AM";
                                    }

                                    seekHUD.setText("This event will begin at: " + outsideHourText + ":00 " + outsideAppend);
                                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                        @Override
                                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                            int prog = (int) Math.round(seekBar.getProgress() * 0.59);

                                            String preText = "This event will begin at: ";
                                            String timeText = InformationCentral.returnUserFriendlyTime(finalHour, prog);

                                            seekHUD.setText(preText + timeText);
                                        }

                                        @Override
                                        public void onStartTrackingTouch(SeekBar seekBar) {

                                        }

                                        @Override
                                        public void onStopTrackingTouch(SeekBar seekBar) {

                                        }
                                    });

                                    finishOffset.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            int offset = (int) Math.round(seekBar.getProgress() * 0.59);
                                            System.out.println("Offset ended up being " + Integer.toString(offset));
                                            offsetFormPopUp.dismiss();

                                            // Find the date associated with the day
                                            Date eventDate = new Date(0);
                                            SimpleDateFormat contentTagFormatter = new SimpleDateFormat("yyyy-MM-dd");
                                            try {
                                                java.util.Date d = contentTagFormatter.parse(hourView.getContentDescription().toString());
                                                eventDate = Date.valueOf(contentTagFormatter.format(d));
                                            } catch (ParseException ex) {
                                                ex.printStackTrace();
                                            }

                                            BoundEvent boundEvent = new BoundEvent(duration, name, category, eventDate, finalHour, offset);
                                            System.out.println("minuteOffset.clickListener(): BoundEvent created by name of " + boundEvent.name + " on date " + boundEvent.day.toString());
                                            List<BoundEvent> dayList = InformationCentral.boundEventLog.get(eventDate.toString());
                                            if (dayList == null) {
                                                dayList = new ArrayList<BoundEvent>();
                                            }
                                            dayList.add(boundEvent);
                                            InformationCentral.boundEventLog.put(eventDate.toString(), dayList);
                                            int boundPos = layoutMgr.findFirstVisibleItemPosition();
                                            adapter.refreshDisplay((CalendarAdapter.DayViewHolder) scrollCalendarView.findViewHolderForAdapterPosition(boundPos));
                                            InformationCentral.purgePassedEvents();
                                            InformationCentral.updateUpcomingEvent();
                                            InformationCentral.saveBoundEventLog();
                                            InformationCentral.bindNotificationAlarm(boundEvent, "imminent_events");
                                            InformationCentral.visuallyUpdateUpcomingEventHUD();
                                        }
                                    });
                                }
                                return true;
                            }
                        });
                    }
                }
            }
        });

        // Add functionality to the date jump button
        ImageButton jumpToDateButton = (ImageButton) findViewById(R.id.jump_to_date_button);
        jumpToDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // long myBirthday = 1022050000000L; // May 22, 2002
                // long jameyBirthday = -7200000000L; // October 9, 1969

                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this);
                dialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        String monthString = Integer.toString(month + 1);
                        if (monthString.length() < 2) {
                            monthString = "0" + monthString;
                        }
                        String dayString = Integer.toString(day);
                        if (dayString.length() < 2) {
                            dayString = "0" + dayString;
                        }
                        String dateText = Integer.toString(year) + "-" + monthString + "-" + dayString;
                        System.out.println("DIALOG DEBUG: " + dateText);
                        Date validDate = Date.valueOf(dateText);
                        InformationCentral.jumpToDate(validDate);
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        // Add functionality to the category button
        ImageButton categoryMenu = (ImageButton) findViewById(R.id.category_menu_button);
        categoryMenu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Create popup window for the dialog
                View categoryView = inflater.inflate(R.layout.category_menu, null);
                int width = RelativeLayout.LayoutParams.MATCH_PARENT;
                int height = RelativeLayout.LayoutParams.MATCH_PARENT;
                Button saveCategoryMenuButton = categoryView.findViewById(R.id.save_button);
                saveCategoryMenuButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.burgundy));
                boolean f = true;
                final PopupWindow categoryPopUp = new PopupWindow(categoryView, width, height, f);
                categoryPopUp.showAtLocation(categoryMenu, Gravity.CENTER, 0, 0);

                // Create functionality for the category creation button
                ImageButton categoryCreator = (ImageButton) categoryView.findViewById(R.id.add_category_button);
                categoryCreator.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View categoryCreationView = inflater.inflate(R.layout.new_category_form, null);
                        int width = RelativeLayout.LayoutParams.MATCH_PARENT;
                        int height = RelativeLayout.LayoutParams.MATCH_PARENT;
                        categoryCreationView.findViewById(R.id.create_category_button).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.burgundy));
                        categoryCreationView.findViewById(R.id.change_color_button).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.faerie));
                        boolean f = true;
                        final PopupWindow categoryCreationPopUp = new PopupWindow(categoryCreationView, width, height, f);
                        categoryCreationPopUp.showAtLocation(findViewById(R.id.main_tab), Gravity.CENTER, 0, 0);

                        final Button changeColor = categoryCreationView.findViewById(R.id.change_color_button);
                        final Button createCategoryButton = categoryCreationView.findViewById(R.id.create_category_button);

                        changeColor.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                View colorPicker = inflater.inflate(R.layout.color_picker, null);
                                int width = RelativeLayout.LayoutParams.MATCH_PARENT;
                                int height = RelativeLayout.LayoutParams.MATCH_PARENT;
                                boolean f = true;
                                final PopupWindow colorPickerPopup = new PopupWindow(colorPicker, width, height, f);
                                colorPickerPopup.showAtLocation(categoryMenu, Gravity.CENTER, 0, 0);
                                for (int i = 0; i < ((ViewGroup) colorPicker.findViewById(R.id.color_tray)).getChildCount(); i++) {
                                    final ImageButton colorButton = (ImageButton) ((ViewGroup) colorPicker.findViewById(R.id.color_tray)).getChildAt(i);
                                    clickAndResetColorSwatch(changeColor, colorButton, colorPickerPopup);
                                }
                            }
                        });

                        createCategoryButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                categoryCreationPopUp.dismiss();
                                EditText categoryNameField = (EditText) categoryCreationView.findViewById(R.id.category_box);
                                String name = categoryNameField.getText().toString();
                                InformationCentral.categoryLog.add(name);
                                int color = ContextCompat.getColor(InformationCentral.mainApplicationContext, R.color.pink);
                                try {
                                    System.out.println("CreateCategoryButton onClick(): Color being parsed is " + changeColor.getContentDescription().toString());
                                    color = Color.parseColor(changeColor.getContentDescription().toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                InformationCentral.categoryColorMap.put(name, color);
                                InformationCentral.saveCategories();

                                // Create its view
                                View categoryDisplayBar = inflater.inflate(R.layout.category_menu_bar, categoryView.findViewById(R.id.category_tray), false);
                                categoryDisplayBar.findViewById(R.id.color_swatch).setBackgroundColor(color);
                                ((TextView) categoryDisplayBar.findViewById(R.id.category_name)).setText(name);
                                ((ViewGroup) categoryView.findViewById(R.id.category_tray)).addView(categoryDisplayBar);

                                ImageButton deleteCategory = (ImageButton) categoryDisplayBar.findViewById(R.id.delete_category_button);
                                deleteCategory.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ViewGroup tray = (ViewGroup) categoryView.findViewById(R.id.category_tray);
                                        tray.removeView((View) view.getParent());
                                        String deletedName = ((TextView) ((ViewGroup) view.getParent()).findViewById(R.id.category_name)).getText().toString();
                                        InformationCentral.categoryLog.remove(deletedName);
                                        InformationCentral.categoryColorMap.remove(deletedName);
                                        InformationCentral.saveCategories();
                                    }
                                });
                            }
                        });
                    }
                });

                for (int i = 0; i < InformationCentral.categoryLog.size(); i++) {
                    String cName = InformationCentral.categoryLog.get(i);
                    View categoryDisplayBar = inflater.inflate(R.layout.category_menu_bar, categoryView.findViewById(R.id.category_tray), false);
                    categoryDisplayBar.findViewById(R.id.color_swatch).setBackgroundColor(InformationCentral.categoryColorMap.get(cName));
                    ((TextView) categoryDisplayBar.findViewById(R.id.category_name)).setText(cName);
                    ((ViewGroup) categoryView.findViewById(R.id.category_tray)).addView(categoryDisplayBar);

                    ImageButton deleteCategory = (ImageButton) categoryDisplayBar.findViewById(R.id.delete_category_button);
                    deleteCategory.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ViewGroup tray = (ViewGroup) categoryView.findViewById(R.id.category_tray);
                            tray.removeView((View) view.getParent());
                            String deletedName = ((TextView) ((ViewGroup) view.getParent()).findViewById(R.id.category_name)).getText().toString();
                            InformationCentral.categoryLog.remove(deletedName);
                            InformationCentral.categoryColorMap.remove(deletedName);
                            InformationCentral.saveCategories();
                        }
                    });
                }
                // Save categories

                saveCategoryMenuButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        InformationCentral.saveCategories();
                        categoryPopUp.dismiss();
                    }
                });
            }
        });

        ImageButton expandCollapse = (ImageButton) findViewById(R.id.expand_collapse_button);
        expandCollapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (InformationCentral.upcomingHUDstatus == "expanded") {
                    InformationCentral.upcomingHUDstatus = "collapsed";
                } else if (InformationCentral.upcomingHUDstatus == "collapsed") {
                    InformationCentral.upcomingHUDstatus = "expanded";
                }
                InformationCentral.visuallyUpdateUpcomingEventHUD();
            }
        });
    }

    public void setSideTabStatus(String status) {
        ConstraintLayout mainTab = (ConstraintLayout) findViewById(R.id.main_tab);
        ConstraintLayout sideTab = (ConstraintLayout) findViewById(R.id.side_tab);
        TextView HUD = (TextView) findViewById(R.id.current_date_HUD);
        ImageButton tabButton = (ImageButton) findViewById(R.id.side_tab_button);
        ConstraintLayout.LayoutParams mainP = (ConstraintLayout.LayoutParams) mainTab.getLayoutParams();
        ConstraintLayout.LayoutParams sideP = (ConstraintLayout.LayoutParams) sideTab.getLayoutParams();

        switch (status) {
            case "open": // We set both widths to 0dp and let the constraints take over
                mainP.width = 0;
                sideP.width = 0;
                HUD.setVisibility(View.INVISIBLE);
                sideTabStatus = "open";
                tabButton.setImageResource(R.drawable.close_tab_button);
                break;
            case "closed":
                mainP.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
                sideP.width = 0;
                HUD.setVisibility(View.VISIBLE);
                sideTabStatus = "closed";
                tabButton.setImageResource(R.drawable.open_tab_button);
                break;
            case "minimized":
                mainP.width = 0;
                sideP.width = 75;
                HUD.setVisibility(View.INVISIBLE);
                sideTabStatus = "minimized";
                break;
        }

        mainTab.setLayoutParams(mainP);
        sideTab.setLayoutParams(sideP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        InformationCentral.purgePassedEvents();
        InformationCentral.updateUpcomingEvent();
        InformationCentral.visuallyUpdateUpcomingEventHUD();
    }

    public void refreshSideTab() {
        loadFloatingEvents(); // Make sure the events match the events on file before we display them
        LinearLayout eventPanel = (LinearLayout) findViewById(R.id.events_panel); // Get a reference to where we show the events

        eventPanel.removeAllViews(); // Remove all the current stuff in the panel

        for (int i = 0; i < floatingEvents.size(); i++) { // Loop through all of the events
            FloatingEvent event = floatingEvents.get(i); // Get the current event in the loop
            int current = i; // Store a reference to the current loop number

            TextView reorderSensor = new TextView(eventPanel.getContext()); // Create a 5px tall barrier that allows us to drag and reorder the events
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 5);
            reorderSensor.setLayoutParams(p); // Set the parameters of the barrier
            reorderSensor.setBackground(null); // Make sure the barrier is invisible
            reorderSensor.setOnDragListener(new View.OnDragListener() { // Create a listener that notices when we drag an event over the barrier

                @Override
                public boolean onDrag(View view, DragEvent dragEvent) {
                    if (dragEvent.getAction() == dragEvent.ACTION_DRAG_ENTERED) { // If an event goes into the barrier:
                        // The barrier opens up and becomes pink
                        p.height = 100;
                        reorderSensor.setLayoutParams(p);
                        reorderSensor.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.pink));
                    } else if (dragEvent.getAction() == dragEvent.ACTION_DRAG_EXITED) { // If an event leaves the barrier:
                        // The barrier closes back to 5px tall and becomes invisible
                        p.height = 5;
                        reorderSensor.setLayoutParams(p);
                        reorderSensor.setBackground(null);
                    } else if (dragEvent.getAction() == dragEvent.ACTION_DROP) { // If we drop an event into the barrier:
                        p.height = 5;
                        reorderSensor.setLayoutParams(p);
                        reorderSensor.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.wan_pink));
                        ClipData data = dragEvent.getClipData();
                        Scanner s = new Scanner(data.getItemAt(0).coerceToText(getApplicationContext()).toString()).useDelimiter("/");
                        String eventName = s.next();
                        System.out.println("ReorderSensor DEBUG - eventName = " + eventName);
                        FloatingEvent droppedEvent = floatingEvents.get(0);
                        int pos = 0;
                        for (int i = 0; i < floatingEvents.size(); i++) {
                            String targetName = floatingEvents.get(i).name;
                            if (targetName.equals(eventName)) {
                                droppedEvent = floatingEvents.get(i);
                                System.out.println("ReorderSensor DEBUG - event found at position " + Integer.toString(i));
                                pos = i;
                                break;
                            }
                        }
                        FloatingEvent cloneEvent = new FloatingEvent(droppedEvent.duration, droppedEvent.name, droppedEvent.category);
                        floatingEvents.add(current, cloneEvent);
                        floatingEvents.remove(droppedEvent);
                        saveFloatingEvents();
                        refreshSideTab();
                    }
                    return true;
                }
            });
            eventPanel.addView(reorderSensor);

            View eventView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.event_template, eventPanel, false);

            TextView name = eventView.findViewById(R.id.event_name);
            TextView category = eventView.findViewById(R.id.event_category);
            TextView duration = eventView.findViewById(R.id.event_duration);

            if (InformationCentral.categoryColorMap.get(event.category) == null) {
                System.out.println("Color not found in map: ");
                System.out.println(InformationCentral.categoryColorMap.get(event.category));
            } else {
                eventView.setBackgroundColor(InformationCentral.categoryColorMap.get(event.category));
            }

            name.setText(event.name);
            category.setText(event.category);

            int parsedDuration = 0;
            if (event.duration >= 60) {
                int hour = event.duration / 60;
                int minute = event.duration % 60;
                if (minute == 0) {
                    duration.setText(Integer.toString(hour) + " hr ");
                } else {
                    duration.setText(Integer.toString(hour) + " hr " + Integer.toString(minute) + " min");
                }
            } else {
                parsedDuration = event.duration;
                duration.setText(Integer.toString(parsedDuration) + " min");
            }

            ImageButton deleteButton = eventView.findViewById(R.id.delete_button);

            RelativeLayout.LayoutParams newParameter = (RelativeLayout.LayoutParams) deleteButton.getLayoutParams();
            newParameter.removeRule(RelativeLayout.START_OF);
            newParameter.addRule(RelativeLayout.ALIGN_PARENT_END);
            deleteButton.setLayoutParams(newParameter);

            RelativeLayout.LayoutParams newTitleParameter = (RelativeLayout.LayoutParams) name.getLayoutParams();
            newTitleParameter.addRule(RelativeLayout.START_OF, R.id.delete_button);
            name.setLayoutParams(newTitleParameter);

            deleteButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    floatingEvents.remove(event);
                    saveFloatingEvents();
                    refreshSideTab();
                }
            });

            eventView.findViewById(R.id.notif_button).setVisibility(View.GONE);
            eventView.findViewById(R.id.settings_button).setVisibility(View.GONE);
            eventView.findViewById(R.id.edit_button).setVisibility(View.GONE);

            eventPanel.addView(eventView); // Adds the above event to the panel
            makeDraggable(eventView, event); // Adds draggability to the event
        }

        TextView buffer = new TextView(eventPanel.getContext());
        buffer.setBackground(null);
        LinearLayout.LayoutParams bufferP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150);
        buffer.setLayoutParams(bufferP);
        eventPanel.addView(buffer);
    }

    public void saveFloatingEvents() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("floatingEventLog", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("number_of_events", floatingEvents.size()); // This value will tell us how many events are saved

        for (int i = 0; i < floatingEvents.size(); i++) {
            FloatingEvent event = floatingEvents.get(i);
            String parsableEventData = (event.name + "/" + event.category + "/" + Integer.toString(event.duration));
            editor.putString(Integer.toString(i), parsableEventData);
            editor.apply();
        }
    }

    public void loadFloatingEvents() {
        List<FloatingEvent> temporarilyLoadedEvents = new ArrayList<FloatingEvent>(); // Creates a place to put the loaded events without contaminating the global list
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("floatingEventLog", Context.MODE_PRIVATE);

        int num = sharedPref.getInt("number_of_events", -1);
        if (num == -1) {
            System.out.println("No events found.");
        } else {
            for (int i = 0; i < num; i++) { // If there ARE events, this loops through them
                String parsable = sharedPref.getString(Integer.toString(i), "blank");
                Scanner s = new Scanner(parsable).useDelimiter("/");
                FloatingEvent resurrectedEvent = new FloatingEvent(45, "placeholder", "placeholder");
                resurrectedEvent.name = s.next();
                resurrectedEvent.category = s.next();
                resurrectedEvent.duration = Integer.parseInt(s.next());
                temporarilyLoadedEvents.add(resurrectedEvent);
            }
        }

        floatingEvents = temporarilyLoadedEvents;
    }

    public void makeDraggable(View eventView, FloatingEvent correspondent) {
        eventView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {

                String parsableData = (correspondent.name + "/" + correspondent.category + "/" + Integer.toString(correspondent.duration));
                ClipData.Item clip_name = new ClipData.Item(parsableData);
                String[] mime_type = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData data = new ClipData("FloatingEvent", mime_type, clip_name);

                View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(eventView);

                eventView.startDragAndDrop(data, dragshadow, eventView, 0);
                return true;
            }
        });
    }

    public void clickAndResetColorSwatch(View colorSwatch, ImageButton clickButton, PopupWindow window) {
        clickButton.setBackgroundColor(Color.parseColor(clickButton.getContentDescription().toString()));
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colorSwatch.setBackgroundColor(Color.parseColor(clickButton.getContentDescription().toString()));
                colorSwatch.setContentDescription(clickButton.getContentDescription().toString());
                window.dismiss();
            }
        });
    }

    private void createNotificationChannel() {
        String channelID = "imminent_events";
        String channelName = "Imminent events";
        String channelDesc = "Events upcoming in an hour or less";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(channelID, channelName, importance);
        channel.setDescription(channelDesc);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}