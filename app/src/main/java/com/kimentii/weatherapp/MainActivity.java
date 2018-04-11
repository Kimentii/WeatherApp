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
        iconImageView.setImageDrawable(getIconFromWeatherDescription(weatherGuess.getWeather().getId()));
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
        dayWeatherIconImageView.setImageDrawable(getIconFromWeatherDescription(weatherGuess.getWeather().getId()));
        dayOfWeekTextView.setText(weatherGuess.getDateAsCalendar().getDisplayName(Calendar.DAY_OF_WEEK,
                Calendar.SHORT, Locale.getDefault()));
        timeTextView.setText(weatherGuess.getDateAsCalendar().get(Calendar.HOUR_OF_DAY)
                + ":" + weatherGuess.getDateAsCalendar().get(Calendar.MINUTE));
        weatherTextView.setText(weatherGuess.getWeather().getMain());
        temperatureTextView.setText((int) weatherGuess.getMain().getTempInCelsius() +
                getResources().getString(R.string.symbol_degree_celsius));
        linearLayout.addView(dayLayout);
    }

    private Drawable getIconFromWeatherDescription(int id) {
        Log.d(TAG, "Weather id: " + id);
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

            /*if (description.equals("clear sky")) {
                resources.getDrawable(R.drawable.art_clear_sky);
            } else if (description.equals("few clouds")) {
                resources.getDrawable(R.drawable.art_few_clouds);
            } else if (description.equals("scattered clouds")) {
                resources.getDrawable(R.drawable.art_few_clouds);
            } else if (description.equals("broken clouds")) {
                resources.getDrawable(R.drawable.art_clouds);
            } else if (description.equals("shower rain")) {
                resources.getDrawable(R.drawable.art_shower_rain);
            } else if (description.equals("rain")) {
                resources.getDrawable(R.drawable.art_rain);
            } else if (description.equals("thunderstorm")) {
                resources.getDrawable(R.drawable.art_thunderstorm);
            } else if (description.equals("snow")) {
                resources.getDrawable(R.drawable.art_snow);
            } else if (description.equals("mist")) {
                resources.getDrawable(R.drawable.art_mist);
            }*/
        return null;
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
        if (addresses != null && addresses.size() > 0) {
            return addresses.get(0);
        } else {
            return null;
        }
    }

    private String getCity(Location location) {
        Address address = getAddress(location);
        return address != null ? address.getLocality() : null;
    }

    private String getCountryCode(Location location) {
        Address address = getAddress(location);
        return address != null ? address.getCountryCode() : null;
    }

    private String getCountryName(Location location) {
        Address address = getAddress(location);
        return address != null ? address.getCountryName() : null;
    }
}
