package com.helenpahno.infinitecalendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public abstract class InformationCentral {
    public static Context mainApplicationContext;
    public static Locale l;
    public static AlarmManager alarmManager;
    public static View upcomingEventHUD;

    public static CalendarAdapter runtimeAdapter;
    public static RecyclerView runtimeRecyclerView;

    public static String upcomingHUDstatus;

    public static HashMap<String, List<BoundEvent>> boundEventLog = new HashMap<String, List<BoundEvent>>();
    public static List<FloatingEvent> floatingEventLog = new ArrayList<FloatingEvent>();
    public static List<String> categoryLog = new ArrayList<String>();
    public static HashMap<String, Integer> categoryColorMap = new HashMap<String, Integer>();

    public static List<Date> recyclingDates = new ArrayList<Date>();

    public static BoundEvent nextUpcomingEvent;

    public static boolean eventPurgeCompletedSafely = false;

    public static List<BoundEvent> getBoundEventListFromDate(Date date) {
        List<BoundEvent> list;
        String dateKey = date.toString();
        list = boundEventLog.get(dateKey);

        return list;
    }

    public static void appendBoundEventToDate(Date date, BoundEvent boundEvent) {
        String dateKey = date.toString();
        List<BoundEvent> list = new ArrayList<BoundEvent>();

        if (boundEventLog.get(dateKey) != null) {
            list = boundEventLog.get(dateKey);
        }

        list.add(boundEvent);
        boundEventLog.put(dateKey, list);
    }

    public static void jumpToDate(Date date) {
        // Clear the runtime dates
        // Add a runtime date, one past, and one future
        recyclingDates.clear();

        Date dateMorrow = new Date(Date.from(date.toInstant().plus(1, ChronoUnit.DAYS)).getTime());

        recyclingDates.add(date);
        recyclingDates.add(dateMorrow);

        runtimeAdapter.notifyItemChanged(0);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runtimeRecyclerView.smoothScrollBy(0, 1);
            }
        }, 200);
    }

    public static void saveBoundEventLog() {
        SharedPreferences sharedPref = mainApplicationContext.getSharedPreferences("boundEventLog", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        int overarch = 0;

        if (boundEventLog.size() != 0) {
            for (List<BoundEvent> eventList : boundEventLog.values()) {
                for (int i = 0; i < eventList.size(); i++) {
                    BoundEvent event = eventList.get(i);
                    String notifEnabled = "true";
                    if (event.notificationsEnabled == false) {
                        notifEnabled = "false";
                    } else {
                        notifEnabled = "true";
                    }
                    String parsableData = (event.name + "/" + event.category + "/" + Integer.toString(event.duration) + "/" + event.day.toString() + "/" + Integer.toString(event.hour) + "/" + Integer.toString(event.minuteOffset) + "/" + notifEnabled); // Create some parsable data here
                    editor.putString(Integer.toString(overarch), parsableData);
                    System.out.println("saveBoundEventLog(): System event saved by name of " + event.name + " on date " + event.day.toString());
                    overarch++;
                }
            }
        } else {
            overarch = -1;
        }

        editor.putInt("numerator", overarch);
        editor.apply();
    }

    public static void loadBoundEventLog() {
        HashMap<String, List<BoundEvent>> tempEventLog = new HashMap<String, List<BoundEvent>>();
        SharedPreferences sharedPref = mainApplicationContext.getSharedPreferences("boundEventLog", Context.MODE_PRIVATE);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        int overarchingQuantity = sharedPref.getInt("numerator", -1);

        if (overarchingQuantity == -1) {
            System.out.println("No bound events found.");
            tempEventLog = null;
        } else {
            List<BoundEvent> temp = new ArrayList<BoundEvent>();
            Date persistentDate = null;
            for (int i = 0; i < overarchingQuantity; i++) {
                String loadedParseData = sharedPref.getString(Integer.toString(i), "nil");
                if (loadedParseData != "nil") {
                    Scanner s = new Scanner(loadedParseData).useDelimiter("/"); // Create a scanner to pick through the SharedPreferences string with all the slashes in it
                    String name = s.next(); // Find the event's name from the parsable string
                    String category = s.next(); // Find the event's category from the parsable string
                    int duration = Integer.parseInt(s.next()); // Find, and convert, the event's duration to int from the parsable string
                    Date day = new Date(0); // Create a default date value, UNIX time Dec. 31 1969, to use as the fallback date if the date parser errors out
                    try {
                        String dateParseRequest = s.next();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                        java.util.Date d = format.parse(dateParseRequest); // Get a util date first because Java date parsers suck
                        day = Date.valueOf(formatter.format(d));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int hour = Integer.parseInt(s.next());
                    int minuteOffset = Integer.parseInt(s.next());
                    String notifEnabled = "false";
                    try {
                        notifEnabled = s.next();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    BoundEvent loadedEvent = new BoundEvent(duration, name, category, day, hour, minuteOffset); // Take all the data and make an event with it
                    // DEBUG
                    System.out.println("Method loadBoundEventLog(): BoundEvent loaded by name of " + loadedEvent.name + " on date " + loadedEvent.day.toString());

                    if (notifEnabled.equals("false")) {
                        loadedEvent.notificationsEnabled = false;
                    } else {
                        loadedEvent.notificationsEnabled = true;
                    }

                    if (tempEventLog.containsKey(day.toString())) {
                        List<BoundEvent> overrideList = tempEventLog.get(day.toString()); // Get the right list in the HashMap
                        overrideList.add(loadedEvent);
                    } else {
                        List<BoundEvent> createdList = new ArrayList<BoundEvent>();
                        createdList.add(loadedEvent);
                        tempEventLog.put(day.toString(), createdList);
                    }
                } else {
                    System.out.println("BoundEvent declared null from file at position " + Integer.toString(i));
                }
            }
        }

        boundEventLog = tempEventLog;
        purgePassedEvents();
        updateUpcomingEvent();
    }

    public static void deleteBoundEvent(BoundEvent discardedEvent, Date fromDate, CalendarAdapter.DayViewHolder callHolder) {
        rescindNotificationAlarm(discardedEvent, "imminent_events");
        List<BoundEvent> list = boundEventLog.get(fromDate.toString());
        if (list != null && list.contains(discardedEvent)) {
            list.remove(discardedEvent);
            boundEventLog.put(fromDate.toString(), list);
        }

        if (list != null && list.size() == 0) {
            boundEventLog.remove(fromDate.toString());
        }
        saveBoundEventLog();
        updateUpcomingEvent();
        visuallyUpdateUpcomingEventHUD();
    }

    public static void saveCategories() {
        SharedPreferences sharedPref = mainApplicationContext.getSharedPreferences("categoryLog", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("numerator", categoryLog.size());

        for (int i = 0; i < categoryLog.size(); i++) {
            String category = categoryLog.get(i);
            editor.putString(Integer.toString(i), category); // Save the category

            int assocColor = categoryColorMap.get(category);
            editor.putInt(category, assocColor);
        }
        editor.apply();
    }

    public static void loadCategories() {
        SharedPreferences sharedPref = mainApplicationContext.getSharedPreferences("categoryLog", Context.MODE_PRIVATE);
        int numerator = sharedPref.getInt("numerator", -1);

        if (numerator == -1) {
            System.out.println("No saved categories found.");
        } else {
            for (int i = 0; i < numerator; i++) {
                String category = sharedPref.getString(Integer.toString(i), "null");
                categoryLog.add(category);
                categoryColorMap.put(category, sharedPref.getInt(category, Color.parseColor("#FF000000")));
            }
        }
    }

    public static void purgePassedEvents() {
        if (boundEventLog == null) {
            System.out.println("purgePassedEvents(): NEW boundEventLog created");
            boundEventLog = new HashMap<String, List<BoundEvent>>();
        }
        if (boundEventLog.size() != 0) {
            boolean modified = false;
            while (modified == false) {
                try {
                    for (String key : boundEventLog.keySet()) {
                        Calendar c = Calendar.getInstance();
                        Date givenDate = Date.valueOf(key);
                        Date today = new Date(c.getTimeInMillis());
                        List<BoundEvent> eventByDate = boundEventLog.get(key);
                        List<BoundEvent> purgedEvents = new ArrayList<BoundEvent>();

                        // Loop through the list and determine which events have passed or not, by hour
                        for (int i = 0; i < eventByDate.size(); i++) {
                            BoundEvent event = eventByDate.get(i);
                            long eventMillis = givenDate.getTime() + (event.hour * 3600000) + (event.minuteOffset * 60000) + (event.duration * 60000);

                            if (eventMillis < today.getTime()) {
                                purgedEvents.add(event);
                            }
                        }

                        for (int i = 0; i < purgedEvents.size(); i++) {
                            eventByDate.remove(purgedEvents.get(i));
                        }

                        if (eventByDate.size() == 0) { // If no more events are left after all passed events have been purged...
                            boundEventLog.remove(givenDate.toString()); // ... remove the date from the HashMap of days containing events.
                        }
                    }
                    modified = true;
                } catch (Exception e) {
                    System.out.println("ConcurrentMod exception.... waiting.... ");
                    try {
                        Thread.sleep(10);
                    } catch (Exception ex) {
                        System.out.println("Modification exception..... fuck");
                    }
                }
            }
        }

        saveBoundEventLog();
    }

    public static void updateUpcomingEvent() {
        if (boundEventLog == null) {
            System.out.println("updateUpcomingEvent(): NEW boundEventLog created... probably for the second time");
            boundEventLog = new HashMap<String, List<BoundEvent>>();
        }
        Calendar c = Calendar.getInstance(); // Instantiate a new calendar
        Date today = new Date(c.getTimeInMillis()); // Use the calendar to get today's date
        Date UNIXepochBegins = new Date(0);
        Date mostImmediateDate = new Date(0); // Set a variable for the most immediate date
        System.out.println(boundEventLog);
        for (String dateKey : boundEventLog.keySet()) { // Iterate through all dates on which we have events
            Date date = Date.valueOf(dateKey);
            System.out.println("updateUpcomingEvent(): Day of " + dateKey + " being scrutinized");
            if (mostImmediateDate.getTime() == UNIXepochBegins.getTime()) {
                mostImmediateDate = date;
                System.out.println("updateUpcomingEvent(): " + dateKey + " replaced the UNIX epoch default");
            } else if (mostImmediateDate.getTime() > date.getTime()) {
                System.out.println("updateUpcomingEvent(): " + dateKey + " replaced " + mostImmediateDate.toString());
                mostImmediateDate = date;
            }
        }
        int earliestHourInDate = 24;
        BoundEvent tempUpcomingEvent = new BoundEvent(10, "placeholder", "placeholder", UNIXepochBegins, 0, 99);
        if (mostImmediateDate.getTime() - UNIXepochBegins.getTime() != 0) {
            for (int i = 0; i < boundEventLog.get(mostImmediateDate.toString()).size(); i++) {
                BoundEvent eventInQuestion = boundEventLog.get(mostImmediateDate.toString()).get(i);
                long eventStartMillis = eventInQuestion.day.getTime() + (eventInQuestion.hour * 3600000) + (eventInQuestion.minuteOffset * 60000);
                long eventEndMillis = eventStartMillis + (eventInQuestion.duration * 60000);
                System.out.println("updateUpcomingEvent(): " + eventInQuestion.name + " being checked for hour");
                if (eventStartMillis < today.getTime() && eventEndMillis > today.getTime()) {
                    System.out.println("Event found to be going on right now; skipping to next");
                } else {
                    if (eventInQuestion.hour < earliestHourInDate) {
                        System.out.println("updateUpcomingEvent(): " + eventInQuestion.name + " replaced the previous hour " + Integer.toString(earliestHourInDate));
                        earliestHourInDate = eventInQuestion.hour;
                        tempUpcomingEvent = eventInQuestion;
                    }
                }

            }
        } else {
            System.out.println("updateUpcomingEvent(): There was no mostImmediateDate. Default to UNIX epoch persisted. No events found, or else, we fucked everything.");
        }

        nextUpcomingEvent = tempUpcomingEvent;
    }

    public static String returnUserFriendlyDurationFromEvent(BoundEvent event) {
        String userFriendlyDuration = "";

        String firstTime = "";
        String firstAppend = "";
        String secondTime = "";
        String secondAppend = "";
        int rawBeginTime;
        int rawEndTime;

        if (event.hour > 12) { // Set the hour
            firstTime = firstTime + Integer.toString(event.hour - 12);
            firstAppend = "PM";
        } else {
            if (event.hour == 0) {
                firstTime = firstTime + "12";
            } else {
                firstTime = firstTime + Integer.toString(event.hour);
            }
            firstAppend = "AM";
        }

        if (Integer.toString(event.minuteOffset).length() < 2) {
            firstTime = firstTime + ":0" + Integer.toString(event.minuteOffset);
        } else {
            firstTime = firstTime + ":" + Integer.toString(event.minuteOffset);
        }

        firstTime = firstTime + " " + firstAppend;
        rawBeginTime = (event.hour * 60) + event.minuteOffset;
        rawEndTime = rawBeginTime + event.duration;

        if (rawEndTime >= (13 * 60)) {
            secondTime = secondTime + Integer.toString((int) Math.floor(rawEndTime / 60) - 12);
            secondAppend = "PM";
        } else {
            if (((int) rawEndTime / 60) == 0) {
                secondTime = secondTime + "12";
            } else {
                secondTime = secondTime + Integer.toString((int) Math.floor(rawEndTime / 60));
            }
            secondAppend = "AM";
        }

        if (Integer.toString(rawEndTime % 60).length() < 2) {
            secondTime = secondTime + ":0" + Integer.toString(rawEndTime % 60);
        } else {
            secondTime = secondTime + ":" + Integer.toString(rawEndTime % 60);
        }

        secondTime = secondTime + " " + secondAppend;

        if (rawEndTime > (24 * 60)) {
            System.out.println("This event stretches across the midnight boundary - handle it for us, from the Walmart overnight crew.");
        }

        userFriendlyDuration = firstTime + " - " + secondTime;

        return userFriendlyDuration;
    }

    public static void editBoundEventAttribute(BoundEvent event, String attributeType, String overwrite) {
        // When called, this function changes the name, category, or duration of the passed BoundEvent.

        Date eventDay = event.day;
        List<BoundEvent> eventList = boundEventLog.get(eventDay.toString());
        BoundEvent cachedEvent = null;
        for (int i = 0; i < eventList.size(); i++) {
            if (eventList.get(i).name.equals(event.name)) {
                cachedEvent = eventList.get(i);
            }
        }

        switch (attributeType) {
            case "name":
                cachedEvent.name = overwrite;
                break;
            case "category":
                cachedEvent.category = overwrite;
                break;
            case "duration":
                try {
                    cachedEvent.duration = Integer.parseInt(overwrite);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("editBoundEventAttribute(): Number given was not a valid integer. Change could not be completed.");
                }
                break;
        }

        saveBoundEventLog();
    }

    public static void editFloatingEventAttribute(FloatingEvent event, String attributeType, String overwrite) {
        // When called, this function changes the name, category, or duration of the passed FloatingEvent.

    }

    public static void sendNotificationForEvent(String channelID, String uName, String uTime, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(mainApplicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String notifName = "Upcoming event: " + uName;
        String notifText = "Happening at " + uTime + ".";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mainApplicationContext, channelID);
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setContentTitle(notifName);
        builder.setContentText(notifText);
        builder.setContentIntent(pendingIntent);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        System.out.println("sendNotificationForEvent(): Builder working as intended");

        NotificationManagerCompat NMC = NotificationManagerCompat.from(mainApplicationContext);

        // LOSER code that Android Studio FORCED me to paste in
        if (ActivityCompat.checkSelfPermission(mainApplicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // Back to you, Chet
        NMC.notify(0, builder.build());
    }

    public static void bindNotificationAlarm (BoundEvent event, String channel) {
        Intent i = new Intent(mainApplicationContext, EventNotificationReceiver.class);
        String eventData = event.name + "/" + Integer.toString(event.hour) + "/" + Integer.toString(event.minuteOffset);
        i.putExtra("CHANNEL_ID", channel);
        i.putExtra("EVENT_DATA", eventData);
        PendingIntent p = PendingIntent.getBroadcast(mainApplicationContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        long fireTime = 0;
        long eventDay = event.day.getTime();
        fireTime = eventDay + (event.hour * 3600000) + (event.minuteOffset * 60000) - (event.alarmBuffer * 60000);

        Calendar c = Calendar.getInstance();
        long g = c.getTimeInMillis();

        System.out.println("bindNotificationAlarm(): Alarm bound for " + event.name + " at millisecond time " + Long.toString(fireTime) + "\nCurrent time in milliseconds: " + Long.toString(g));
        alarmManager.set(AlarmManager.RTC_WAKEUP, fireTime, p);
    }

    public static void rescindNotificationAlarm (BoundEvent event, String channel) {
        Intent intent = new Intent(InformationCentral.mainApplicationContext, EventNotificationReceiver.class);
        String eventData = event.name + "/" + Integer.toString(event.hour) + "/" + Integer.toString(event.minuteOffset);
        intent.putExtra("CHANNEL_ID", "imminent_events");
        intent.putExtra("EVENT_DATA", eventData);
        PendingIntent p = PendingIntent.getBroadcast(InformationCentral.mainApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(p);
    }
    
    public static void visuallyUpdateUpcomingEventHUD() {
        TextView eventTitle = (TextView) upcomingEventHUD.findViewById(R.id.upcoming_event_title);
        TextView eventDuration = (TextView) upcomingEventHUD.findViewById(R.id.upcoming_event_duration);
        TextView eventDate = (TextView) upcomingEventHUD.findViewById(R.id.upcoming_event_date);
        TextView topText = (TextView) upcomingEventHUD.findViewById(R.id.upcoming_text);
        ImageButton expandCollapseButton = (ImageButton) upcomingEventHUD.findViewById(R.id.expand_collapse_button);
        RelativeLayout.LayoutParams buttonParams = (RelativeLayout.LayoutParams) expandCollapseButton.getLayoutParams();

        BoundEvent upcoming = InformationCentral.nextUpcomingEvent;

        try {
            upcomingEventHUD.setBackgroundColor(InformationCentral.categoryColorMap.get(upcoming.category));
        } catch (Exception e) {
            e.printStackTrace();
            // Color will take care of itself
        }

        Calendar c = Calendar.getInstance();
        java.util.Date utilityDay = new java.util.Date(upcoming.day.getTime());
        c.setTime(utilityDay);

        SimpleDateFormat weekFormatter = new SimpleDateFormat("EEEE", InformationCentral.l);
        SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM", InformationCentral.l);

        if (InformationCentral.upcomingHUDstatus == "expanded") {
            expandCollapseButton.setImageResource(R.drawable.collapse_view_button);
            expandCollapseButton.setPadding(10, 10, 10, 10);
            buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            buttonParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);

            String dayOfWeek = weekFormatter.format(utilityDay);
            String month = monthFormatter.format(utilityDay);
            String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
            String userFriendlyDay = dayOfWeek + ", " + month + " " + day;

            eventTitle.setVisibility(View.VISIBLE);
            eventDuration.setVisibility(View.VISIBLE);
            eventDate.setVisibility(View.VISIBLE);
            eventTitle.setText(upcoming.name + " (" + upcoming.category + ")");
            eventDuration.setText(InformationCentral.returnUserFriendlyDurationFromEvent(upcoming));
            eventDate.setText(userFriendlyDay);
            topText.setText("Next upcoming event:");
        } else if (InformationCentral.upcomingHUDstatus == "collapsed") {
            expandCollapseButton.setImageResource(R.drawable.expand_view_button);
            expandCollapseButton.setPadding(0, 0, 0, 0);
            buttonParams.removeRule(RelativeLayout.CENTER_HORIZONTAL);
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

            String month = monthFormatter.format(utilityDay);
            String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
            eventTitle.setText("");
            eventTitle.setVisibility(View.GONE);
            eventDuration.setText("");
            eventDuration.setVisibility(View.GONE);
            eventDate.setText("");
            eventDate.setVisibility(View.GONE);
            topText.setText("Next event: " + upcoming.name + " (" + month + " " + day + ")");
        }

        expandCollapseButton.setLayoutParams(buttonParams);
    }
}