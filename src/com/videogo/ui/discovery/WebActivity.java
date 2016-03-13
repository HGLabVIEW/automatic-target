package com.videogo.ui.discovery;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import com.videogo.R;
import com.videogo.constant.Constant;
import com.videogo.constant.IntentConsts;
import com.videogo.constant.UrlManager;
import com.videogo.ui.realplay.RealPlayActivity;
import com.videogo.util.LogUtil;
import com.videogo.widget.PullToRefreshHeader;
import com.videogo.widget.PullToRefreshHeader.Style;
import com.videogo.widget.TitleBar;
import com.videogo.widget.WebViewEx;
import com.videogo.widget.WebViewEx.WebChromeClientEx;
import com.videogo.widget.WebViewEx.WebViewClientEx;
import com.videogo.widget.pulltorefresh.IPullToRefresh.Mode;
import com.videogo.widget.pulltorefresh.IPullToRefresh.OnRefreshListener;
import com.videogo.widget.pulltorefresh.LoadingLayout;
import com.videogo.widget.pulltorefresh.PullToRefreshBase;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.LoadingLayoutCreator;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.Orientation;
import com.videogo.widget.pulltorefresh.PullToRefreshWebView;

public class WebActivity extends Activity {

    private static final String TAG = WebActivity.class.getSimpleName();

    private static final String DEMO_PREFIX = Constant.RESP_URL_PREFIX + "shipingc?";
    private static final String HLS_SUFFIX = ".m3u8";

    /** 标题栏 */
    private TitleBar mTitleBar;
    /** 下拉刷新WebView */
    private PullToRefreshWebView mPullToRefreshWebView;
    /** WebView */
    private WebViewEx mWebView;

    private ImageView mProgressView;

    protected UrlManager mUrlManager;
    private Animation mRotateAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_page);

        findViews();
        initData();
        initViews();
    }

    /**
     * 控件关联
     */
    private void findViews() {
        mTitleBar = (TitleBar) findViewById(R.id.title_bar);
        mPullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.webview);
        mWebView = mPullToRefreshWebView.getRefreshableView();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mUrlManager = UrlManager.getInstance();

        mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateAnimation.setInterpolator(new LinearInterpolator());
        mRotateAnimation.setDuration(1800);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mTitleBar.setTitle(title);
    }

    public Button addTitleBack() {
        return mTitleBar.addBackButton(new OnClickListener() {

            @Override
            public void onClick(View v) {
                WebActivity.super.onBackPressed();
            }
        });
    }

    public Button addTitleRightButton(int resId, OnClickListener l) {
        return mTitleBar.addRightButton(resId, l);
    }

    public void addTitleProgress() {
        mProgressView = mTitleBar.addRightProgress();
        mProgressView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mProgressView.getAnimation() == null) {
                    reload();
                }
            }
        });
    }

    /**
     * 初始化控件
     */
    private void initViews() {
        mPullToRefreshWebView.setLoadingLayoutCreator(new LoadingLayoutCreator() {

            @Override
            public LoadingLayout create(Context context, boolean headerOrFooter, Orientation orientation) {
                if (headerOrFooter)
                    return new PullToRefreshHeader(context, Style.NO_TIME);
                return null;
            }
        });
        mPullToRefreshWebView.setMode(Mode.PULL_FROM_START);
        mPullToRefreshWebView.setOnRefreshListener(new OnRefreshListener<WebViewEx>() {

            @Override
            public void onRefresh(PullToRefreshBase<WebViewEx> refreshView, boolean headerOrFooter) {
                if (headerOrFooter) {
                    reload();
                }
            }
        });

        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setSupportZoom(true);
        // JS 支持
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        setWebChromeClient(new CustomWebChromeClient());

        // HTML5 Storage支持
        mWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        mWebView.getSettings().setAppCachePath(appCachePath);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);

        mWebView.setDownloadListener(new CustomDownloadListener());
    }

    public void setPullToRefresh(boolean enable) {
        mPullToRefreshWebView.setMode(enable ? Mode.PULL_FROM_START : Mode.DISABLED);
    }

    public void setWebViewClient(CustomWebViewClient webViewClient) {
        mWebView.setWebViewClient(webViewClient);
    }

    public void setWebChromeClient(CustomWebChromeClient webChromeClient) {
        mWebView.setWebChromeClient(webChromeClient);
    }

    public WebView getWebView() {
        return mWebView;
    }

    public void reload() {
        mWebView.reload();
    }

    public class CustomWebViewClient extends WebViewClientEx {

        public CustomWebViewClient() {
            super(mWebView);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("mailto:") || url.startsWith("geo:") || url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;

            } else if (url.startsWith(DEMO_PREFIX)) {
                Intent intent = new Intent(WebActivity.this, RealPlayActivity.class);
                intent.putExtra(IntentConsts.EXTRA_RTSP_URL, url.substring(DEMO_PREFIX.length()));
                startActivity(intent);
                return true;

            } else if (url.endsWith(HLS_SUFFIX)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "video/*");
                startActivity(intent);
                return true;

            }

            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public void onPageStartedCompat(WebView view, String url, Bitmap favicon) {
            super.onPageStartedCompat(view, url, favicon);
            if (mProgressView != null)
                mProgressView.startAnimation(AnimationUtils.loadAnimation(WebActivity.this, R.anim.rotate_clockwise));
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            LogUtil.debugLog(TAG, "errorCode:" + errorCode + ";description:" + description + ";failingUrl:"
                    + failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mProgressView != null)
                mProgressView.clearAnimation();
            mPullToRefreshWebView.onRefreshComplete();
        }
    }

    public class CustomWebChromeClient extends WebChromeClientEx {

        public CustomWebChromeClient() {
            super(mWebView);
        }

        private View mView;
        private CustomViewCallback mCallback;

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            WebActivity.super.onBackPressed();
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            if (mCallback != null) {
                mCallback.onCustomViewHidden();
                mCallback = null;
                return;
            }

            ViewGroup parent = (ViewGroup) mWebView.getParent();
            parent.removeView(mWebView);
            parent.addView(view);
            mView = view;
            mCallback = callback;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (mView != null) {
                if (mCallback != null) {
                    mCallback.onCustomViewHidden();
                    mCallback = null;
                }

                ViewGroup parent = (ViewGroup) mView.getParent();
                parent.removeView(mView);
                parent.addView(mWebView);
                mView = null;
            }
        }
    }

    private class CustomDownloadListener implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }
}