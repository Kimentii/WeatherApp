package com.kimentii.weatherapp.dto;

import java.util.List;

public class WeatherGuess {

    private List<Weather> weather;
    private Main main;
    private int dt;

    public List<Weather> getWeather() {
        return weather;
    }

    public Main getMain() {
        return main;
    }

    public int getDateInSeconds() {
        return dt;
    }
}
