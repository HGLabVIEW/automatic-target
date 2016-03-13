package com.videogo.ui.discovery;
import android.graphics.drawable.RotateDrawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.TextView;

public class CollectRotateAnimation extends Animation {
	private RotateDrawable rotateDrawable;
	
	public CollectRotateAnimation(RotateDrawable rotateDrawable, long duration) {
		setInterpolator(new LinearInterpolator());
		setRepeatCount(Animation.INFINITE);
		setDuration(duration);
		this.rotateDrawable = rotateDrawable;
	}
	
	@Override
	public void start() {
		if (rotateDrawable == null) {
			return ;
		} else {
			super.start();
		}
	}
	
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		super.applyTransformation(interpolatedTime, t);
		float level = 10000 * interpolatedTime;
		rotateDrawable.setLevel((int) level);
	}
}