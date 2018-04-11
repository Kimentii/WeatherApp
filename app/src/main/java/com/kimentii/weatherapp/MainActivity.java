package com.kimentii.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.kimentii.weatherapp.dto.WeatherForecast;
import com.kimentii.weatherapp.dto.WeatherGuess;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int PERMISSIONS_REQUEST_LOCATION = 42;

    TextView networkStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        networkStatusTextView = findViewById(R.id.tv_network_status);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOnline()) {
                    networkStatusTextView.setVisibility(View.INVISIBLE);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSIONS_REQUEST_LOCATION);
                        return;
                    }
                    Handler handler = new Handler();
                    refreshWeatherForecast(handler);
                    Snackbar.make(view, "Done", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    networkStatusTextView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Status: offline");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length == 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Handler handler = new Handler();
                    refreshWeatherForecast(handler);
                } else {

                }
                return;
            }
        }
    }

    private void refreshWeatherForecast(final Handler handler) {
        final Location location = getLocation();
        if (location == null) {
            Log.d(TAG, "!!! NO LOCATION");
            return;
        }
        final String city = getCity(location);
        final String countryCode = getCountryCode(location);
        Log.d(TAG, "City: " + city);
        Log.d(TAG, "countryCode: " + countryCode);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = OpenWeatherMapRequester.requestWeather(getApplicationContext(),
                        city, countryCode);
                final WeatherForecast weatherForecast = new Gson().fromJson(jsonObject.toString(), WeatherForecast.class);
                /*for (WeatherGuess weatherGuess : weatherForecast.getList()) {
                    Log.d(TAG, "WeatherGuess: " + "date: " + weatherGuess.getDateInSeconds()
                            + " Temp: " + weatherGuess.getMain().getTempInCelsius());
                    Log.d(TAG, "Weather date: " + weatherGuess.getDateAsCalendar().get(Calendar.DAY_OF_WEEK)
                            + " time: " + weatherGuess.getDateAsCalendar().get(Calendar.HOUR_OF_DAY)
                            + ":" + weatherGuess.getDateAsCalendar().get(Calendar.MINUTE)
                            + " forecast: " + weatherGuess.getWeather().getMain());

                }*/
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateCurrentWeatherUi(location, weatherForecast.getList().get(0));
                    }
                });
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout linearLayout = findViewById(R.id.ll_week_forecast);
                        linearLayout.removeAllViews();
                        for (WeatherGuess weatherGuess : weatherForecast.getList()) {
                            addDailyWeatherForecastUi(weatherGuess);
                        }
                    }
                });
                // Log.d(TAG, jsonObject.toString());
            }
        });
        thread.start();
    }

    private void updateCurrentWeatherUi(Location location, WeatherGuess weatherGuess) {
        final ImageView iconImageView = findViewById(R.id.iv_current_weather_icon);
        final TextView countryTextView = findViewById(R.id.tv_current_weather_country);
        final TextView cityTextView = findViewById(R.id.tv_current_weather_city);
        final TextView currentWeatherTextView = findViewById(R.id.tv_current_weather);
        iconImageView.setImageDrawable(getIconFromWeatherDescription(weatherGuess.getWeather().getDescription()));
        countryTextView.setText(getCountryName(location));
        cityTextView.setText(getCity(location));
        currentWeatherTextView.setText(weatherGuess.getWeather().getMain() + " "
                + (int) weatherGuess.getMain().getTempInCelsius()
                + getResources().getString(R.string.symbol_degree_celsius));
    }

    private void addDailyWeatherForecastUi(WeatherGuess weatherGuess) {
        LayoutInflater layoutInflater = getLayoutInflater();
        View dayLayout = layoutInflater.inflate(R.layout.day_info, null, false);
        LinearLayout linearLayout = findViewById(R.id.ll_week_forecast);
        ImageView dayWeatherIconImageView = dayLayout.findViewById(R.id.iv_day_weather_icon);
        TextView dayOfWeekTextView = dayLayout.findViewById(R.id.tv_day_of_week);
        TextView timeTextView = dayLayout.findViewById(R.id.tv_time);
        TextView weatherTextView = dayLayout.findViewById(R.id.tv_weather);
        TextView temperatureTextView = dayLayout.findViewById(R.id.tv_temperature);
        dayWeatherIconImageView.setImageDrawable(getIconFromWeatherDescription(weatherGuess.getWeather().getDescription()));
        dayOfWeekTextView.setText(weatherGuess.getDateAsCalendar().getDisplayName(Calendar.DAY_OF_WEEK,
                Calendar.SHORT, Locale.getDefault()));
        timeTextView.setText(weatherGuess.getDateAsCalendar().get(Calendar.HOUR_OF_DAY)
                + ":" + weatherGuess.getDateAsCalendar().get(Calendar.MINUTE));
        weatherTextView.setText(weatherGuess.getWeather().getMain());
        temperatureTextView.setText((int) weatherGuess.getMain().getTempInCelsius() +
                getResources().getString(R.string.symbol_degree_celsius));
        linearLayout.addView(dayLayout);
    }

    private Drawable getIconFromWeatherDescription(String description) {
        return getResources().getDrawable(R.drawable.art_clear);
    }

    /*private String getAbbreviatedDayOfWeek(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return "mo";
            case Calendar.TUESDAY:
                return ""
        }
    }*/

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private Location getLocation() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        } else {
            Log.d(TAG, "Has location permission.");
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider, 0, 0.0f, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        // Log.d(TAG, "onLocationChanged");
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        //  Log.d(TAG, "onStatusChanged");
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        //   Log.d(TAG, "onProviderEnabled");
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        // Log.d(TAG, "onProviderDisabled");
                    }
                });
            }
            Log.d(TAG, "Num of providers: " + providers.size());
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }
            return bestLocation;
        }
    }

    private Address getAddress(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses.size() > 0) {
            return addresses.get(0);
        } else {
            return null;
        }
    }

    private String getCity(Location location) {
        return getAddress(location).getLocality();
    }

    private String getCountryCode(Location location) {
        return getAddress(location).getCountryCode();
    }

    private String getCountryName(Location location) {
        return getAddress(location).getCountryName();
    }
}
