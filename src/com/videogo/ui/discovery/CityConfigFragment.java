package com.videogo.ui.discovery;

import java.util.Date;
import java.util.List;

import com.videogo.R;
import com.videogo.asynctask.AsyncTaskBase;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.resp.ConfigCity;
import com.videogo.util.Utils;
import com.videogo.widget.PullToRefreshFooter;
import com.videogo.widget.PullToRefreshFooter.Style;
import com.videogo.widget.PullToRefreshHeader;
import com.videogo.widget.pulltorefresh.IPullToRefresh.Mode;
import com.videogo.widget.pulltorefresh.IPullToRefresh.OnRefreshListener;
import com.videogo.widget.pulltorefresh.LoadingLayout;
import com.videogo.widget.pulltorefresh.PullToRefreshBase;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.LoadingLayoutCreator;
import com.videogo.widget.pulltorefresh.PullToRefreshListView;
import com.videogo.widget.pulltorefresh.PullToRefreshBase.Orientation;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class CityConfigFragment extends DialogFragment {
	private PullToRefreshListView mRefreshListView;
	private ListView mListView;
	private CityConfigAdapter mAdapter;
	private static final int pageSize = 20;
	private int pageStart = 0;
	private TextView emptyView;
	private CityConfigTask cityConfigTask;

	public CityConfigFragment() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_city_config, container, false);
		mRefreshListView = (PullToRefreshListView) view.findViewById(R.id.cifyConfigList);
		mRefreshListView.setLoadingLayoutCreator(new LoadingLayoutCreator() {
			@Override
			public LoadingLayout create(Context context, boolean headerOrFooter,
					Orientation orientation) {
				if (headerOrFooter) {
					return new PullToRefreshHeader(getActivity());
				} else {
					return new PullToRefreshFooter(getActivity(), Style.EMPTY_NO_MORE);
				}
			}
		});
		mRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView,
					boolean headerOrFooter) {
				cityConfigTask = new CityConfigTask(getActivity(), headerOrFooter);
				cityConfigTask.execute();
			}
		});
		mRefreshListView.setMode(Mode.BOTH);
		mListView = mRefreshListView.getRefreshableView();
		mAdapter = new CityConfigAdapter(getActivity());
		mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ConfigCity configCity = (ConfigCity) parent.getAdapter().getItem(position);
                new AsyncTaskBase<Void, Void, String>(getActivity(), true) {

                    @Override
                    protected String realDoInBackground(Void... params) throws BaseException {
                        return EzvizAPI.getInstance().getCityConfig(configCity.getCityKey());
                    }

                    @Override
                    protected void realOnPostExecute(String cityName) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.cityConfig).setMessage(cityName).create().show();
                    }

                    @Override
                    protected void onError(int mErrorCode) {
                    }
                }.execute();
            }
        });
		emptyView = new TextView(getActivity());
		emptyView.setText(R.string.refresh_empty_hint);
		emptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		emptyView.setGravity(Gravity.CENTER);
		
		getDialog().setTitle(R.string.cities);

        mRefreshListView.setRefreshing();
		return view;
	}

    @Override
    public void onDestroyView() {
        mRefreshListView = null;
        mListView = null;
        mAdapter.clear();
        mAdapter = null;
        cityConfigTask.cancel(true);
        cityConfigTask = null;
        emptyView = null;
        super.onDestroyView();
    }

    private class CityConfigTask extends AsyncTaskBase<Void, Void, List<ConfigCity>> {
		private boolean mHeaderOrFooter;
		
		public CityConfigTask(Context context, boolean headerOrFooter) {
			super(context);
			this.mHeaderOrFooter = headerOrFooter;
		}

		@Override
		protected List<ConfigCity> realDoInBackground(Void... params)
				throws BaseException {
			if (mHeaderOrFooter) {
				pageStart = 0;
			} else {
				pageStart = mAdapter.getCount() / pageSize;
			}
			return EzvizAPI.getInstance().getConfigCityList(pageStart, pageSize);
		}

		@Override
		protected void realOnPostExecute(List<ConfigCity> result) {
			mRefreshListView.onRefreshComplete();
			if (result != null) {
				if (mHeaderOrFooter) {
					CharSequence dateText = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date());
					for (LoadingLayout layout : mRefreshListView.getLoadingLayoutProxy(true, false).getLayouts()) {
						((PullToRefreshHeader) layout).setLastRefreshTime(":" + dateText);
					}
					mRefreshListView.setFooterRefreshEnabled(true);
					mAdapter.clear();
				}
				if (result.size() < pageSize) {
					mRefreshListView.setFooterRefreshEnabled(false);
				} else {
					mRefreshListView.setFooterRefreshEnabled(true);
				}
				mAdapter.appendData(result);
			}
			if (mAdapter.getCount() == 0) {
				mRefreshListView.setEmptyView(emptyView);
			}
		}

		@Override
		protected void onError(int mErrorCode) {
            mRefreshListView.onRefreshComplete();
            switch (mErrorCode) {
                case ErrorCode.ERROR_WEB_SESSION_ERROR:
                case ErrorCode.ERROR_WEB_SESSION_EXPIRE:
                case ErrorCode.ERROR_WEB_HARDWARE_SIGNATURE_ERROR:
                    EzvizAPI.getInstance().gotoLoginPage();
                    break;
                default:
                    String hintText = TextUtils.isEmpty(msg) ? getString(R.string.refresh_fail_hint) : msg;
                    if(mAdapter.getCount() == 0) {
                        emptyView.setText(hintText + mErrorCode);
                        mRefreshListView.setEmptyView(emptyView);
                    } else {
                        Utils.showToast(getActivity(), hintText + mErrorCode);
                    }
                    break;
            }
		}
		
	}
}

class CityConfigAdapter extends VideoGoBaseAdapter<ConfigCity> {
	public CityConfigAdapter(Context context) {
		super(context);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.city_config_item, null);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.render(position);
		return convertView;
	}
	
	private class ViewHolder {
		private TextView cityKey, cityName;
		
		public ViewHolder(View view) {
			cityKey = (TextView) view.findViewById(R.id.cityKey);
			cityName = (TextView) view.findViewById(R.id.cityName);
		}
		
		public void render(int position) {
			ConfigCity configCity = getItem(position);
			cityKey.setText(configCity.getCityKey());
			cityName.setText(configCity.getCityName());
		}
	}
}
