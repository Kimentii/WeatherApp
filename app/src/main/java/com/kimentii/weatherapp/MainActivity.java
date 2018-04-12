package com.kimentii.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.kimentii.weatherapp.dto.WeatherForecast;
import com.kimentii.weatherapp.dto.WeatherGuess;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int PERMISSIONS_REQUEST_LOCATION = 42;

    private TextView networkStatusTextView;
    private FloatingActionButton refreshFloatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        networkStatusTextView = findViewById(R.id.tv_network_status);

        refreshFloatingActionButton = findViewById(R.id.fab);
        final Handler handler = new Handler();
        refreshFloatingActionButton.setOnClickListener(new View.OnClickListener() {
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
                    refreshWeatherForecast(handler);
                } else {
                    networkStatusTextView.setVisibility(View.VISIBLE);
                    networkStatusTextView.setText(R.string.message_offline);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Status: offline");
                }
            }
        });

        final WeatherForecast weatherForecast = SharedPreferencesProvider
                .getWeatherForecast(MainActivity.this);

        final Location location = SharedPreferencesProvider
                .getLocation(MainActivity.this);

        if (location != null && weatherForecast != null) {
            updateCurrentWeatherUi(location, weatherForecast.getList().get(0));
            LinearLayout linearLayout = findViewById(R.id.ll_week_forecast);
            linearLayout.removeAllViews();
            for (WeatherGuess weatherGuess : weatherForecast.getList()) {
                addDailyWeatherForecastUi(weatherGuess);
            }
        } else {
            TextView messageTextView = findViewById(R.id.tv_message_no_data);
            messageTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onRequestPermissionsResult");
        if (BuildConfig.DEBUG) Log.d(TAG, "Num of grandResults: " + grantResults.length);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length == 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "onRequestPermissionsResult: refreshing data");
                    Handler handler = new Handler();
                    refreshWeatherForecast(handler);
                }
            }
        }
    }

    private void refreshWeatherForecast(final Handler handler) {
        final Location location = getLocation();
        if (location == null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "!!! no location !!!");
            return;
        }
        SharedPreferencesProvider.putLocation(MainActivity.this, location);
        final String city = getCity(location, Locale.ENGLISH);
        final String countryCode = getCountryCode(location, Locale.ENGLISH);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = OpenWeatherMapRequester.requestWeather(getApplicationContext(),
                        city, countryCode);
                if (jsonObject != null) {
                    final WeatherForecast weatherForecast = new Gson().fromJson(jsonObject.toString(), WeatherForecast.class);
                    SharedPreferencesProvider.putWeatherForecast(MainActivity.this, weatherForecast);
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
                            stopFabRotateAnimation(refreshFloatingActionButton);
                        }
                    });
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Fail in loading data.");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            stopFabRotateAnimation(refreshFloatingActionButton);
                            networkStatusTextView.setVisibility(View.VISIBLE);
                            networkStatusTextView.setText(R.string.message_cant_load_data);
                        }
                    });
                }
            }
        });
        startFabRotateAnimation(refreshFloatingActionButton);
        thread.start();
    }

    private void updateCurrentWeatherUi(Location location, WeatherGuess weatherGuess) {
        findViewById(R.id.tv_message_no_data).setVisibility(View.INVISIBLE);
        final ImageView iconImageView = findViewById(R.id.iv_current_weather_icon);
        final TextView countryTextView = findViewById(R.id.tv_current_weather_country);
        final TextView cityTextView = findViewById(R.id.tv_current_weather_city);
        final TextView currentWeatherTextView = findViewById(R.id.tv_current_weather);
        iconImageView.setImageDrawable(getIconFromWeatherDescription(weatherGuess.getWeather().getId()));
        countryTextView.setText(getCountryName(location, Locale.getDefault()));
        cityTextView.setText(getCity(location, Locale.getDefault()));
        try {
            currentWeatherTextView.setText(String.format(getString(R.string.title_current_weather),
                    weatherGuess.getWeather().getMain(getApplicationContext()),
                    (int) weatherGuess.getMain().getTempInCelsius(),
                    getResources().getString(R.string.symbol_degree_celsius)));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        dayWeatherIconImageView.setImageDrawable(getIconFromWeatherDescription(weatherGuess.getWeather().getId()));
        dayOfWeekTextView.setText(weatherGuess.getDateAsCalendar().getDisplayName(Calendar.DAY_OF_WEEK,
                Calendar.SHORT, Locale.getDefault()));
        SimpleDateFormat simpleDateFormat;
        if (BuildConfig.DEBUG) Log.d(TAG, "Locale: " + Locale.getDefault().getCountry());
        simpleDateFormat = new SimpleDateFormat("h:mm", Locale.getDefault());

        timeTextView.setText(simpleDateFormat.format(weatherGuess.getDateAsCalendar().getTime()));
        weatherTextView.setText(weatherGuess.getWeather().getMain(getApplicationContext()));
        try {
            temperatureTextView.setText(String.format(getString(R.string.title_temperature),
                    (int) weatherGuess.getMain().getTempInCelsius(),
                    getResources().getString(R.string.symbol_degree_celsius)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        linearLayout.addView(dayLayout);
    }

    private Drawable getIconFromWeatherDescription(int id) {
        Resources resources = getResources();
        if (id >= 200 && id < 300) {
            return resources.getDrawable(R.drawable.art_thunderstorm);
        } else if (id >= 300 && id < 400) {
            return resources.getDrawable(R.drawable.art_rain);
        } else if (id >= 500 && id < 600) {
            return resources.getDrawable(R.drawable.art_shower_rain);
        } else if (id >= 600 && id < 700) {
            return resources.getDrawable(R.drawable.art_snow);
        } else if (id >= 700 && id < 800) {
            return resources.getDrawable(R.drawable.art_mist);
        } else if (id == 800) {
            return resources.getDrawable(R.drawable.art_clear_sky);
        } else if (id == 801) {
            return resources.getDrawable(R.drawable.art_few_clouds);
        } else if (id > 801 && id < 810) {
            return resources.getDrawable(R.drawable.art_clouds);
        }
        return null;
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private Location getLocation() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Has location permission.");
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return null;
            List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                }, this.getMainLooper());
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Num of providers: " + providers.size());
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

    private void startFabRotateAnimation(FloatingActionButton floatingActionButton) {
        floatingActionButton.setClickable(false);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.loading_data);
        floatingActionButton.startAnimation(animation);
    }

    private void stopFabRotateAnimation(FloatingActionButton floatingActionButton) {
        floatingActionButton.clearAnimation();
        floatingActionButton.setClickable(true);
    }

    private Address getAddress(Location location, Locale locale) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        Geocoder gcd = new Geocoder(getApplicationContext(), locale);
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {
            return addresses.get(0);
        } else {
            return null;
        }
    }

    private String getCity(Location location, Locale locale) {
        Address address = getAddress(location, locale);
        return address != null ? address.getLocality() : null;
    }

    private String getCountryCode(Location location, Locale locale) {
        Address address = getAddress(location, locale);
        return address != null ? address.getCountryCode() : null;
    }

    private String getCountryName(Location location, Locale locale) {
        Address address = getAddress(location, locale);
        return address != null ? address.getCountryName() : null;
    }
}
