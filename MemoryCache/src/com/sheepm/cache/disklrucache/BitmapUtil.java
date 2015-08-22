package com.sheepm.cache.disklrucache;

import java.io.FileDescriptor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtil {
	
	/**
	 * 计算目标图片缩放比例
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
		int height = options.outHeight;
		int width = options.outWidth;
		int inSampleSize = 1;
		
		if (height > reqHeight || width > reqWidth) {
			//计算高度和宽度对目标高宽的比例
			int heightRadio = Math.round(height/reqHeight);
			int widthRadio = Math.round(width/reqWidth);
			//选择高宽中较小的一个作为压缩比例，保证图片比目标尺寸大
			inSampleSize = heightRadio < widthRadio ? heightRadio : widthRadio;
		}
		return inSampleSize;
	}
	
	public static Bitmap decodeSampledBitmap(FileDescriptor descriptor,int reqWidth,int reqHeight){
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		//加载时将injustdecodebounds设置为true，获取图片大小
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(descriptor, null, options);
		//计算压缩比例
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight); 
		options.inJustDecodeBounds = false;
		
		return BitmapFactory.decodeFileDescriptor(descriptor, null, options);
	}

}
