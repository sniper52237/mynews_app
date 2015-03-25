package com.wj.mynews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.wj.mynews.service.SyncHttp;

public class CommentsActivity extends Activity
{
	private List<HashMap<String, Object>> commsData;
	private ListView commentsList;
	private int mNid;
	private Button commentsTitlebarNews;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comments);
		
		commentsTitlebarNews = (Button) findViewById(R.id.comments_titlebar_news);
		commentsTitlebarNews.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});
		
		commsData = new ArrayList<HashMap<String,Object>>();
		
		//获取传递过来的新闻编号
		Intent intent = getIntent();
		mNid = intent.getIntExtra("nid", 0); 
		
		commentsList = (ListView) findViewById(R.id.comments_list);
		//开启新线程获取数据
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Looper.prepare();
				getComments(mNid);
			}
		}).start();
		
		SimpleAdapter sa = new SimpleAdapter(this, commsData, R.layout.comments_list_item, 
				new String[]{"commentator_from", "comment_ptime", "comment_content"}, 
				new int[]{R.id.commentator_from, R.id.comment_ptime, R.id.comment_content});
		
		commentsList.setAdapter(sa);
	}
	
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.arg1)
			{
			case 1:
				Toast.makeText(CommentsActivity.this, "暂无回复", Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(CommentsActivity.this, "获取评论失败", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	private void getComments(int nid)
	{
		String url = "http://192.168.36.1:8080/web/getComments";
		String params = "nid=" + nid + "&startnid=0&count=10";
		SyncHttp syncHttp = new SyncHttp();
		Message msg = handler.obtainMessage();
		try
		{
			//以Get方式请求，并获取返回结果
			String retStr = syncHttp.httpGet(url, params);
			JSONObject jsonObject = new JSONObject(retStr);
			int retCode = jsonObject.getInt("ret");
			if (retCode == 0)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				//获取返回数目
				int totalnum = dataObject.getInt("totalnum");
				if (totalnum > 0)
				{
					JSONArray commsList = dataObject.getJSONArray("commentslist");
					for (int i=0; i<commsList.length(); i++)
					{
						JSONObject commsObject = (JSONObject) commsList.opt(i);
						HashMap<String, Object> hashMap = new HashMap<String, Object>();
						hashMap.put("cid", commsObject.getInt("cid"));
						hashMap.put("commentator_from", commsObject.getString("region"));
						hashMap.put("comment_ptime", commsObject.getString("ptime"));
						hashMap.put("comment_content", commsObject.getString("content"));
						commsData.add(hashMap);
					}
				}
				else
				{
					msg.arg1 = 1; //暂无回复
					handler.sendMessage(msg);
				}
			}
			else
			{
				msg.arg1 = 2; //获取评论失败
				handler.sendMessage(msg);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			msg.arg1 = 2;
			handler.sendMessage(msg);
		}
	}
	

}
