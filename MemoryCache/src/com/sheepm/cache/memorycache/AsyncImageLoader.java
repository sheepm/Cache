package com.sheepm.cache.memorycache;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sheepm.cache.disklrucache.DiskLruCache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
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
	//硬盘缓存
	private File cacheDir;
	public DiskLruCache diskCache;
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
		
		try {
			//获取文件缓存路径
			cacheDir = getDiskCacheDir(context, "bitmap");
			if (!cacheDir.exists()) {
				cacheDir.mkdir();
			}
			//创建DiskLruCache实例，初始化硬盘缓存
			diskCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, 20*1024*1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		loadBitmaps(view, imageView, imgUrl, 0, 0);
	}
	
	public void loadBitmaps(View view,ImageView imageView,String imageUrl,int reqWidth,int reqHeight){
		if (imageView != null && loadingBitmap != null) {
			imageView.setImageBitmap(loadingBitmap);
		}
		Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
		if (bitmap == null) {
			BitmapWorkerTask task = new BitmapWorkerTask(imageLoader,view,reqWidth,reqHeight);
			taskCollection.add(task);
			task.executeOnExecutor(executor, imageUrl);
		}else {
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

	/**
	 * 根据传入的unique返回硬盘缓存地址
	 * 
	 * @param context
	 * @param uniqueName
	 * @return
	 */
	public File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			//当有SD卡时
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			//当没有SD卡或SD卡被移除
			cachePath = context.getCacheDir().getPath();
		}

		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * 获取当前应用程序版本号
	 * 
	 * @param context
	 * @return
	 */
	public int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	/**
	 * 使用MD5算法对传入的key进行加密，以免出现url不合法
	 * @param key
	 * @return
	 */
	public String hashKeyForDisk(String key){
		String cacheKey;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(key.getBytes());
			cacheKey = bytesToHexString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
			e.printStackTrace();
		}
		return cacheKey;
	}
	
	private String bytesToHexString(byte[] bytes){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				builder.append("0");
			}
			builder.append(hex);
		}
		return builder.toString();
	}
	
	/**
	 * 返回当前缓存的大小，以byte为单位
	 * @return
	 */
	public long getDiskCache(){
		if (diskCache != null) {
			return diskCache.size();
		}
		
		return 0;
	}
	
	/**
	 * 将缓存记录同步到journal文件中
	 */
	public void flushCache(){
		if (diskCache != null) {
			try {
				diskCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 清空磁盘缓存
	 */
	public void clearCache(){
		if (diskCache != null) {
			try {
				diskCache.delete();
				diskCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, 20*1024*1024);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}

	
}
