package com.videogo.ui.discovery;

import java.util.HashMap;
import java.util.List;

import com.videogo.R;
import com.videogo.exception.BaseException;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.req.SearchSquareVideoInfo;
import com.videogo.openapi.bean.resp.FavoriteInfo;
import com.videogo.openapi.bean.resp.SquareVideoInfo;
import com.videogo.widget.TitleBar;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;

public class SearchVideoActivity extends FragmentActivity implements SearchVideoFragment.IOnSearchClick, SquareVideoListFragment.OnDataProcess {
	private SearchSquareVideoInfo searchSquareVideoInfo;
	private SearchVideoFragment searchFragment;
	private SquareVideoListFragment searchResultFragment;
	private TitleBar titleBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_square_video_search);
		titleBar = (TitleBar) findViewById(R.id.title_bar);
		titleBar.setTitle(R.string.search);
//		titleBar.addRightTextButton(getResources().getString(R.string.cities), new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				FragmentManager fm = getSupportFragmentManager();
//				CityConfigFragment ccf = new CityConfigFragment();
//				ccf.show(fm, "city_config_fragment");
//			}
//		});
		searchFragment = (SearchVideoFragment) getSupportFragmentManager().findFragmentById(R.id.searchFragment);
		searchFragment.setOnSearchClick(this);
	}
	
	@Override
	public void onSearch(SearchSquareVideoInfo searchSquareVideoInfo) {
		this.searchSquareVideoInfo = searchSquareVideoInfo;
		if (searchResultFragment == null) {
			searchResultFragment = new SquareVideoListFragment();
		}
		searchResultFragment.setOnDataProcess(this);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.container, searchResultFragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}
	
	@Override
	public List<SquareVideoInfo> loadMore(int pageStart, int pageSize, Object...objects)
			throws BaseException {
		if (searchSquareVideoInfo != null) {
			searchSquareVideoInfo.setPageSize(pageSize);
			searchSquareVideoInfo.setPageStart(pageStart);
			List<SquareVideoInfo> result = EzvizAPI.getInstance().searchSquareVideo(searchSquareVideoInfo);
			if (result != null) {
				StringBuilder sb = new StringBuilder();
				for (SquareVideoInfo videoInfo : result) {
					sb.append(videoInfo.getSquareId() + ",");
				}
				List<FavoriteInfo> favoriteInfoList = EzvizAPI.getInstance().checkSquareVideoFavorite(sb.toString());
				HashMap<String, Boolean> isCollected = new HashMap<String, Boolean>();
				for (FavoriteInfo favoriteInfo : favoriteInfoList) {
					isCollected.put(favoriteInfo.getSquareId(), true);
				}
				for (SquareVideoInfo videoInfo :result) {
					if (isCollected.get(videoInfo.getSquareId()) != null && isCollected.get(videoInfo.getSquareId())) {
						videoInfo.setCollected(true);
					}
				}
			}
			return result;
		} else {
			return null;
		}
	}
}
