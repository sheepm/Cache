package com.sheepm.cache.memorycache;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

public class AsyncImageLoader {

	private static AsyncImageLoader imageLoader;

	private Context context;
	// 异步任务执行者
	private Executor executor;
	// 加载任务的集合
	public Set<BitmapWorkerTask> taskCollection;
	// 内存缓存
	public LruMemoryCache memoryCache;
	// 加载中显示的bitmap
	public Bitmap loadingBitmap;
	// 加载完成显示的Bitmap
	public Bitmap loadfailBitmap;

	public static AsyncImageLoader getInstance(Context context) {
		if (imageLoader == null) {
			imageLoader = new AsyncImageLoader(context);
		}
		return imageLoader;
	}

	public AsyncImageLoader(Context context) {
		this.context = context;
		// 初始化线程池
		executor = new ThreadPoolExecutor(3, 200, 10, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		// 初始化任务集合
		taskCollection = new HashSet<BitmapWorkerTask>();
		// 获取应用程序最大可用内存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 4;
		// 设置内存缓存为最大可用内存的四分之一
		memoryCache = new LruMemoryCache(cacheSize);
	}

	// 设置加载中的图片
	public void setLoadingDrawable(int resourceId) {
		loadingBitmap = BitmapFactory.decodeResource(context.getResources(),
				resourceId);
	}

	// 设置加载失败的图片
	public void setFailDrawable(int resourceId) {
		loadfailBitmap = BitmapFactory.decodeResource(context.getResources(),
				resourceId);
	}

	/**
	 * 加载图片，先是加载中图片，如果内存中没有再加载网络图片
	 * 
	 * @param view
	 * @param imageView
	 * @param imgUrl
	 */
	public void loadBitmaps(View view, ImageView imageView, String imgUrl) {
		if (imageView != null && loadingBitmap != null) {
			imageView.setImageBitmap(loadingBitmap);
		}
		Bitmap bitmap = getBitmapFromMemoryCache(imgUrl);
		if (bitmap == null) {
			BitmapWorkerTask task = new BitmapWorkerTask(imageLoader, view);
			taskCollection.add(task);
			task.executeOnExecutor(executor, imgUrl);
		} else {
			if (imageView != null && bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
		}
	}

	/**
	 * 设置图片到内存缓存中
	 * 
	 * @param key
	 * @param value
	 */
	public void addBitmapToMemoryCache(String key, Bitmap value) {

		if (getBitmapFromMemoryCache(key) == null) {
			memoryCache.put(key, value);
		}
	}

	/**
	 * 根据key从memorycache中取图片
	 * 
	 * @param key
	 * @return
	 */
	public Bitmap getBitmapFromMemoryCache(String key) {
		return memoryCache.get(key);
	}

	/**
	 * 取消所有正在下载或等待下载的任务
	 */
	public void cancelAllTask() {
		if (taskCollection != null) {
			for (BitmapWorkerTask task : taskCollection) {
				task.cancel(false);
			}
		}
	}
}
