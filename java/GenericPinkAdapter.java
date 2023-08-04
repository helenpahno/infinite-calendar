package com.helenpahno.infinitecalendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class GenericPinkAdapter extends BaseAdapter {

    List<String> assets;
    Context context;
    ViewGroup parent;

    public GenericPinkAdapter(Context c, List<String> array, ViewGroup v) {
        context = c;
        assets = array;
        parent = v;
    }

    @Override
    public int getCount() {
        return assets.size();
    }

    @Override
    public Object getItem(int i) {
        return assets.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = LayoutInflater.from(context).inflate(R.layout.generic_spinner_bar, viewGroup, false);
        TextView optionName = view.findViewById(R.id.option_name);
        optionName.setText(assets.get(i));
        return view;
    }
}
