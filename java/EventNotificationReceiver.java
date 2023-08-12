package com.helenpahno.infinitecalendar;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Scanner;

public class EventNotificationReceiver extends BroadcastReceiver {

    public static String notificationChannelID = "placeholder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Broadcast received");

        String channelID = intent.getStringExtra("CHANNEL_ID");
        String parsableData = intent.getStringExtra("EVENT_DATA");
        Scanner s = new Scanner(parsableData).useDelimiter("/");
        String eventName = s.next();
        String eventHour = s.next();
        String eventMinute = s.next();

        int hour = Integer.parseInt(eventHour);
        int minute = Integer.parseInt(eventMinute);

        if (minute < 10) {
            eventMinute = "0" + eventMinute;
        }

        String stringTime = "99:00 PM"; // Placeholder value; if it shows to the end-user, something goofed up

        if (hour == 0) { // If the hour is 12 midnight
            stringTime = "12:" + eventMinute + " AM";
        } else if (hour == 12) { // If the hour is 12 noon
            stringTime = "12:" + eventMinute + " PM";
        } else if (hour < 12 && hour != 0) { // If the hour is before noon and after midnight
            stringTime = Integer.toString(hour) + ":" + eventMinute + " AM";
        } else if (hour > 12) { // If the hour is after noon
            stringTime = Integer.toString(hour - 12) + ":" + eventMinute + " PM";
        }

        InformationCentral.sendNotificationForEvent(channelID, eventName, stringTime, intent);
    }
}
