package info.xuluan.podcast.utils;

import java.util.ArrayList;


public class DialogMenu {
	
	private ArrayList<MenuItem> mMenuItems;
	private String[] mDialogItems;
	private String mDialogHeader;
	
	public DialogMenu()
	{
		mMenuItems = new ArrayList<MenuItem>();
	}
	
	public void setHeader(String header)
	{
		mDialogHeader = header;
	}
	
	public String getHeader()
	{
		return mDialogHeader;
	}
	
	public void addMenu(int i, String str)
	{
		mMenuItems.add(new MenuItem(i,str));			
	}
	
	public String[] getItems()
	{
		
		mDialogItems = new String[mMenuItems.size()];
		
		for(int i=0;i<mMenuItems.size();i++) {
			mDialogItems[i] = mMenuItems.get(i).title;
			
		}
		return mDialogItems;
	}
	
	public int getSelect(int sel)
	{
		return mMenuItems.get(sel).id;
	}
	
	class MenuItem {

		public int id;
		public String title;
		
		public MenuItem(int i, String str) 
		{
			id = i;
			title = str;
		}
	
	}
	
}