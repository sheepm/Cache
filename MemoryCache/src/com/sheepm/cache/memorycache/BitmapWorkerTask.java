package com.sheepm.cache.memorycache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.sheepm.cache.disklrucache.BitmapUtil;
import com.sheepm.cache.disklrucache.DiskLruCache;
import com.sheepm.cache.disklrucache.DiskLruCache.Snapshot;

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
	//生成图片的宽度
	private int reqWidth;
	//生成图片的高度
	private int reqHeight;
	

	public BitmapWorkerTask(AsyncImageLoader imageLoader, View view,int reqWidth,int reqHeight) {
		this.imageLoader = imageLoader;
		this.view = view;
		this.reqWidth = reqWidth;
		this.reqHeight = reqHeight;
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		imageUrl = params[0];
		FileDescriptor descriptor = null;
		FileInputStream inputStream = null;
		
		Snapshot snapshot = null;
		
		try {
			//生成图片对应的key
			String key = imageLoader.hashKeyForDisk(imageUrl);
			snapshot = imageLoader.diskCache.get(key);
			
			if (snapshot == null) {
				//在磁盘中没有找到缓存文件，去网络下载，并写入到缓存中
				DiskLruCache.Editor editor = imageLoader.diskCache.edit(key);
				if (editor != null) {
					OutputStream outputStream = editor.newOutputStream(0); //因为是1对1，直接设为0，就是取第一个
					if (downloadUrlToStream(imageUrl, outputStream)) {
						editor.commit();
					}else {
						editor.abort();  //取消本次写入操作
					}
				}
				snapshot = imageLoader.diskCache.get(key);
			}
			if (snapshot != null) {
				inputStream = (FileInputStream) snapshot.getInputStream(0);
				descriptor = inputStream.getFD();
			}
			//将缓存数据解析成bitmap对象
			Bitmap bitmap = null;
			if (descriptor != null) {
				bitmap = BitmapUtil.decodeSampledBitmap(descriptor, reqWidth, reqHeight);
			}
			if (bitmap != null) {
				//将bitmap加入到内存缓存中
				imageLoader.addBitmapToMemoryCache(params[0], bitmap);
			}
			return bitmap;
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	/**
	 * 根据url从网上获取流，并写入到output流中
	 * @param imageUrl
	 * @param outputStream
	 * @return
	 */
	private boolean downloadUrlToStream(String imageUrl,OutputStream outputStream){
		HttpURLConnection conn = null;
		BufferedOutputStream out = null;
		BufferedInputStream  in = null;
		
		try {
			URL url = new URL(imageUrl);
			conn = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(conn.getInputStream(), 8*1024);
			out = new BufferedOutputStream(outputStream, 8*1024);
			
			int b;
			while((b = in.read())!= -1){
				out.write(b);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if (conn != null) {
				conn.disconnect();
			}
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

}
