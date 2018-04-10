package com.kimentii.weatherapp.dto;

public class Main {
    private float temp;
    private float pressure;

    public float getTempInCelsius() {
        return temp - 273.15f;
    }
}
