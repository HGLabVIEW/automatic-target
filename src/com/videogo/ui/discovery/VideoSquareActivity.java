package com.videogo.ui.discovery;

import android.os.Bundle;

import com.videogo.R;
import com.videogo.constant.UrlManager;

public class VideoSquareActivity extends WebActivity {

    private String mUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
        initTitleBar();
        initViews();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mUrl = mUrlManager.getUrl(UrlManager.URL_VIDEO_SQUARE);
    }

    /**
     * 初始化标题栏
     */
    private void initTitleBar() {
        setTitle(R.string.localmgt_video_square_txt);
        addTitleBack();
        addTitleProgress();
    }

    /**
     * 初始化控件
     */
    private void initViews() {
        setWebViewClient(new CustomWebViewClient());

        getWebView().loadUrl(mUrl);
    }
}