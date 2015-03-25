package com.wj.mynews.custom;

import java.util.List;
import java.util.Map;

import com.wj.mynews.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class CustomSimpleAdapter extends SimpleAdapter
{
	public CustomSimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
	{
		super(context, data, resource, from, to);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = super.getView(position, convertView, parent);
		// 更新第一个TextView的背景
		if (position == 0)
		{
			TextView tv = (TextView) v;
			tv.setTextColor(0xffadb2ad);// 浅白
			tv.setBackgroundResource(R.drawable.categorybar_item_background);
		}
		return v;
	}

}
