package com.kimentii.weatherapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenWeatherMapRequester {
    private static final String TAG = OpenWeatherMapRequester.class.getSimpleName();
    private static final int MAX_ATTEMPTS_NUMBER = 10;
    private static final int BUFFER_SIZE = 1024;

    private static final String OPEN_WEATHER_MAP_API =
            "http://api.openweathermap.org/data/2.5/forecast?q=%s,%s";

    public static JSONObject requestWeather(Context context, String city, String countryCode) {
        for (int i = 0; i < MAX_ATTEMPTS_NUMBER; i++) {
            try {
                URL url = new URL(String.format(OPEN_WEATHER_MAP_API, city, countryCode));
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.addRequestProperty("x-api-key",
                        context.getString(R.string.open_weather_maps_app_id));

                int status = connection.getResponseCode();
                if (BuildConfig.DEBUG) Log.d(TAG, "Connection status: " + status);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuffer json = new StringBuffer(BUFFER_SIZE);
                String tmp = "";
                while ((tmp = reader.readLine()) != null)
                    json.append(tmp).append("\n");
                reader.close();

                if (BuildConfig.DEBUG) Log.d(TAG, "json: " + json);

                JSONObject data = new JSONObject(json.toString());

                if (data.getInt("cod") != 200) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "!!! response code isn't 200 !!!");
                    return null;
                }
                connection.disconnect();
                return data;
            } catch (FileNotFoundException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, "RequestWeather attempt: " + i);
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
