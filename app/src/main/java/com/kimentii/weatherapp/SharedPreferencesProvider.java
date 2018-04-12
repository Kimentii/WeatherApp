package com.kimentii.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;

import com.google.gson.Gson;
import com.kimentii.weatherapp.dto.WeatherForecast;

public class SharedPreferencesProvider {

    public static void putWeatherForecast(AppCompatActivity appCompatActivity, WeatherForecast weatherForecast) {
        SharedPreferences sharedPreferences = appCompatActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = new Gson().toJson(weatherForecast);
        editor.putString(appCompatActivity.getResources().getString(R.string.key_saved_weather_forecast), json);
        editor.apply();
    }

    public static WeatherForecast getWeatherForecast(AppCompatActivity appCompatActivity) {
        SharedPreferences sharedPreferences = appCompatActivity.getPreferences(Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(appCompatActivity.getResources()
                .getString(R.string.key_saved_weather_forecast), null);
        if (json != null) {
            return new Gson().fromJson(json, WeatherForecast.class);
        } else {
            return null;
        }
    }

    public static void putLocation(AppCompatActivity appCompatActivity, Location location) {
        SharedPreferences sharedPreferences = appCompatActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(appCompatActivity.getResources().getString(R.string.key_saved_location_provider),
                location.getProvider());
        editor.putString(appCompatActivity.getResources().getString(R.string.key_saved_latitude),
                String.valueOf(location.getLatitude()));
        editor.putString(appCompatActivity.getResources().getString(R.string.key_saved_longitude),
                String.valueOf(location.getLongitude()));
        editor.apply();
    }

    public static Location getLocation(AppCompatActivity appCompatActivity) {
        SharedPreferences sharedPreferences = appCompatActivity.getPreferences(Context.MODE_PRIVATE);
        String provider = sharedPreferences.getString(
                appCompatActivity.getResources().getString(R.string.key_saved_location_provider), null);
        String latitude = sharedPreferences.getString(
                appCompatActivity.getResources().getString(R.string.key_saved_latitude), null);
        String longitude = sharedPreferences.getString(
                appCompatActivity.getResources().getString(R.string.key_saved_longitude), null);
        Location location = null;
        if (provider != null && latitude != null && longitude != null) {
            location = new Location(provider);
            location.setLatitude(Double.parseDouble(latitude));
            location.setLongitude(Double.parseDouble(longitude));
        }
        return location;
    }
}
