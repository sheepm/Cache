package com.example.memorycache;

import java.util.ArrayList;
import java.util.List;

import com.sheepm.cache.memorycache.AsyncImageLoader;

import android.os.Bundle;
import android.widget.ListView;
import android.app.Activity;

public class MainActivity extends Activity {

	private ListView listView;

	private List<String> data;

	private ListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
	}

	private void initView() {
		AsyncImageLoader.getInstance(this).setLoadingDrawable(
				R.drawable.loading);
		AsyncImageLoader.getInstance(this).setFailDrawable(
				R.drawable.ic_launcher);
		listView = (ListView) findViewById(R.id.listview);
		data = new ArrayList<String>();
		data.add(Constants.url1);
		data.add(Constants.url2);
		data.add(Constants.url3);
		data.add(Constants.url4);
		data.add(Constants.url5);
		data.add(Constants.url6);
		data.add(Constants.url7);
		data.add(Constants.url8);
		data.add(Constants.url9);
		data.add(Constants.url10);
		data.add(Constants.url11);
		data.add(Constants.url12);
		data.add(Constants.url13);
		data.add(Constants.url14);
		data.add(Constants.url15);
		data.add(Constants.url16);
		data.add(Constants.url17);
		data.add(Constants.url18);
		data.add(Constants.url19);
		data.add(Constants.url20);
		data.add(Constants.url21);
		data.add(Constants.url22);
		data.add(Constants.url23);
		data.add(Constants.url24);
		data.add(Constants.url25);
		data.add(Constants.url26);
		data.add(Constants.url27);
		data.add(Constants.url28);
		
		adapter = new ListAdapter(this, data, listView);
		listView.setAdapter(adapter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//在调用onPause方法，将缓存记录同步到journal文件中
		AsyncImageLoader.getInstance(this).flushCache();
	}

}
