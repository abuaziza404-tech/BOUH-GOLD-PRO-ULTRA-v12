package com.aboaziza.bouhterrain;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public final class Ui {
    public static final int OBSIDIAN = Color.rgb(5, 7, 10);
    public static final int GOLD = Color.rgb(212, 175, 55);
    public static final int CYAN = Color.rgb(0, 229, 255);
    public static final int PANEL = Color.rgb(18, 23, 34);

    private Ui() {}

    public static TextView label(android.content.Context c, String text, float sp, int color, int style) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(android.graphics.Typeface.DEFAULT, style);
        v.setPadding(12, 8, 12, 8);
        return v;
    }

    public static Button button(android.content.Context c, String text, boolean gold) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(gold ? Color.BLACK : CYAN);
        b.setTextSize(13f);
        b.setBackgroundResource(gold ? R.drawable.button_gold : R.drawable.button_dark);
        b.setPadding(10, 8, 10, 8);
        return b;
    }
}
