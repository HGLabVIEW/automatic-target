package com.videogo.ui.discovery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.videogo.R;
import com.videogo.asynctask.AsyncTaskBase;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.resp.SquareColumnInfo;
import com.videogo.util.Utils;
import com.videogo.widget.PullToRefreshFooter;
import com.videogo.widget.PullToRefreshFooter.Style;
import com.videogo.widget.PullToRefreshHeader;
import com.videogo.widget.TitleBar;
import com.videogo.widget.pulltorefresh.IPullToRefresh.Mode;
import com.videogo.widget.pulltorefresh.IPullToRefresh.OnRefreshListener;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.LoadingLayoutCreator;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.Orientation;
import com.videogo.widget.pulltorefresh.LoadingLayout;
import com.videogo.widget.pulltorefresh.PullToRefreshBase;
import com.videogo.widget.pulltorefresh.PullToRefreshGridView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class SquareColumnActivity extends Activity {
	private PullToRefreshGridView mRefreshGridView;
	private GridView mGridView;
	private SquareColumnAdapter adapter;
	private TitleBar mTitleBar;
	private boolean isFirstLoad = true;
	private TextView emptyView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_square);

		mTitleBar = (TitleBar) findViewById(R.id.title_bar);
		mTitleBar.setTitle(R.string.localmgt_video_square_txt);
		mTitleBar.addRightTextButton(getString(R.string.search), new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SquareColumnActivity.this, SearchVideoActivity.class);
				startActivity(intent);
			}
		});
		
		mTitleBar.addBackButton(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		
		mRefreshGridView = (PullToRefreshGridView) findViewById(R.id.square_channel);
		mGridView = mRefreshGridView.getRefreshableView();
        mGridView.setId(0);  //PullToRefreshGridView和mGridView共用了一个id，设成0防止程序被杀掉恢复的时候因为同id问题崩溃
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
		mRefreshGridView.setOnRefreshListener(new OnRefreshListener<GridView>() {
			@Override
			public void onRefresh(PullToRefreshBase<GridView> refreshView,
					boolean headerOrFooter) {
				new GetSquareInfoTask(headerOrFooter).execute();
			}
		});
		mRefreshGridView.setMode(Mode.PULL_FROM_START);
		adapter = new SquareColumnAdapter(SquareColumnActivity.this);
		mGridView.setAdapter(adapter);
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(SquareColumnActivity.this, SquareVideoListActivity2.class);
				intent.putParcelableArrayListExtra(SquareVideoListActivity2.EXTRA_KEY.COLUMN_INFOS, (ArrayList<? extends Parcelable>) adapter.getList());
				intent.putExtra(SquareVideoListActivity2.EXTRA_KEY.COLUMN_POSITION, position);
				startActivity(intent);
			}
		});
		
		emptyView = new TextView(this);
		emptyView.setText(R.string.refresh_empty_hint);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (isFirstLoad) {
			mRefreshGridView.setMode(Mode.PULL_FROM_START);
			mRefreshGridView.setRefreshing();
			isFirstLoad = false;
		}
	}
	
	private class GetSquareInfoTask extends AsyncTaskBase<Void, Void, List<SquareColumnInfo>> {
		private boolean mHeaderOrFooter;
		
		public GetSquareInfoTask(boolean headerOrFooter) {
			super(SquareColumnActivity.this);
			mHeaderOrFooter = headerOrFooter;
		}
		
		@Override
		protected void onError(int errorCode) {
			mRefreshGridView.onRefreshComplete();
			switch (errorCode) {
            case ErrorCode.ERROR_WEB_SESSION_ERROR:
            case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
            case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                EzvizAPI.getInstance().gotoLoginPage();
                break;
            default:
                String hintText = TextUtils.isEmpty(msg) ? getString(R.string.refresh_fail_hint) : msg;
                if(adapter.getCount() == 0) {
                	emptyView.setText(hintText + errorCode);
                	mRefreshGridView.setEmptyView(emptyView);
                } else {
                    Utils.showToast(SquareColumnActivity.this, hintText + errorCode);
                }
                break;
        }
		}

		@Override
		protected List<SquareColumnInfo> realDoInBackground(Void... params) throws BaseException {
			return EzvizAPI.getInstance().getSquareColumn();
		}

		@Override
		protected void realOnPostExecute(List<SquareColumnInfo> result) {
			mRefreshGridView.onRefreshComplete();
			if (result != null) {
				if (mHeaderOrFooter) {
					CharSequence dateText = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date());
					for (LoadingLayout layout : mRefreshGridView.getLoadingLayoutProxy(true, false).getLayouts()) {
						((PullToRefreshHeader) layout).setLastRefreshTime(":" + dateText);
					}
					adapter.clear();
				}
				if (adapter.getCount() == 0 && result.size() == 0) {
					mRefreshGridView.setEmptyView(emptyView);
				}
				adapter.setList(result);
			}
		}
	}
}
