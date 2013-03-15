package info.xuluan.podcast.utils;

import java.io.File;
import android.os.Environment;

public class SDCardMgr {

	public static String SDCARD_DIR =
			Environment.getExternalStorageDirectory().getPath(); //e.g. /sdcard
	public static final String APP_DIR = "/xuluan.podcast";
	public static final String DOWNLOAD_DIR = "/download";
	public static final String EXPORT_DIR = "/export";
	
	public static boolean getSDCardStatus()
	{
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}
	
	public static boolean getSDCardStatusAndCreate()
	{
		boolean b = getSDCardStatus();
		if(b)
			createDir();
		return b;
	}	

	public static String getExportDir()
	{
		File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
		SDCARD_DIR = sdDir.getAbsolutePath();
		return SDCARD_DIR + APP_DIR + EXPORT_DIR;
	}	

	public static String getDownloadDir()
	{
		File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
		SDCARD_DIR = sdDir.getAbsolutePath();
		return SDCARD_DIR + APP_DIR + DOWNLOAD_DIR;
	}
	
	public static String getAppDir()
	{
		File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
		SDCARD_DIR = sdDir.getAbsolutePath();
		return SDCARD_DIR + APP_DIR;
	}


	private static boolean createDir()
	{
		File file = new File(getDownloadDir());
		boolean exists = (file.exists());
		if (!exists) {
			return file.mkdirs();
		}
		
		file = new File(getExportDir());
		exists = (file.exists());
		if (!exists) {
			return file.mkdirs();
		}		
		return true;
	}		

}
