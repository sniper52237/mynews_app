package com.wj.mynews;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import com.wj.mynews.model.Parameter;
import com.wj.mynews.service.SyncHttp;

public class NewsDetailsActivity extends Activity
{
	private ViewFlipper newsBodyFlipper;
	private LayoutInflater newsBodyInflater;
	private Button newsdetailsTitlebarPrevious;
	private Button newsdetailsTitlebarNext;
	private Button newsdetailsTitlebarComments;
	private ArrayList<HashMap<String, Object>> mNewsData;
	private TextView newsDetails;
	private LinearLayout mNewsReplyEditLayout;
	private LinearLayout mNewsReplyImgLayout;
	private EditText newsReplyEdittext;
	
	public int count = 0;
	private float startX;
	private int mPosition;
	private int mCursor;
	private int mNid;
	
	
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			newsDetails.setText(Html.fromHtml(msg.obj.toString()));
		}
	};
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newsdetails);
		
		mNewsReplyEditLayout = (LinearLayout) findViewById(R.id.news_reply_edit_layout);
		mNewsReplyImgLayout = (LinearLayout) findViewById(R.id.news_reply_img_layout);
		Button mNewsReplyPost = (Button) findViewById(R.id.news_reply_post);
		ImageButton newsReplyImgBtn = (ImageButton) findViewById(R.id.news_reply_img_btn);
		newsdetailsTitlebarPrevious = (Button) findViewById(R.id.newsdetails_titlebar_previous);
		newsdetailsTitlebarNext = (Button) findViewById(R.id.newsdetails_titlebar_next);
		newsdetailsTitlebarComments = (Button) findViewById(R.id.newsdetails_titlebar_comments);
		newsReplyEdittext = (EditText) findViewById(R.id.news_reply_edittext);
		
		//为上一条新闻和下一条新闻按钮绑定自定义的监听器
		NewsDetailsListener newsDetailsListener = new NewsDetailsListener();
		newsdetailsTitlebarPrevious.setOnClickListener(newsDetailsListener);
		newsdetailsTitlebarNext.setOnClickListener(newsDetailsListener);
		newsdetailsTitlebarComments.setOnClickListener(newsDetailsListener);
		newsReplyImgBtn.setOnClickListener(newsDetailsListener);
		mNewsReplyPost.setOnClickListener(newsDetailsListener);
		
		//获取传递的信息
		Intent intent = getIntent();
		String categoryName  = intent.getStringExtra("categoryTitle");
		//设置标题栏名称
		TextView titlebarTitle = (TextView) findViewById(R.id.newsdetails_titlebar_title);
		titlebarTitle.setText(categoryName);
		//Position
		mCursor = mPosition = intent.getIntExtra("position", 0);
		//newsData
		mNewsData =  (ArrayList<HashMap<String, Object>>) intent.getSerializableExtra("newsData");
		
		//加载ViewFlipper中的布局
		newsBodyInflater = getLayoutInflater();
		
		inflateView(0);
		
	}

	private void inflateView(int index)
	{
		//第一条新闻加载布局
		View newsBodyView = newsBodyInflater.inflate(R.layout.news_body, null);
		//获取点击新闻基本信息
		HashMap<String, Object> hashMap = mNewsData.get(mPosition);
		//mNid
		mNid = (Integer) hashMap.get("nid");
		TextView newsTitle = (TextView) newsBodyView.findViewById(R.id.news_body_title);
		//设置新闻标题
		newsTitle.setText(hashMap.get("newslist_item_title").toString());
		TextView newsPtimeAndSource = (TextView) newsBodyView.findViewById(R.id.news_body_ptime_source);
		//设置新闻来源和出处
		newsPtimeAndSource.setText(hashMap.get("newslist_item_source").toString()+"  "+hashMap.get("newslist_item_ptime").toString());
		//跟帖
		newsdetailsTitlebarComments.setText(hashMap.get("newslist_item_comments")+"跟帖");
		
		newsBodyFlipper = (ViewFlipper) findViewById(R.id.news_body_flipper);
		newsBodyFlipper.addView(newsBodyView,index);
		
		//为newsDetails绑定左右滑动的监听器
		newsDetails = (TextView) newsBodyView.findViewById(R.id.news_body_details);
		newsDetails.setOnTouchListener(newsBodyonOnTouchListener);
		
		// 启动线程
	    new UpdateNewsThread().start();
	}
	
	private class UpdateNewsThread extends Thread
	{
		@Override
		public void run()
		{
			String newsBody = getNewsBody();
			Message msg = handler.obtainMessage();
			msg.obj = newsBody;
			handler.sendMessage(msg);
		}
	}
	
	private Handler postHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.arg1)
			{
			case 1:
				Toast.makeText(NewsDetailsActivity.this, "发表成功", Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(NewsDetailsActivity.this, "发表失败", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	
	private class PostCommentThread extends Thread
	{
		@Override
		public void run()
		{
			Looper.prepare();
			String url = "http://192.168.36.1:8080/web/postComment";
			List<Parameter> params = new ArrayList<Parameter>();
			params.add(new Parameter("nid", mNid+""));
			params.add(new Parameter("region", "黄冈师范学院"));
			params.add(new Parameter("content", newsReplyEdittext.getText().toString()));
			SyncHttp syncHttp = new SyncHttp();
			Message msg = postHandler.obtainMessage();
			try
			{
				String retStr = syncHttp.httpPost(url, params);
				JSONObject object = new JSONObject(retStr);
				int retCode = object.getInt("ret");
				if (retCode == 0)
				{
					msg.arg1 = 1;//发表成功
					postHandler.sendMessage(msg);
					return;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			msg.arg1 = 2;//发表失败
			postHandler.sendMessage(msg);
		}
		
	}
	
	private String getNewsBody()
	{
		String retStr = "网络连接失败，请稍后再试";
		SyncHttp syncHttp = new SyncHttp();
		String url = "http://192.168.36.1:8080/web/getNews";
		String params = "nid=" + mNid;
		try
		{
			String retString = syncHttp.httpGet(url,params);
			JSONObject jsonObject = new JSONObject(retString);
			//获取返回码  0表示成功
			int retCode = jsonObject.getInt("ret");
			if (retCode == 0)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				JSONObject newsObject = dataObject.getJSONObject("news");
				retStr = newsObject.getString("body");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return retStr;
	}
	
	
	
	private class NewsDetailsListener implements OnClickListener
	{
		Intent intent;
		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
			case R.id.newsdetails_titlebar_previous:
				showPrevious();
				break;
			case R.id.newsdetails_titlebar_next:
				showNext();
				break;
			case R.id.newsdetails_titlebar_comments:
				intent = new Intent();
				intent.setClass(NewsDetailsActivity.this, CommentsActivity.class);
				intent.putExtra("nid", mNid);
				startActivity(intent);
				break;
			case R.id.news_reply_img_btn:
				mNewsReplyImgLayout.setVisibility(View.GONE);
				mNewsReplyEditLayout.setVisibility(View.VISIBLE);
				//使跟帖回复框获得焦点
				newsReplyEdittext.requestFocus();
				//自动弹出输入法
//				InputMethodManager m =  (InputMethodManager) newsReplyEdittext.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//				m.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);
				break;
			case R.id.news_reply_post:
				new PostCommentThread().start();
				mNewsReplyImgLayout.setVisibility(View.VISIBLE);
				mNewsReplyEditLayout.setVisibility(View.GONE);
			}
		}
	}
	
	
	public void showPrevious()
	{
		if (mPosition > 0)
		{
			//设置上一屏动画
			newsBodyFlipper.setInAnimation(this, R.anim.push_right_in); //定义上一屏进来时的动画
			newsBodyFlipper.setOutAnimation(this, R.anim.push_right_out); //定义当前页滑出时的动画
			
			mPosition--;
			
			//记录当前新闻编号
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			if (mCursor > mPosition)
			{
				mCursor = mPosition;
				inflateView(0);
				System.out.println(newsBodyFlipper.getChildCount());
				newsBodyFlipper.showNext();// 显示下一页
			}
			//显示上一页
			newsBodyFlipper.showPrevious();
		}
		else
		{
			Toast.makeText(this, "没有上一条新闻了", Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}
	
	public void showNext()
	{
		//判断是否是最后一篇新闻
		if (mPosition < mNewsData.size()-1)
		{
			//设置下一屏动画
			newsBodyFlipper.setInAnimation(this, R.anim.push_left_in); //定义下一屏进来时的动画
			newsBodyFlipper.setOutAnimation(this, R.anim.push_left_out); //定义当前页滑出时的动画
			mPosition++;
			
			//记录当前新闻编号
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			
			//判断下一页是否已经创建
			if (mPosition >= newsBodyFlipper.getChildCount())
			{
				inflateView(newsBodyFlipper.getChildCount());
			}
			//显示下一屏
			newsBodyFlipper.showNext();
		}
		else
		{
			Toast.makeText(this, "没有下一条新闻了", Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}
	
	//添加左右滑动的监听器
	private OnTouchListener newsBodyonOnTouchListener = new OnTouchListener()
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			switch (event.getAction())
			{
			//手指按下
			case MotionEvent.ACTION_DOWN:
				//记录起始坐标
				startX = event.getX();
				mNewsReplyImgLayout.setVisibility(View.VISIBLE);
				mNewsReplyEditLayout.setVisibility(View.GONE);
				break;
			//手指抬起
			case MotionEvent.ACTION_UP:
				//手指向左滑
				if (event.getX() < startX)
				{
					showNext();
				}
				else if (event.getX() > startX)
				{
					showPrevious();
				}
				break;
			}
			return true;
		}
	};

}
