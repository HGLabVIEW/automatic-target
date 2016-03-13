package com.videogo.asynctask;

import android.content.Context;

import com.videogo.exception.BaseException;
import com.videogo.openapi.EzvizAPI;
import com.videogo.openapi.bean.resp.SquareVideoInfo;

/**
 * 
 * @author wuwenchao3
 * 收藏及取消收藏
 */
public class CollectTask extends AsyncTaskBase<Void, Void, Boolean>{
	private Context context;
	private OnDataNotifyListener onDataNotifyListener;
	private SquareVideoInfo videoInfo;
	
	public interface OnDataNotifyListener {
		public void onDataNotify(boolean result);
	}
	
	public CollectTask(Context context, SquareVideoInfo squareVideoInfo, OnDataNotifyListener onDataNotifyListener) {
		super(context);
		this.context = context;
		this.onDataNotifyListener = onDataNotifyListener;
		this.videoInfo = squareVideoInfo;
	}

	
	@Override
	protected Boolean realDoInBackground(Void... params) throws BaseException {
		boolean result = false;
		if (videoInfo.isCollected()) {
			result = EzvizAPI.getInstance().cancelSquareVideoFavorite(videoInfo.getFavoriteId());
		} else {
			result = EzvizAPI.getInstance().favoriteSquareVideo(videoInfo.getSquareId());
		}
		return result;
	}

	@Override
	protected void realOnPostExecute(Boolean result) {
		onDataNotifyListener.onDataNotify(result);
	}
	
	@Override
	protected void onError(int mErrorCode) {
		onDataNotifyListener.onDataNotify(false);
	}
}
