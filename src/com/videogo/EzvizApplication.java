/* 
 * @ProjectName VideoGoJar
 * @Copyright HangZhou Hikvision System Technology Co.,Ltd. All Right Reserved
 * 
 * @FileName EzvizApplication.java
 * @Description 这里对文件进行描述
 * 
 * @author chenxingyf1
 * @data 2014-7-12
 * 
 * @note 这里写本文件的详细功能描述和注释
 * @note 历史记录
 * 
 * @warning 这里写本文件的相关警告
 */
package com.videogo;

import android.app.Application;

import com.videogo.openapi.EzvizAPI;

/**
 * 自定义应用
 * @author chenxingyf1
 * @data 2014-7-12
 */
public class EzvizApplication extends Application {
    //开放平台申请的APP key & secret key
    //open
    public static String APP_KEY = "8698d52f6ac34929b5286698fe7a10e8";
    public static String SECRET_KEY = "32be2dea4158a84ef4294a126038c90f";
    
    public static String API_URL = "https://open.ys7.com";
    public static String WEB_URL = "https://auth.ys7.com";

    
    @Override
    public void onCreate() {
        super.onCreate();
        
        EzvizAPI.init(this, APP_KEY, SECRET_KEY); 
        //EzvizAPI.init(this, APP_KEY, SECRET_KEY, "/mnt/sdcard/VideoGo/libs/"); 
        EzvizAPI.getInstance().setServerUrl(API_URL, WEB_URL);     
        //EzvizAPI.getInstance().setAccessToken("at.3tbl7zqm4s1qbyc06693hxrt00caiwps-706jl5farq-03uwgfo-ssheljs47");
        //EzvizAPI.getInstance().setUserCode("71cd711da693b315");
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        //EzvizAPI.getInstance().gotoLoginPage(false);
    }
}
