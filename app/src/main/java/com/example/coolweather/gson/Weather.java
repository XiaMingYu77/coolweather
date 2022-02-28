package com.example.coolweather.gson;


import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 引用各个实体类
 */
public class Weather {

    public String status; //获取成功返回OK

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
