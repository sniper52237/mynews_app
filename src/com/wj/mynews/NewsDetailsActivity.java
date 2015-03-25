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
		
		//Ϊ��һ�����ź���һ�����Ű�ť���Զ���ļ�����
		NewsDetailsListener newsDetailsListener = new NewsDetailsListener();
		newsdetailsTitlebarPrevious.setOnClickListener(newsDetailsListener);
		newsdetailsTitlebarNext.setOnClickListener(newsDetailsListener);
		newsdetailsTitlebarComments.setOnClickListener(newsDetailsListener);
		newsReplyImgBtn.setOnClickListener(newsDetailsListener);
		mNewsReplyPost.setOnClickListener(newsDetailsListener);
		
		//��ȡ���ݵ���Ϣ
		Intent intent = getIntent();
		String categoryName  = intent.getStringExtra("categoryTitle");
		//���ñ���������
		TextView titlebarTitle = (TextView) findViewById(R.id.newsdetails_titlebar_title);
		titlebarTitle.setText(categoryName);
		//Position
		mCursor = mPosition = intent.getIntExtra("position", 0);
		//newsData
		mNewsData =  (ArrayList<HashMap<String, Object>>) intent.getSerializableExtra("newsData");
		
		//����ViewFlipper�еĲ���
		newsBodyInflater = getLayoutInflater();
		
		inflateView(0);
		
	}

	private void inflateView(int index)
	{
		//��һ�����ż��ز���
		View newsBodyView = newsBodyInflater.inflate(R.layout.news_body, null);
		//��ȡ������Ż�����Ϣ
		HashMap<String, Object> hashMap = mNewsData.get(mPosition);
		//mNid
		mNid = (Integer) hashMap.get("nid");
		TextView newsTitle = (TextView) newsBodyView.findViewById(R.id.news_body_title);
		//�������ű���
		newsTitle.setText(hashMap.get("newslist_item_title").toString());
		TextView newsPtimeAndSource = (TextView) newsBodyView.findViewById(R.id.news_body_ptime_source);
		//����������Դ�ͳ���
		newsPtimeAndSource.setText(hashMap.get("newslist_item_source").toString()+"  "+hashMap.get("newslist_item_ptime").toString());
		//����
		newsdetailsTitlebarComments.setText(hashMap.get("newslist_item_comments")+"����");
		
		newsBodyFlipper = (ViewFlipper) findViewById(R.id.news_body_flipper);
		newsBodyFlipper.addView(newsBodyView,index);
		
		//ΪnewsDetails�����һ����ļ�����
		newsDetails = (TextView) newsBodyView.findViewById(R.id.news_body_details);
		newsDetails.setOnTouchListener(newsBodyonOnTouchListener);
		
		// �����߳�
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
				Toast.makeText(NewsDetailsActivity.this, "����ɹ�", Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(NewsDetailsActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
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
			params.add(new Parameter("region", "�Ƹ�ʦ��ѧԺ"));
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
					msg.arg1 = 1;//����ɹ�
					postHandler.sendMessage(msg);
					return;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			msg.arg1 = 2;//����ʧ��
			postHandler.sendMessage(msg);
		}
		
	}
	
	private String getNewsBody()
	{
		String retStr = "��������ʧ�ܣ����Ժ�����";
		SyncHttp syncHttp = new SyncHttp();
		String url = "http://192.168.36.1:8080/web/getNews";
		String params = "nid=" + mNid;
		try
		{
			String retString = syncHttp.httpGet(url,params);
			JSONObject jsonObject = new JSONObject(retString);
			//��ȡ������  0��ʾ�ɹ�
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
				//ʹ�����ظ����ý���
				newsReplyEdittext.requestFocus();
				//�Զ��������뷨
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
			//������һ������
			newsBodyFlipper.setInAnimation(this, R.anim.push_right_in); //������һ������ʱ�Ķ���
			newsBodyFlipper.setOutAnimation(this, R.anim.push_right_out); //���嵱ǰҳ����ʱ�Ķ���
			
			mPosition--;
			
			//��¼��ǰ���ű��
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			if (mCursor > mPosition)
			{
				mCursor = mPosition;
				inflateView(0);
				System.out.println(newsBodyFlipper.getChildCount());
				newsBodyFlipper.showNext();// ��ʾ��һҳ
			}
			//��ʾ��һҳ
			newsBodyFlipper.showPrevious();
		}
		else
		{
			Toast.makeText(this, "û����һ��������", Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}
	
	public void showNext()
	{
		//�ж��Ƿ������һƪ����
		if (mPosition < mNewsData.size()-1)
		{
			//������һ������
			newsBodyFlipper.setInAnimation(this, R.anim.push_left_in); //������һ������ʱ�Ķ���
			newsBodyFlipper.setOutAnimation(this, R.anim.push_left_out); //���嵱ǰҳ����ʱ�Ķ���
			mPosition++;
			
			//��¼��ǰ���ű��
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			
			//�ж���һҳ�Ƿ��Ѿ�����
			if (mPosition >= newsBodyFlipper.getChildCount())
			{
				inflateView(newsBodyFlipper.getChildCount());
			}
			//��ʾ��һ��
			newsBodyFlipper.showNext();
		}
		else
		{
			Toast.makeText(this, "û����һ��������", Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}
	
	//������һ����ļ�����
	private OnTouchListener newsBodyonOnTouchListener = new OnTouchListener()
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			switch (event.getAction())
			{
			//��ָ����
			case MotionEvent.ACTION_DOWN:
				//��¼��ʼ����
				startX = event.getX();
				mNewsReplyImgLayout.setVisibility(View.VISIBLE);
				mNewsReplyEditLayout.setVisibility(View.GONE);
				break;
			//��ָ̧��
			case MotionEvent.ACTION_UP:
				//��ָ����
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
