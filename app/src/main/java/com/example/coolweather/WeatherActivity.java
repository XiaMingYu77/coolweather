package com.example.coolweather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.uri.HttpUtil;
import com.example.coolweather.uri.Utility;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;
import com.scwang.smart.refresh.layout.listener.OnMultiListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ImageView bingPicImg;
    private Toolbar weatherToolbar;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private RefreshLayout weatherRefreshLayout;

    private Weather weather;

    String myHeFengKey = "c443db1f3b274a72814734a794b89f99";
    int intervalTimeInOneDay = 3;

    private void initActivity() {
        bingPicImg = findViewById(R.id.bing_pic_img);
        weatherToolbar = findViewById(R.id.weather_toolbar);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.weather_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        weatherRefreshLayout = findViewById(R.id.refreshLayout);
    }

    /**
     * 根据时间判断是否需要更新（不同日期或者间隔时间达到3小时）
     *
     * @param weatherString
     * @return
     */
    private boolean checkTimeInterval(String weatherString) {
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String updateTime = weather.basic.update.updateTime;
            //获取并解析当前时间
            String part = "yyyy-MM-dd HH:mm";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(part);
            Date date = new Date(System.currentTimeMillis());
            String timeRightNow = simpleDateFormat.format(date);
            //对比两者时间
            String strs1[] = updateTime.split(" ");
            String strs2[] = timeRightNow.split(" ");
            if (!strs1[0].equals(strs2[0])) {
                return false;
            } else {
                int hour1 = Integer.valueOf(strs1[1].split(":")[0]);
                int hour2 = Integer.valueOf(strs2[1].split(":")[0]);
                if (hour2 >= hour1 + intervalTimeInOneDay) {
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * 天气信息更新起始端
     */
    private void updateWeatherData() {
        String weatherId = weather.basic.weatherId;
        weatherLayout.setVisibility(View.INVISIBLE);
        requestWeather(weatherId);
        weatherRefreshLayout.finishRefresh();
    }

    private void updateWeatherData_afterChoose() {
        String weatherId = getIntent().getStringExtra("weather_id");
        weatherLayout.setVisibility(View.INVISIBLE);
        requestWeather(weatherId);
    }

    /**
     * Toolbar中的菜单
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.weather_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.re_choose_county:
                Toast.makeText(this, "重新选择城市", Toast.LENGTH_SHORT).show();
                weatherClear();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
        }
        return true;
    }

    /**
     * 清空天气缓存准备更新
     */
    private void weatherClear() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("weather");
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);//表示活动的布局会显示在状态栏上面
            getWindow().setStatusBarColor(Color.TRANSPARENT);//将状态栏设置为透明
        }

        initActivity();
        setSupportActionBar(weatherToolbar);

        //SharedPreferences 用于储存数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //初始化背景图片
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        String weatherString = prefs.getString("weather", null);
        //有缓存时判断是否需要更新，不需要直接开始解析
        if (weatherString != null) {
            reloadWeather();
            showWeatherInfo(weather);
            if (checkTimeInterval(weatherString)) {
                showWeatherInfo(weather);
            } else {
                weatherRefreshLayout.autoRefresh();
                updateWeatherData();
            }
        } else {
            updateWeatherData_afterChoose();
        }
        initSmartRefresh();
    }

    private void initSmartRefresh() {
        weatherRefreshLayout.setEnableRefresh(true);
        weatherRefreshLayout.setEnableOverScrollBounce(true);
        weatherRefreshLayout.setEnableOverScrollDrag(true);
        weatherRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                //开始下拉刷新
                updateWeatherData();
            }
        });
    }

    /**
     * 根据天气id获取城市天气信息
     *
     * @param weatherId
     */
    private void requestWeather(final String weatherId) {
        String weatherUri = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=" + myHeFengKey;
        HttpUtil.sendOkHttpRequest(weatherUri, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this)
                                    .edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                            reloadWeather();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        loadBingPic();//这样每次请求天气信息时也会刷新背景图片
    }

    /**
     * 处理并展示Weather实体类中的信息
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        //清空可能残留的天气信息
        forecastLayout.removeAllViews();
        for (Forecast forecast :
                weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_txt);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                Log.d("Weather", bingPic);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    public boolean reloadWeather() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
        String weatherText = sharedPreferences.getString("weather",null);
        if (weatherText!=null){
            weather=Utility.handleWeatherResponse(weatherText);
            return true;
        }else{
            return false;
        }
    }
}