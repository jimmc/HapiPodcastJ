package info.xuluan.podcast.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.Toast;

public class ZipExporter {
	public interface ContentWriter {
		void writeContent(ZipOutputStream zos) throws IOException;
	}
	
	public static void exportToZipFile(Activity act, String filename, ContentWriter cw) {
		String filepath = SDCardMgr.getExportDir()+"/"+filename;
		File outFile = new File(filepath);
		ZipOutputStream zos = null;
		Toast.makeText(act, "Please wait... ", Toast.LENGTH_LONG).show(); 
		try {
			FileOutputStream os = new FileOutputStream(outFile);
			zos = new ZipOutputStream(new BufferedOutputStream(os));
			zos.setLevel(0); //mp3 files don't compress well, and our XML is small
			cw.writeContent(zos);
			zos.close();
			zos = null;
			Toast.makeText(act, "Exported zip file to : "+ filepath, 
					Toast.LENGTH_LONG).show();
		} catch (IOException ex) {
			ex.printStackTrace();
			Toast.makeText(act, "Export zip failed ", 
					Toast.LENGTH_LONG).show();
		} finally {
			try {
				if (zos!=null)
					zos.close();
			} catch (Exception ex) {}
		}
	}
	
	public static String getExportZipFileName(String base)
	{
		base = base.replaceAll("[\\s\\\\:\\<\\>\\[\\]\\*\\|\\/\\?\\{\\}\\'\\\"]+", "_");		
		return base+".zip";
	}

	public static void writeTagLine(PrintWriter out, String tag, int level, boolean isStart) {
		writeIndent(out,level);
		out.print("<"+(isStart?"":"/")+tag+">\n");
	}
	
	public static void writeXmlField(PrintWriter out, String fieldName, long value, int level) {
		writeXmlField(out, fieldName, Long.toString(value), level);
	}
	
	public static void writeXmlField(PrintWriter out, String fieldName, String text, int level) {
		if (text==null)
			return;
		writeIndent(out,level);
		out.print("<"+fieldName+">");
		out.print(TextUtils.htmlEncode(text).toString());
		out.print("</"+fieldName+">\n");
	}

	public static void writeIndent(PrintWriter out, int level) {
		for (; level>0; level--)
			out.print("  ");	//indent
	}

}
