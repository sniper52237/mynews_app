package com.wj.mynews.utils;

public class StringUtil
{
	/**
	 * 从字符串转换成整型
	 * @param str
	 * @return
	 */
	public static int string2Int(String str)
	{
		try
		{
			int value = Integer.valueOf(str);
			return value;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}

}
