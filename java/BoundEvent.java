package com.helenpahno.infinitecalendar;

import java.sql.Date;

public class BoundEvent extends FloatingEvent {
    public java.sql.Date day;
    public int hour;
    public int minuteOffset;

    public BoundEvent(int newDur, String newName, String newCat, java.sql.Date newDay, int newHour, int min) {
        super(newDur, newName, newCat);
        day = newDay;
        hour = newHour;
        minuteOffset = min;
    }
}
