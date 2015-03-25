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
	
	private final int COLUMNWIDTHDP = 55;//定义导航栏一个选项的宽度
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
				//新闻列表适配器
				newsListAdapter = new SimpleAdapter(MainActivity.this, newsData, R.layout.newslist_item, 
						new String[]{"newslist_item_title", "newslist_item_digest", "newslist_item_source", "newslist_item_ptime"}, 
						new int[]{R.id.newslist_item_title, R.id.newslist_item_digest, R.id.newslist_item_source, R.id.newslist_item_ptime});
				
				newsListView.setAdapter(newsListAdapter);
			}
			else if (msg.what == NONEWS)
			{
				Toast.makeText(MainActivity.this, "该栏目下暂时没有新闻", Toast.LENGTH_SHORT).show();
			}
			else if (msg.what == NEWSFAILD)
			{
				Toast.makeText(MainActivity.this, "获取新闻失败", Toast.LENGTH_SHORT).show();
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
		
		//找到ListView
		newsListView = (ListView) findViewById(R.id.newslist);
		mTitlebarRefresh = (Button) findViewById(R.id.refresh_btn);
		mLoadnewsProgress = (ProgressBar) findViewById(R.id.loadnews_progress);
		mTitlebarRefresh.setOnClickListener(loadMoreListener);
		
		//获取布局加载器
		mInflater = getLayoutInflater();
		footerView = mInflater.inflate(R.layout.loadmore, null);
		newsListView.addFooterView(footerView);

		//获取新闻分类数组
		String[] categoryArray = getResources().getStringArray(R.array.category_array);

		//将新闻分类保存在List中
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

		// px单位转换为dp
		mColumnWidthPX = DensityUtils.dp2px(this, COLUMNWIDTHDP);
		// 创建GridView导航分类栏
		GridView categoryGridView = new GridView(this);
		categoryGridView.setColumnWidth(mColumnWidthPX);// 设置每个单元格的宽度
		categoryGridView.setNumColumns(GridView.AUTO_FIT);// 单元格的数量
		categoryGridView.setGravity(Gravity.CENTER);// 设置对齐方式
		
		//设置单元格选择是背景色为透明，这样选择时就不现实黄色背景
		categoryGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		
		//根据单元格宽度和数目计算GridView总宽度
		int width = mColumnWidthPX * categoriesList.size();
		LayoutParams params = new LayoutParams(width, LayoutParams.MATCH_PARENT);
		
		//更新categoryGridView宽度和高度，这样categoryGridView在一行显示
		categoryGridView.setLayoutParams(params);
		
		//设置默认值
		mCid=1;
		mCategoryTitle="焦点";
		
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
				//水平滚动的距离
				categoryScrollview.fling(DensityUtils.dp2px(MainActivity.this, mFlingWidth));
			}
		});
		
		//为导航条GridView设置监听器改变状态
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
				//获取点击的新闻分类编号
				mCid = categoriesList.get(position).get("category_title").getCid();
				//获取点击的新闻分类标题
				mCategoryTitle = categoriesList.get(position).get("category_title").getTitle();
				//getNewsData(mCid,newsCount);
				mLoadNewsAsyncTask = new LoadNewsAsyncTask();
				mLoadNewsAsyncTask.execute(mCid,newsCount);
			}
		});
		
		
		//从网络获取的集合数据
		getNewsData(1, newsCount);
		//ListView 绑定监听器
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
		//为加载更多按钮添加监听器
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
	
	//得到指定 的某类新闻的集合
	private ArrayList<HashMap<String, Object>> getSpecCatNews(int cid,int newsCount)
	{
		ArrayList<HashMap<String, Object>> newsList = new ArrayList<HashMap<String,Object>>();
		String urlStr = "http://192.168.36.1:8080/web/getSpecifyCategoryNews";
		String params = "startnid=0&count=" + newsCount + "&cid=" + cid;
		SyncHttp syncHttp = new SyncHttp();
		try
		{
			//以get方式请求，并获取里面的信息
			String retStr = syncHttp.httpGet(urlStr, params);
			JSONObject jsonObject = new JSONObject(retStr);
			//获取返回码 0表示成功
			int retCode = jsonObject.getInt("ret");
			if (retCode == 0)
			{
				 JSONObject dataObject = jsonObject.getJSONObject("data");
				 //获取返回数目
				 int totalnum = dataObject.getInt("totalnum");
				 if (totalnum > 0)
				 {
					 //获取返回新闻集合
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
	
	//异步加载更多新闻数据
	private class LoadNewsAsyncTask extends AsyncTask<Object, Integer, Void>
	{
		@Override
		protected void onPreExecute()
		{
			mTitlebarRefresh.setVisibility(View.GONE);
			mLoadnewsProgress.setVisibility(View.VISIBLE);
			mLoadMoreBtn.setText("正在加载...");
		}

		@Override
		protected Void doInBackground(Object... params) //可变参数   Object类型的params数组
		{
			getNewsData( (Integer)params[0], (Integer)params[1] );
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			mTitlebarRefresh.setVisibility(View.VISIBLE);
			mLoadnewsProgress.setVisibility(View.GONE);
			mLoadMoreBtn.setText("加载更多");
		}
		
		
	}
}
