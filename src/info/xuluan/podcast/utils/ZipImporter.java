package info.xuluan.podcast.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;

public class ZipImporter {
	public interface ContentReader {
		void readContent(ZipInputStream zos, ZipEntry entry) throws IOException;
	}

	public static void importFromZipFile(Activity act, File inFile, ContentReader cr) {
        InputStream inputStream = null;
        try {
        	inputStream = new FileInputStream(inFile);
        	ZipInputStream zis = new ZipInputStream(inputStream);
        	ZipEntry entry;
        	while((entry = zis.getNextEntry()) != null) {
        		cr.readContent(zis, entry);
        	}
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
        finally{
        	if (inputStream != null) {
        		try {
        			inputStream.close();
        		}
        		catch (Exception ex) {}
        	}
        }
	}
	
    public static Map<String,String> getChildrenContent(Element base) {
    	HashMap<String,String> map = new HashMap<String,String>();
    	for (Node child=base.getFirstChild(); child!=null; child=child.getNextSibling()) {
			String tag = child.getNodeName();
			String content = child.getTextContent();
			map.put(tag, content);
		}
    	return map;
    }

    public static Element getFirstElementByTagName(Element base, String tag) {
    	NodeList elements = base.getElementsByTagName(tag);
    	if (elements==null || elements.getLength()==0)
    		return null;	//no such child element found
    	return (Element)elements.item(0);
    }
    
    public static long parseLong(String s, long dflt) {
    	try {
    		return Long.parseLong(s);
    	}
    	catch (NumberFormatException ex) {
    		return dflt;
    	}
    }
    
    public static long parseLong(String s) { return parseLong(s,0); }
}
