package com.gz.mymqtt;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * store and read titles for sharedpreferences
 * Created by host on 2016/1/22.
 */
public class LocalTitles {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private List<String> titles;

    public LocalTitles(Context context) {
        titles = new ArrayList<>();
        sharedPreferences = context.getSharedPreferences("titles", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void add(String t) {
        getTitles();
        if (titles.contains(t))
            return;
        titles.add(t);
        renewSP();
    }

    public void remove(String t) {
        getTitles();
        if (!titles.contains(t))
            return;
        titles.remove(t);
        renewSP();
    }

    private void renewSP() {
        editor.clear();
        int size = titles.size();
        for (int i = 0; i < size; i++) {
            editor.putString("title" + i, titles.get(i));
        }
        editor.commit();
    }

    public List<String> getTitles() {
        titles.clear();
        for (int i = 0; !sharedPreferences.getString("title" + i, "null").equals("null"); i++) {
            titles.add(sharedPreferences.getString("title" + i, "null"));
        }
        StringBuilder sb = new StringBuilder();
        for (String t : titles) {
            sb.append(t + " ");
        }
        Log.i("localtitles", sb.toString());
        return titles;
    }
}
