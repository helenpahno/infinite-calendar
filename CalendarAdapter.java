package com.helenpahno.infinitecalendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder>{

    Context context;
    public LinearLayoutManager manager;

    ViewGroup parent1;
    float density;
    int pixels;
    int screenWidth;
    public CalendarAdapter(Context c, LinearLayoutManager lm) {
        manager = lm;
        context = c;
    }

    @Override
    public DayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.day_view, parent, false);

        parent1 = parent; // sorry i know this looks dumb i just got tired of innovating

        screenWidth = parent.getContext().getResources().getDisplayMetrics().widthPixels;
        density = parent.getContext().getResources().getDisplayMetrics().density;
        pixels = (int) (2880 * density + 0.5f);

        for (int i = 0; i < 24; i++) {
            int id1 = parent.getResources().getIdentifier("hour_" + i,
                    "id", parent.getContext().getPackageName());
            TextView hourView = v.findViewById(id1);
            hourView.getLayoutParams().height = pixels/24;
            hourView.setTextSize(16f);
            hourView.setTextColor(ContextCompat.getColor(context, R.color.burgundy));
        }

        DayViewHolder vh = new DayViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(DayViewHolder holder, int pos) {
        Date relevantDay = InformationCentral.recyclingDates.get(pos);
        holder.setDay(relevantDay);
        holder.dayLayout.setContentDescription(relevantDay.toString());
        System.out.println("Below: the particular date-associated BoundEvent(s) for " + relevantDay.toString());
        System.out.println(InformationCentral.boundEventLog.get(relevantDay.toString()));

        for (int i = 0; i < 24; i++) {
            int id1 = parent1.getResources().getIdentifier("hour_" + i,
                    "id", parent1.getContext().getPackageName());
            TextView hourView = holder.dayLayout.findViewById(id1);
            hourView.getLayoutParams().height = pixels/24;
            hourView.setContentDescription(relevantDay.toString());
        }

        refreshDisplay(holder);
    }

    @Override
    public int getItemCount() {
        return InformationCentral.recyclingDates.size();
    }

    public class DayViewHolder extends RecyclerView.ViewHolder {
        View dayLayout;
        Date dayAssociated;

        public DayViewHolder(View itemView) {
            super(itemView);
            dayLayout = itemView;
        }

        public Date getDay() { return dayAssociated; }

        public void setDay(Date day) {
            dayAssociated = day;
        }
    }

    public void refreshDisplay (DayViewHolder holder) {
        Date date = holder.getDay();
        ViewGroup dayView = (ViewGroup) holder.dayLayout;
        ViewGroup eventSlot = dayView.findViewById(R.id.event_slot);

        eventSlot.removeAllViews();

        // 1: Get all events associated with a particular date, in the form of a List<BoundEvent>
        List<BoundEvent> allAssociatedEvents = InformationCentral.getBoundEventListFromDate(date);

        // 2: Loop through the list
        if (allAssociatedEvents != null && allAssociatedEvents.size() > 0) {
            for (int i = 0; i < allAssociatedEvents.size(); i++) {
                BoundEvent eventCode = allAssociatedEvents.get(i);

                View eventView = LayoutInflater.from(dayView.getContext()).inflate(R.layout.event_template, dayView, false);
                TextView title = eventView.findViewById(R.id.event_name);
                TextView category = eventView.findViewById(R.id.event_category);
                TextView durationStretch = eventView.findViewById(R.id.event_duration);

                title.setText(eventCode.name);
                category.setText(eventCode.category);

                String durationString = "";

                durationString = InformationCentral.returnUserFriendlyDurationFromEvent(eventCode);
                durationStretch.setText(durationString);

                eventSlot.addView(eventView);

                int hourMarginOffset = (int) ((pixels/24) * eventCode.hour);
                int minuteMarginOffset = (int) ((pixels/24/60) * eventCode.minuteOffset);
                int topMargin = hourMarginOffset + minuteMarginOffset;

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) eventView.getLayoutParams();
                params.width = (int) (screenWidth * 0.7);
                params.height = (int) (eventCode.duration * 2 * density + 0.5f);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(0, topMargin, 0, 0);
                eventView.setLayoutParams(params);

                try {
                    eventView.setBackgroundColor(InformationCentral.categoryColorMap.get(eventCode.category));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ImageButton deleteButton = (ImageButton) eventView.findViewById(R.id.delete_button);
                deleteButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        deleteEvent(eventCode, date, holder);
                        eventSlot.removeView(eventView);
                    }
                });
            }
        }
    }

    public void deleteEvent (BoundEvent discardedEvent, Date fromDate, DayViewHolder callHolder) {
        List<BoundEvent> list = InformationCentral.boundEventLog.get(fromDate.toString());
        if (list != null && list.contains(discardedEvent)) {
            list.remove(discardedEvent);
            InformationCentral.boundEventLog.put(fromDate.toString(), list);
        }
        InformationCentral.saveBoundEventLog();
        refreshDisplay(callHolder);
    }
}
