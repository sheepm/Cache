package com.example.memorycache;

import java.util.List;

import com.sheepm.cache.memorycache.AsyncImageLoader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class ListAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private List<String> mdatas;
	private ListView mListView;
	private Context mContext;

	public ListAdapter(Context context, List<String> data, ListView listView) {
		this.mContext = context;
		mInflater = LayoutInflater.from(context);
		this.mdatas = data;
		this.mListView = listView;
	}

	@Override
	public int getCount() {
		return mdatas.size();
	}

	@Override
	public Object getItem(int position) {
		return mdatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.list_item, null);
			viewHolder.imageView = (ImageView) convertView
					.findViewById(R.id.imageview);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		// 给imageview设置一个tag,是加载时不会乱序
		viewHolder.imageView.setTag(mdatas.get(position));
		// 开启异步线程加载图片
		AsyncImageLoader.getInstance(mContext).loadBitmaps(mListView,
				viewHolder.imageView, mdatas.get(position));
		return convertView;
	}

}

class ViewHolder {

	ImageView imageView;
}
