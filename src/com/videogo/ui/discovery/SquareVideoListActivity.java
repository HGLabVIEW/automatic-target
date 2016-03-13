package com.videogo.ui.discovery;

import java.util.Date;
import java.util.List;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.videogo.R;
import com.videogo.constant.Constant;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.req.GetSquareVideoInfoList;
import com.videogo.openapi.bean.resp.SquareColumnInfo;
import com.videogo.openapi.bean.resp.SquareVideoInfo;
import com.videogo.ui.realplay.RealPlayActivity;
import com.videogo.util.ConnectionDetector;
import com.videogo.util.Utils;
import com.videogo.widget.PullToRefreshFooter;
import com.videogo.widget.PullToRefreshFooter.Style;
import com.videogo.widget.PullToRefreshHeader;
import com.videogo.widget.TitleBar;
import com.videogo.widget.pulltorefresh.IPullToRefresh.Mode;
import com.videogo.widget.pulltorefresh.IPullToRefresh.OnRefreshListener;
import com.videogo.widget.pulltorefresh.LoadingLayout;
import com.videogo.widget.pulltorefresh.PullToRefreshBase;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.LoadingLayoutCreator;
import com.videogo.widget.pulltorefresh.PullToRefreshGridView;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.Orientation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

public class SquareVideoListActivity extends Activity {
	private PullToRefreshGridView mRefreshGridView;
	private GridView mGridView;
	private TitleBar mTitleBar;
	private SquareVideoAdapter mAdapter;
	private SquareColumnInfo columnInfo;
	private static final int pageSize = 20;
	private boolean isFirstLoad = true;
	private TextView emptyView;
    private static final String DEMO_PREFIX = Constant.RESP_URL_PREFIX + "shipingc?";
	
	public interface EXTRA_KEY {
		String SQUARE_COLUMN_ITEM = "SquareColumnItem";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_square_video_list);
		
		mTitleBar = (TitleBar) findViewById(R.id.title_bar);
		mTitleBar.setTitle(R.string.localmgt_video_square_txt);
		mTitleBar.addBackButton(new OnClickListener() {
			@Override
			public void onClick(View v) {				
				onBackPressed();
			}
		});
		
		mRefreshGridView = (PullToRefreshGridView) findViewById(R.id.squareVideoList);
		mGridView = mRefreshGridView.getRefreshableView();
		mGridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));
		mRefreshGridView.setLoadingLayoutCreator(new LoadingLayoutCreator() {
			@Override
			public LoadingLayout create(Context context, boolean headerOrFooter,
					Orientation orientation) {
				if (headerOrFooter) {
					return new PullToRefreshHeader(context);
				} else {
					return new PullToRefreshFooter(context, Style.MORE);
				}
			}
		});
		mRefreshGridView.setMode(Mode.BOTH);
		mRefreshGridView.setOnRefreshListener(new OnRefreshListener<GridView>() {
			@Override
			public void onRefresh(PullToRefreshBase<GridView> refreshView,
					boolean headerOrFooter) {
				if (columnInfo != null) {
					new GetVideoInfoTask(headerOrFooter).execute();
				}
			}
		});
		mAdapter = new SquareVideoAdapter(this);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(SquareVideoListActivity.this, RealPlayActivity.class);
				SquareVideoInfo item = mAdapter.getItem(position);
	            intent.putExtra(IntentConsts.EXTRA_RTSP_URL, item.getPlayUrl().substring(DEMO_PREFIX.length()));
	            startActivity(intent);
			}
		});
		
		emptyView = new TextView(this);
		emptyView.setText(R.string.refresh_empty_hint);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		columnInfo = getIntent().getParcelableExtra(EXTRA_KEY.SQUARE_COLUMN_ITEM);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (isFirstLoad) {
			mRefreshGridView.setMode(Mode.BOTH);
			mRefreshGridView.setRefreshing();
			isFirstLoad = false;
		}
	}
	
	private class GetVideoInfoTask extends AsyncTask<Void, Void, List<SquareVideoInfo>> {
		private boolean mHeaderOrFooter;
		private int mErrorCode = 0;
		
		public GetVideoInfoTask(boolean headerOrFooter) {
			mHeaderOrFooter = headerOrFooter;
		}
		
		@Override
		protected List<SquareVideoInfo> doInBackground(Void... params) {
			if (isFinishing()) {
				return null;
			}
			if (!ConnectionDetector.isNetworkAvailable(SquareVideoListActivity.this)) {
				mErrorCode = ErrorCode.ERROR_WEB_NET_EXCEPTION;
				return null;
			}
			if (columnInfo == null) {
				return null;
			}
			try {
				GetSquareVideoInfoList getSquareVideoInfoList = new GetSquareVideoInfoList();
				getSquareVideoInfoList.setPageSize(pageSize);
				if (mHeaderOrFooter) {
					getSquareVideoInfoList.setPageStart(0);
				} else {
					getSquareVideoInfoList.setPageStart(mAdapter.getCount() / pageSize);
				}
				getSquareVideoInfoList.setChannel(Integer.valueOf(columnInfo.getChannelCode()));
				return EzvizAPI.getInstance().getSquareVideoList(getSquareVideoInfoList);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (BaseException e) {
				e.printStackTrace();
				mErrorCode = e.getErrorCode();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<SquareVideoInfo> result) {
			super.onPostExecute(result);
			mRefreshGridView.onRefreshComplete();
			
			if (result != null) {
				if (mHeaderOrFooter) {
					CharSequence dateText = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date());
					for (LoadingLayout layout : mRefreshGridView.getLoadingLayoutProxy(true, false).getLayouts()) {
						((PullToRefreshHeader) layout).setLastRefreshTime(":" + dateText);
					}
					mAdapter.clear();
				}
				if (mAdapter.getCount() == 0 && result.size() == 0) {
					mRefreshGridView.setEmptyView(emptyView);
				} else if (result.size() < pageSize) {
					mRefreshGridView.setFooterRefreshEnabled(false);
				} else if (mHeaderOrFooter) {
					mRefreshGridView.setFooterRefreshEnabled(true);
				}
				mAdapter.appendData(result);
			}
			
			if (mErrorCode != 0) {
				onError(mErrorCode);
			}
		}
		
		protected void onError(int errorCode) {
			switch (errorCode) {
            case ErrorCode.ERROR_WEB_SESSION_ERROR:
            case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
            case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                EzvizAPI.getInstance().gotoLoginPage();
                break;
            default:
                if(mAdapter.getCount() == 0) {
                	emptyView.setText(R.string.refresh_fail_hint);
                } else {
                    Utils.showToast(SquareVideoListActivity.this, R.string.refresh_fail_hint,  errorCode);
                }
                break;
        }
		}
	}
}
