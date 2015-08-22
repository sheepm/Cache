package com.sheepm.cache.memorycache;

import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

/**
 * 异步加载图片
 * 
 * @author sheepm
 * 
 */
public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

	private AsyncImageLoader imageLoader;
	// 显示图片控件所在的视图
	private View view;
	// 图片Url地址
	protected String imageUrl;

	public BitmapWorkerTask(AsyncImageLoader imageLoader, View view) {
		this.imageLoader = imageLoader;
		this.view = view;
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		imageUrl = params[0];
		// 通过url下载图片
		Bitmap bitmap = downloadBitmap(params[0]);
		if (bitmap != null) {
			// 将图片放入内存缓存中
			imageLoader.addBitmapToMemoryCache(params[0], bitmap);
		}

		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		// 通过tag返回一个imageview对象
		ImageView imageView = (ImageView) view.findViewWithTag(imageUrl);
		if (imageView != null) {
			if (result != null) {
				// 加载成功
				imageView.setImageBitmap(result);
			} else {
				// 加载失败
				if (imageLoader.loadfailBitmap != null) {
					imageView.setImageBitmap(imageLoader.loadfailBitmap);
				}
			}
		}
		imageLoader.taskCollection.remove(this);
	}

	/**
	 * 通过http协议，根据url返回bitmap对象
	 * 
	 * @param imgUrl
	 * @return
	 */
	private Bitmap downloadBitmap(String imgUrl) {
		Bitmap bitmap = null;
		HttpURLConnection conn = null;
		try {
			URL url = new URL(imgUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(6 * 1000);
			conn.setReadTimeout(10 * 1000);
			bitmap = BitmapFactory.decodeStream(conn.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return bitmap;
	}

}
