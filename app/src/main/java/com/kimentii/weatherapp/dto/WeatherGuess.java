package com.kimentii.weatherapp.dto;


import java.util.Calendar;
import java.util.List;

public class WeatherGuess {
    private static final int MILLISECONDS_PER_SECOND = 1000;

    private List<Weather> weather;
    private Main main;
    private int dt;

    public Weather getWeather() {
        return weather.get(0);
    }

    public Main getMain() {
        return main;
    }

    public int getDateInSeconds() {
        return dt;
    }

    public Calendar getDateAsCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dt * MILLISECONDS_PER_SECOND);
        return calendar;
    }
}
