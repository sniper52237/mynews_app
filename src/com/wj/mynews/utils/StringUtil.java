package com.wj.mynews.utils;

public class StringUtil
{
	/**
	 * ���ַ���ת��������
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
