package com.helenpahno.infinitecalendar;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

        TextView dateMarker = (TextView) holder.dayLayout.findViewById(R.id.date_demarcation);
        SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM", InformationCentral.l);
        SimpleDateFormat yearFormatter = new SimpleDateFormat("YYYY", InformationCentral.l);

        Calendar c = Calendar.getInstance();
        java.util.Date utilityDay = new java.util.Date(relevantDay.getTime());
        c.setTime(utilityDay);

        String month = monthFormatter.format(utilityDay);
        String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH) + 1);
        String year = yearFormatter.format(utilityDay);

        String userFriendlyCurrentDay = month + " " + day + ", " + year;
        dateMarker.setText(userFriendlyCurrentDay);

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
                        InformationCentral.deleteBoundEvent(eventCode, date, holder);
                        eventSlot.removeView(eventView);
                        refreshDisplay(holder);
                    }
                });

                ImageButton notifButton = (ImageButton) eventView.findViewById(R.id.notif_button);

                if (eventCode.notificationsEnabled == false) {
                    notifButton.setImageResource(R.drawable.stock_notification_off);
                } else {
                    notifButton.setImageResource(R.drawable.stock_notification_on);
                }

                notifButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (notifButton.getContentDescription().equals("Notifications are on")) {
                            InformationCentral.rescindNotificationAlarm(eventCode, "imminent_events");
                            eventCode.notificationsEnabled = false;

                            notifButton.setContentDescription("Notifications are off");
                            notifButton.setImageResource(R.drawable.stock_notification_off);

                            Toast.makeText(InformationCentral.mainApplicationContext, "Notification disabled for " + eventCode.name, Toast.LENGTH_SHORT).show();
                        } else if (notifButton.getContentDescription().equals("Notifications are off")) {
                            InformationCentral.bindNotificationAlarm(eventCode, "imminent_events");
                            eventCode.notificationsEnabled = true;

                            notifButton.setContentDescription("Notifications are on");
                            notifButton.setImageResource(R.drawable.stock_notification_on);

                            Toast.makeText(InformationCentral.mainApplicationContext, "Notification enabled for " + eventCode.name, Toast.LENGTH_SHORT).show();
                        }

                        InformationCentral.saveBoundEventLog();
                    }
                });

                ImageButton editButton = (ImageButton) eventView.findViewById(R.id.edit_button);
                editButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View form = (View) LayoutInflater.from(InformationCentral.mainApplicationContext).inflate(R.layout.new_event_form, null);

                        int width = RelativeLayout.LayoutParams.MATCH_PARENT;
                        int height = RelativeLayout.LayoutParams.MATCH_PARENT;

                        ViewGroup e = (ViewGroup) form;
                        form.findViewById(R.id.complete_button).setBackgroundColor(ContextCompat.getColor(InformationCentral.mainApplicationContext, R.color.burgundy));
                        boolean f = true;
                        final PopupWindow formPop = new PopupWindow(form, width, height, f);
                        formPop.showAtLocation(view, Gravity.CENTER, 0, 0);

                        // MOD CODE: Avoid .xml like the PLAGUE
                        form.findViewById(R.id.textView).setVisibility(View.GONE);
                        form.findViewById(R.id.name_box).setVisibility(View.GONE);
                        form.findViewById(R.id.textView2).setVisibility(View.GONE);
                        form.findViewById(R.id.category_spinner).setVisibility(View.GONE);

                        ((TextView) form.findViewById(R.id.textView3)).setText("New duration: ");
                        ((TextView) form.findViewById(R.id.event_create_title)).setText("Modify Event");

                        Button saveButton = (Button) form.findViewById(R.id.complete_button);

                        saveButton.setText("Save");
                        saveButton.setBackgroundColor(ContextCompat.getColor(InformationCentral.mainApplicationContext, R.color.burgundy));

                        form.findViewById(R.id.complete_button).setBackgroundColor(ContextCompat.getColor(InformationCentral.mainApplicationContext, R.color.burgundy));

                        List<String> durationOptions = new ArrayList<String>();
                        durationOptions.add("minutes");
                        durationOptions.add("hours");
                        Spinner durationDropDown = form.findViewById(R.id.duration_type_spinner);
                        GenericPinkAdapter durationAdapter = new GenericPinkAdapter(InformationCentral.mainApplicationContext, durationOptions, e);
                        durationDropDown.setAdapter(durationAdapter);

                        saveButton.setOnClickListener(new modifyEventClickListener(eventCode, formPop));
                    }
                });

                editButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                notifButton.setVisibility(View.GONE);

                ImageButton settings = eventView.findViewById(R.id.settings_button);
                settings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ViewGroup mainView = (ViewGroup) view.getParent();

                        TextView eName = mainView.findViewById(R.id.event_name);
                        TextView eCat = mainView.findViewById(R.id.event_category);
                        TextView eDur = mainView.findViewById(R.id.event_duration);

                        Animation rotate1 = AnimationUtils.loadAnimation(InformationCentral.mainApplicationContext, R.anim.rotate_gear_clockwise);
                        Animation rotate2 = AnimationUtils.loadAnimation(InformationCentral.mainApplicationContext, R.anim.rotate_gear_counterclockwise);
                        Animation slideOut = AnimationUtils.loadAnimation(InformationCentral.mainApplicationContext, R.anim.slide_left);
                        Animation slideIn = AnimationUtils.loadAnimation(InformationCentral.mainApplicationContext, R.anim.slide_right);

                        if (deleteButton.getVisibility() == View.GONE) {
                            settings.startAnimation(rotate2);
                            eName.setVisibility(View.GONE);
                            eCat.setVisibility(View.GONE);
                            eDur.setVisibility(View.GONE);
                            editButton.setVisibility(View.VISIBLE);
                            deleteButton.setVisibility(View.VISIBLE);
                            notifButton.setVisibility(View.VISIBLE);
                            deleteButton.startAnimation(slideOut);
                            notifButton.startAnimation(slideOut);
                            editButton.startAnimation(slideOut);
                        } else {
                            settings.startAnimation(rotate1);
                            eName.setVisibility(View.VISIBLE);
                            eCat.setVisibility(View.VISIBLE);
                            eDur.setVisibility(View.VISIBLE);
                            deleteButton.startAnimation(slideIn);
                            notifButton.startAnimation(slideIn);
                            editButton.startAnimation(slideIn);
                            deleteButton.setVisibility(View.GONE);
                            notifButton.setVisibility(View.GONE);
                            editButton.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }
    }

    public void deleteEvent (BoundEvent discardedEvent, Date fromDate, DayViewHolder callHolder) {
        InformationCentral.rescindNotificationAlarm(discardedEvent, "imminent_events");
        List<BoundEvent> list = InformationCentral.boundEventLog.get(fromDate.toString());
        if (list != null && list.contains(discardedEvent)) {
            list.remove(discardedEvent);
            InformationCentral.boundEventLog.put(fromDate.toString(), list);
        }
        InformationCentral.saveBoundEventLog();
        refreshDisplay(callHolder);
    }

    private class modifyEventClickListener implements View.OnClickListener {

        BoundEvent eventInQuestion;
        PopupWindow formPop;

        public modifyEventClickListener(BoundEvent e, PopupWindow p) {
            eventInQuestion = e;
            formPop = p;
        }

        @Override
        public void onClick(View view) {
            ViewGroup formGroup = (ViewGroup) view.getParent();
            String unspecifiedTime = ((TextView) formGroup.findViewById(R.id.duration_box)).getText().toString();
            Spinner dropDown = (Spinner) formGroup.findViewById(R.id.duration_type_spinner);

            if (dropDown.getSelectedItem().toString().equals("hours")) {
                unspecifiedTime = Integer.toString(Integer.parseInt(unspecifiedTime) * 60);
            }

            InformationCentral.editBoundEventAttribute(eventInQuestion, "duration", unspecifiedTime);

            formPop.dismiss();

            InformationCentral.wipeAdapter();
        }
    }
}
