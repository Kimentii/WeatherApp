package com.kimentii.weatherapp.dto;

import android.content.Context;
import android.content.res.Resources;

import com.kimentii.weatherapp.R;

public class Weather {
    private String main;
    private String description;
    private String icon;
    private int id;

    public String getMain(Context context) {
        Resources resources = context.getResources();
        if (id >= 200 && id < 300) {
            return resources.getString(R.string.main_group_thunderstorm);
        } else if (id >= 300 && id < 400) {
            return resources.getString(R.string.main_group_drizzle);
        } else if (id >= 500 && id < 600) {
            return resources.getString(R.string.main_group_rain);
        } else if (id >= 600 && id < 700) {
            return resources.getString(R.string.main_group_snow);
        } else if (id >= 700 && id < 800) {
            return resources.getString(R.string.main_group_atmosphere);
        } else if (id == 800) {
            return resources.getString(R.string.main_group_clear);
        } else if (id == 801) {
            return resources.getString(R.string.main_group_clouds);
        } else if (id > 801 && id < 810) {
            return resources.getString(R.string.main_group_clouds);
        } else if (id >= 900 && id < 910) {
            return resources.getString(R.string.main_group_extreme);
        } else if (id >= 910 && id < 1000) {
            return resources.getString(R.string.main_group_additional);
        }
        return main;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public int getId() {
        return id;
    }
}
