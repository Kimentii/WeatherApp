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
                Log.d(TAG, "Connection status: " + status);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuffer json = new StringBuffer(1024);
                String tmp = "";
                while ((tmp = reader.readLine()) != null)
                    json.append(tmp).append("\n");
                reader.close();

                Log.d(TAG, "json: " + json);

                JSONObject data = new JSONObject(json.toString());

                if (data.getInt("cod") != 200) {
                    Log.d(TAG, "!!! cod is NULL !!!");
                    return null;
                }

                return data;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
