package com.example.coolweather.uri;

import android.text.TextUtils;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;


public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) { //TextUtils是系统自带的工具类
            try {
                Gson gson = new Gson();
                List<Province> allProvinces = gson.fromJson(response, new TypeToken<List<Province>>() {
                }.getType());
                for (int i = 0; i < allProvinces.size(); i++) {
                    Province province = allProvinces.get(i);
                    province.save();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                Gson gson = new Gson();
                List<City> allCities = gson.fromJson(response, new TypeToken<List<City>>() {
                }.getType());
                for (int i = 0; i < allCities.size(); i++) {
                    City city = allCities.get(i);
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCounty(String response, int cityId) {
        if (!TextUtils.isEmpty(response)){
            try{
                Gson gson=new Gson();
                List<County> allCounties=gson.fromJson(response,new TypeToken<List<County>>(){}.getType());
                for (int i = 0; i < allCounties.size(); i++) {
                    County county= allCounties.get(i);
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }
}
