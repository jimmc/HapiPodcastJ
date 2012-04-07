package info.xuluan.podcast.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileUtils {
	public static boolean copy_file(String src, String dst)
	{
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        boolean b=true;
        try {
            File readFile = new File(src);

            File writeFile = new File(dst);

            fileInputStream = new FileInputStream(readFile);

            fileOutputStream = new FileOutputStream(writeFile);

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = fileInputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                fileOutputStream.write(buffer, 0, bytesRead);
            }
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
	
	public static String get_export_file_name(String title, long id)
	{
		title = title.replaceAll("[\\s\\\\:\\<\\>\\[\\]\\*\\|\\/\\?\\{\\}]+", "_");		

		return title+"_"+id+".mp3";
	}

}
