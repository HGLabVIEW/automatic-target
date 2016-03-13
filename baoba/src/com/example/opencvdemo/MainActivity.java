package com.example.opencvdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Subdiv2D;
import org.opencv.videoio.VideoCapture;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends Activity implements OnClickListener {
	ImageView img1;
	ImageView img2;
	ImageView img3;
	ImageView imgsecond;
	Mat mat1;
	Mat mat11;
	Mat mat2;
	Mat mat21;
	Mat mat3;
	Mat circles;
	Mat lines;
	Point centerPoint;
	Point linePoint;
	Point linePoint2;
	Double radius;
	
	double linex;
	double liney;
	double linex2;
	double liney2;
	double score;
	Bitmap bitmap1;
	Bitmap bitmap2;
	Bitmap bitmap3;
	Bitmap bitmapsecond;
	InputStream is1;
	InputStream is2;
	Button btn;
	Button btn1;
	Button btndif;
	Button btnlines;
	Button btncompute;
	TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		img1 = (ImageView) findViewById(R.id.imageView1);
		img2 = (ImageView) findViewById(R.id.imageView2);
		img3 = (ImageView) findViewById(R.id.imageView3);
		imgsecond = (ImageView) findViewById(R.id.imageViewsecond);
		textView = (TextView) findViewById(R.id.text);
		btn = (Button) findViewById(R.id.houghcircles);
		btn1 = (Button) findViewById(R.id.cannybtn);
		btndif = (Button) findViewById(R.id.difbtn);
		btnlines  = (Button) findViewById(R.id.houghlines);
		btncompute = (Button) findViewById(R.id.compute);
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if (status == LoaderCallbackInterface.SUCCESS) {
				// now we can call opencv code !
				final Mat mat1 = new Mat();
				final Mat mat1canny = new Mat();
				final Mat mat2 = new Mat();
				final Mat mat2canny = new Mat();
				final Mat mat3 = new Mat();
				final Mat mat3canny = new Mat();
				final Mat matsec = new Mat();
				final Mat matseccanny = new Mat();
				bitmap1 = drawableToBitmap(img1.getDrawable());
				bitmap2 = drawableToBitmap(img2.getDrawable());
				bitmapsecond= drawableToBitmap(imgsecond.getDrawable());
				bitmap3=Bitmap.createBitmap(bitmap1.getWidth(), bitmap1.getHeight(),Config.ARGB_8888);
				Utils.bitmapToMat(bitmap1, mat1);
				Utils.bitmapToMat(bitmap2, mat2);
				Utils.bitmapToMat(bitmapsecond, matsec);
				btn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						centerPoint = getCirclePoints(mat1canny);
						textView.setText("靶心坐标："+"("+Double.toString(centerPoint.x) + "，"
								+ Double.toString(centerPoint.y)+")\r\n"+"半径："+Integer.toString(radius.intValue()));
						
					}
				});
				btn1.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Imgproc.Canny(mat1, mat1canny, 50, 70);
						Imgproc.Canny(mat2, mat2canny, 50, 70);
						Imgproc.Canny(matsec, matseccanny, 50, 70);
						Utils.matToBitmap(mat1canny, bitmap1);
						Utils.matToBitmap(mat2canny, bitmap2);
						Utils.matToBitmap(matseccanny, bitmapsecond);
						img1.setImageBitmap(bitmap1);
						img2.setImageBitmap(bitmap2);
						imgsecond.setImageBitmap(bitmapsecond);
					}
				});
				btndif.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (mat3.empty()) {
							Toast.makeText(getApplicationContext(), "第一支箭",
								     Toast.LENGTH_SHORT).show();
							Core.absdiff(mat1, mat2, mat3);
							Imgproc.Canny(mat3, mat3canny, 70, 90);
							Utils.matToBitmap(mat3canny, bitmap3);
							img3.setImageBitmap(bitmap3);
						} else {
							Toast.makeText(getApplicationContext(), "第二支箭",
								     Toast.LENGTH_SHORT).show();
							Core.absdiff(mat2, matsec, mat3);
							Imgproc.Canny(mat3, mat3canny, 70, 90);
							Utils.matToBitmap(mat3canny, bitmap3);
							img3.setImageBitmap(bitmap3);

						}
						
					}
				});
				btnlines.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						
						lines = new Mat();
						Imgproc.HoughLinesP(mat3canny, lines, 1, Math.PI/180, 20, 1, 100);
						for (int i = 0; i < lines.cols(); i++){			
							double[] points = lines.get(0, i); 
							double x1 = points[0];
							double y1 = points[1];
							double x2 = points[2];
							double y2 = points[3];
							Point l1 = new Point(x1, y1);
							Point l2 = new Point(x2, y2);
							linex=x1;
							liney=y1;
							linex2=x2;
							liney2=y2;
						}
						textView.setText("端点1坐标："+"("+Double.toString(linex)+"，"+Double.toString(liney)+")\r\n"+
								"端点2坐标："+"("+Double.toString(linex2)+"，"+Double.toString(liney2)+")");
						if (liney>liney2) {
							linePoint=new Point(linex, liney);
						} else {
							linePoint=new Point(linex2, liney2);
						}
						
					}
					
				});
				btncompute.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
					score =	pointDistance(centerPoint, linePoint);
						textView.setText("成绩："+Integer.toString(judgescore(score))+"环");
						Toast.makeText(getApplicationContext(), "成绩："+Integer.toString(judgescore(score))+"环",
							     Toast.LENGTH_SHORT).show();
					}
					
				});
				
			} else {
				super.onManagerConnected(status);
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
				mLoaderCallback);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
				.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;

	}

	public Point getCirclePoints(Mat mat) {
		circles = new Mat();

		Imgproc.HoughCircles(mat, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 50, 100, 10, 10, 320);

		for (int i = 0; i < circles.cols(); i++) {
			double[] circleAttributes = circles.get(i, 0);
			if (circleAttributes == null) {
				continue;
			}
			radius = circleAttributes[2]/4;
			return new Point(circleAttributes[0], circleAttributes[1]);
		
		}
		return null;
		
	}
	private double pointDistance(Point p1, Point p2){
		return Math.sqrt(Math.pow(p1.x-p2.x, 2) + Math.pow(p1.y-p2.y, 2));
	}
	private int judgescore(Double score){
		int huanshu = 0;
		if (score<=radius) {
			huanshu=10;
		} else if (score>radius&&score<=2*radius) {
			huanshu=9;
		} else if (score>2*radius&&score<=3*radius) {
			huanshu=8;
		}else if (score>3*radius&&score<=4*radius) {
			huanshu=7;
		} else if (score>4*radius&&score<=5*radius) {
			huanshu=6;
		} else if (score>5*radius&&score<=6*radius) {
			huanshu=5;
		} else if (score>6*radius&&score<=7*radius) {
			huanshu=4;
		} else if (score>7*radius&&score<=8*radius) {
			huanshu=3;
		} else if (score>8*radius&&score<=9*radius) {
			huanshu=2;
		} else if (score>9*radius&&score<=10*radius) {
			huanshu=1;
		} else if (score>10*radius) {
			
		} 
	return huanshu;
	}

}
