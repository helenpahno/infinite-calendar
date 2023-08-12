package com.helenpahno.infinitecalendar;

import java.sql.Date;

public class BoundEvent extends FloatingEvent {
    public java.sql.Date day;
    public int hour;
    public int minuteOffset;
    public int alarmBuffer = 60; // This is the length of time, in minutes, before the event that it will notify you
    public boolean notificationsEnabled = true; // Whether or not notifications are on; notifications on by default

    public BoundEvent(int newDur, String newName, String newCat, java.sql.Date newDay, int newHour, int min) {
        super(newDur, newName, newCat);
        day = newDay;
        hour = newHour;
        minuteOffset = min;
    }
}
