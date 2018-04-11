package com.kimentii.weatherapp;

import com.kimentii.weatherapp.dto.Main;

import junit.framework.Assert;

import org.junit.Test;


public class MainTest {
    @Test
    public void CorrectValuesConversationTest() throws Exception {
        Main main1 = new Main(184);
        Main main2 = new Main(343);
        Assert.assertEquals(main1.getTempInCelsius(), 184 - 273.15f);
        Assert.assertEquals(main2.getTempInCelsius(), 343 - 273.15f);
    }

    @Test(expected = Exception.class)
    public void ExceptionThrowsWhenImpossibleValueWasSet() throws Exception {
        Main main = new Main(-10);
        main.getTempInCelsius();
    }

    @Test(expected = Exception.class)
    public void ExceptionThrowsWhenTooBigValueWasSet() throws Exception {
        Main main = new Main(180);
        main.getTempInCelsius();
    }

    @Test(expected = Exception.class)
    public void ExceptionThrowsWhenTooLowValueWasSet() throws Exception {
        Main main = new Main(350);
        main.getTempInCelsius();
    }
}