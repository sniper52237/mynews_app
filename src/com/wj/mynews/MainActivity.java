package com.wj.mynews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.wj.mynews.custom.CustomSimpleAdapter;
import com.wj.mynews.model.Category;
import com.wj.mynews.service.SyncHttp;
import com.wj.mynews.utils.DensityUtils;
import com.wj.mynews.utils.StringUtil;

public class MainActivity extends Activity
{
	protected static final int GETNEWS = 1;

	private static final int NONEWS = 2;

	private static final int NEWSFAILD = 3;
	
	private final int COLUMNWIDTHDP = 55;//���嵼����һ��ѡ��Ŀ��
	private int newsCount = 5;
	private int mColumnWidthPX;
	private int mFlingWidth = 1000;
	private int mCid;
	private String  mCategoryTitle;
	
	private ArrayList<HashMap<String, Object>> newsData;
	private List<HashMap<String, Category>> categoriesList;
	private ListView newsListView;
	private LayoutInflater mInflater;
	private View footerView;
	private SimpleAdapter newsListAdapter;
	private Button mTitlebarRefresh;
	private ProgressBar mLoadnewsProgress;
	private Button mLoadMoreBtn;
	private LoadNewsAsyncTask mLoadNewsAsyncTask;
	
	
	
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if (msg.what == GETNEWS)
			{
				newsData = (ArrayList<HashMap<String, Object>>) msg.obj;
				//�����б�������
				newsListAdapter = new SimpleAdapter(MainActivity.this, newsData, R.layout.newslist_item, 
						new String[]{"newslist_item_title", "newslist_item_digest", "newslist_item_source", "newslist_item_ptime"}, 
						new int[]{R.id.newslist_item_title, R.id.newslist_item_digest, R.id.newslist_item_source, R.id.newslist_item_ptime});
				
				newsListView.setAdapter(newsListAdapter);
			}
			else if (msg.what == NONEWS)
			{
				Toast.makeText(MainActivity.this, "����Ŀ����ʱû������", Toast.LENGTH_SHORT).show();
			}
			else if (msg.what == NEWSFAILD)
			{
				Toast.makeText(MainActivity.this, "��ȡ����ʧ��", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private OnClickListener loadMoreListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			mLoadNewsAsyncTask = new LoadNewsAsyncTask();
			switch (v.getId())
			{
			case R.id.loadmore_btn:
//				getNewsData(mCid, 10);
//				newsListAdapter.notifyDataSetChanged();
				mLoadNewsAsyncTask.execute(mCid, 10);
				break;
			case R.id.refresh_btn:
				mLoadNewsAsyncTask.execute(mCid, 10);
				break;
			}
		}
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//�ҵ�ListView
		newsListView = (ListView) findViewById(R.id.newslist);
		mTitlebarRefresh = (Button) findViewById(R.id.refresh_btn);
		mLoadnewsProgress = (ProgressBar) findViewById(R.id.loadnews_progress);
		mTitlebarRefresh.setOnClickListener(loadMoreListener);
		
		//��ȡ���ּ�����
		mInflater = getLayoutInflater();
		footerView = mInflater.inflate(R.layout.loadmore, null);
		newsListView.addFooterView(footerView);

		//��ȡ���ŷ�������
		String[] categoryArray = getResources().getStringArray(R.array.category_array);

		//�����ŷ��ౣ����List��
		categoriesList = new ArrayList<HashMap<String, Category>>();
		for (int i = 0; i < categoryArray.length; i++)
		{
			String[] temp = categoryArray[i].split("[|]"); 
			if (temp.length == 2)
			{
				int cid = StringUtil.string2Int(temp[0]);
				String title = temp[1];
				Category type = new Category(cid, title);
				HashMap<String, Category> hashMap = new HashMap<String, Category>();
				hashMap.put("category_title", type);
				categoriesList.add(hashMap);
			}
		}

		// px��λת��Ϊdp
		mColumnWidthPX = DensityUtils.dp2px(this, COLUMNWIDTHDP);
		// ����GridView����������
		GridView categoryGridView = new GridView(this);
		categoryGridView.setColumnWidth(mColumnWidthPX);// ����ÿ����Ԫ��Ŀ��
		categoryGridView.setNumColumns(GridView.AUTO_FIT);// ��Ԫ�������
		categoryGridView.setGravity(Gravity.CENTER);// ���ö��뷽ʽ
		
		//���õ�Ԫ��ѡ���Ǳ���ɫΪ͸��������ѡ��ʱ�Ͳ���ʵ��ɫ����
		categoryGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		
		//���ݵ�Ԫ���Ⱥ���Ŀ����GridView�ܿ��
		int width = mColumnWidthPX * categoriesList.size();
		LayoutParams params = new LayoutParams(width, LayoutParams.MATCH_PARENT);
		
		//����categoryGridView��Ⱥ͸߶ȣ�����categoryGridView��һ����ʾ
		categoryGridView.setLayoutParams(params);
		
		//����Ĭ��ֵ
		mCid=1;
		mCategoryTitle="����";
		
		CustomSimpleAdapter categoryGridViewAdapter = new CustomSimpleAdapter(
				this, categoriesList, R.layout.category_title,
				new String[] { "category_title" }, new int[] { R.id.tv });

		categoryGridView.setAdapter(categoryGridViewAdapter);

		LinearLayout categoryGridViewList = (LinearLayout) findViewById(R.id.category_layout);
		categoryGridViewList.addView(categoryGridView);

		Button rightBtn = (Button) findViewById(R.id.right_btn);
		final HorizontalScrollView categoryScrollview = (HorizontalScrollView) findViewById(R.id.category_scrollview);

		rightBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//ˮƽ�����ľ���
				categoryScrollview.fling(DensityUtils.dp2px(MainActivity.this, mFlingWidth));
			}
		});
		
		//Ϊ������GridView���ü������ı�״̬
		categoryGridView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				TextView categoryTv;
				for (int i = 0; i < parent.getCount(); i++)
				{
					categoryTv = (TextView) parent.getChildAt(i);
					categoryTv.setTextColor(0xffadb2ad);
					categoryTv.setBackgroundResource(0);
				}
				categoryTv = (TextView) view;
				categoryTv.setTextColor(0xffffffff);
				categoryTv.setBackgroundResource(R.drawable.categorybar_item_background);
				//��ȡ��������ŷ�����
				mCid = categoriesList.get(position).get("category_title").getCid();
				//��ȡ��������ŷ������
				mCategoryTitle = categoriesList.get(position).get("category_title").getTitle();
				//getNewsData(mCid,newsCount);
				mLoadNewsAsyncTask = new LoadNewsAsyncTask();
				mLoadNewsAsyncTask.execute(mCid,newsCount);
			}
		});
		
		
		//�������ȡ�ļ�������
		getNewsData(1, newsCount);
		//ListView �󶨼�����
		newsListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, NewsDetailsActivity.class);
				intent.putExtra("categoryTitle", mCategoryTitle);
				intent.putExtra("newsData", newsData);
				intent.putExtra("position", position);
				startActivity(intent);
				System.out.println("position--->"+position);
			}
		});
		//Ϊ���ظ��ఴť��Ӽ�����
		mLoadMoreBtn = (Button) findViewById(R.id.loadmore_btn);
		mLoadMoreBtn.setOnClickListener(loadMoreListener);
	}

	private void getNewsData(int i,int j)
	{
		mCid = i;
		newsCount = j;
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Looper.prepare();
				newsData = getSpecCatNews(mCid, newsCount);
				Message msg = handler.obtainMessage();
				msg.what = GETNEWS;
				msg.obj = newsData;
				handler.sendMessage(msg);
				Looper.loop();
			}
		}).start();
	}
	
	//�õ�ָ�� ��ĳ�����ŵļ���
	private ArrayList<HashMap<String, Object>> getSpecCatNews(int cid,int newsCount)
	{
		ArrayList<HashMap<String, Object>> newsList = new ArrayList<HashMap<String,Object>>();
		String urlStr = "http://192.168.36.1:8080/web/getSpecifyCategoryNews";
		String params = "startnid=0&count=" + newsCount + "&cid=" + cid;
		SyncHttp syncHttp = new SyncHttp();
		try
		{
			//��get��ʽ���󣬲���ȡ�������Ϣ
			String retStr = syncHttp.httpGet(urlStr, params);
			JSONObject jsonObject = new JSONObject(retStr);
			//��ȡ������ 0��ʾ�ɹ�
			int retCode = jsonObject.getInt("ret");
			if (retCode == 0)
			{
				 JSONObject dataObject = jsonObject.getJSONObject("data");
				 //��ȡ������Ŀ
				 int totalnum = dataObject.getInt("totalnum");
				 if (totalnum > 0)
				 {
					 //��ȡ�������ż���
					 JSONArray newslist = dataObject.getJSONArray("newslist");
					 for (int i=0; i<newslist.length(); i++)
					 {
						 JSONObject newsObject = (JSONObject) newslist.opt(i);
						 HashMap<String, Object> hashMap = new HashMap<String, Object>(); 
						 hashMap.put("nid", newsObject.getInt("nid"));
						 hashMap.put("newslist_item_title", newsObject.getString("title"));
						 hashMap.put("newslist_item_digest", newsObject.getString("digest"));
						 hashMap.put("newslist_item_source", newsObject.getString("source"));
						 hashMap.put("newslist_item_ptime", newsObject.getString("ptime"));
						 hashMap.put("newslist_item_comments", newsObject.get("commentcount"));
						 newsList.add(hashMap);
					 }
				 }
				 else
				 {
					 Message msg = handler.obtainMessage();
					 msg.what = NONEWS;
					 handler.sendMessage(msg);
				 }
			}
			else
			{
				Message msg = handler.obtainMessage();
				msg.what = NEWSFAILD;
				handler.sendMessage(msg);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Message msg = handler.obtainMessage();
			msg.what = NEWSFAILD;
			handler.sendMessage(msg);
		}
		
		return newsList;
	}
	
	//�첽���ظ�����������
	private class LoadNewsAsyncTask extends AsyncTask<Object, Integer, Void>
	{
		@Override
		protected void onPreExecute()
		{
			mTitlebarRefresh.setVisibility(View.GONE);
			mLoadnewsProgress.setVisibility(View.VISIBLE);
			mLoadMoreBtn.setText("���ڼ���...");
		}

		@Override
		protected Void doInBackground(Object... params) //�ɱ����   Object���͵�params����
		{
			getNewsData( (Integer)params[0], (Integer)params[1] );
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			mTitlebarRefresh.setVisibility(View.VISIBLE);
			mLoadnewsProgress.setVisibility(View.GONE);
			mLoadMoreBtn.setText("���ظ���");
		}
		
		
	}
}
