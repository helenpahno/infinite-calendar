package com.helenpahno.infinitecalendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class EventCategoryAdapter extends BaseAdapter {
    Context context;
    List<String> categories;
    List<Integer> colors;
    LayoutInflater inflater;
    ViewGroup parent;

    String newCategoryName;
    int newCategoryColor;

    public EventCategoryAdapter (Context c, List<String> categoryArray, List<Integer> correspondingColors, ViewGroup v) {
        context = c;
        categories = categoryArray;
        colors = correspondingColors;
        parent = v;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int i) {
        return categories.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = LayoutInflater.from(context).inflate(R.layout.category_spinner_bar, viewGroup, false);
        TextView swatch = (TextView) view.findViewById(R.id.color_swatch);
        TextView name = (TextView) view.findViewById(R.id.category_name);
        int color = colors.get(i);
        swatch.setBackgroundColor(color);
        name.setText(categories.get(i));
        return view;
    }
}
