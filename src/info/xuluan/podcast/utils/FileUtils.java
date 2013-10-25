package info.xuluan.podcast.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import android.text.Html;

public class FileUtils {
	public static void copyFile(InputStream inputStream, OutputStream outputStream)
			throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int bytesRead = inputStream.read(buffer);
			if (bytesRead <= 0) {
				break;
			}
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
	}
	
	public static boolean copyFile(String src, String dst)
	{
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        boolean b=true;
        try {
            File readFile = new File(src);
            File writeFile = new File(dst);
            fileInputStream = new FileInputStream(readFile);
            fileOutputStream = new FileOutputStream(writeFile);
            copyFile(fileInputStream, fileOutputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception ex) {}
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception ex) {}
        }	
        
        return b;
	}
	
	public static String getExportFileName(String title, long id, String fileType)
	{
		title = title.replaceAll("[\\s\\\\:\\<\\>\\[\\]\\*\\|\\/\\?\\{\\}\\'\\\"]+", "_");		
		return title+"_"+id+"."+fileType;
	}
	
}
