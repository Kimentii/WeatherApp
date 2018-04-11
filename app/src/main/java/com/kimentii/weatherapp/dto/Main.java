package com.kimentii.weatherapp.dto;

public class Main {
    private static final int MAX_TEMPERATURE_ON_EARTH_IN_CELSIUS = 70;
    private static final int MIN_TEMPERATURE_ON_EARTH_IN_CELSIUS = -90;
    private float temp;
    private float pressure;

    public Main(float temp) {
        this.temp = temp;
    }

    public float getTempInCelsius() throws Exception {
        if (temp < 0f) {
            throw new Exception("Wrong temperature.(impossible temperature)");
        }
        float tempInCelsius = temp - 273.15f;
        if (tempInCelsius > MAX_TEMPERATURE_ON_EARTH_IN_CELSIUS) {
            throw new Exception("Wrong temperature.(temperature is too big)");
        }
        if (tempInCelsius < MIN_TEMPERATURE_ON_EARTH_IN_CELSIUS) {
            throw new Exception("Wrong temperature.(temperature is too low)");
        }
        return tempInCelsius;
    }
}
