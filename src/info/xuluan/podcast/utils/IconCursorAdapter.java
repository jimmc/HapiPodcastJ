package info.xuluan.podcast.utils;

import android.widget.SimpleCursorAdapter;
import android.database.Cursor;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.xuluan.podcastj.R;

import java.util.HashMap;

public class IconCursorAdapter extends SimpleCursorAdapter {

	public static final int ICON_DEFAULT_ID = -1;
	
	public interface FieldHandler {
		public void setViewValue(IconCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId);
	}
	public static class TextFieldHandler implements FieldHandler {
		public void setViewValue(IconCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			//Normal text column, just display what's in the database
			String text = cursor.getString(fromColumnId);

			if (text == null) {
				text = "";
			}

			if (v instanceof TextView) {
				adapter.setViewText((TextView) v, text);
			} else if (v instanceof ImageView) {
				adapter.setViewImage((ImageView) v, text);
			}
		}
	}
	public static class IconFieldHandler implements FieldHandler {
		HashMap<Integer, Integer> mIconMap;
		public IconFieldHandler(HashMap<Integer,Integer> iconMap) {
			mIconMap = iconMap;
		}
		public void setViewValue(IconCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			//The status column gets displayed as our icon
			int status = cursor.getInt(fromColumnId);

			Integer iconI = mIconMap.get(status);
			if (iconI==null)
				iconI = mIconMap.get(ICON_DEFAULT_ID);	//look for default value in map
			int icon = (iconI!=null)?
				iconI.intValue():
				R.drawable.status_unknown;	//Use this icon when not in map and no map default.
					//This allows going back to a previous version after data has been
					//added in a new version with additional status codes.
			adapter.setViewImage2((ImageView) v, icon);
		}
	}
	public final static FieldHandler defaultTextFieldHandler = new TextFieldHandler();
	
	protected int[] mFrom2;
	protected int[] mTo2;
	protected FieldHandler[] mFieldHandlers;

	//Create a set of FieldHandlers for methods calling using the legacy constructor
	private static FieldHandler[] defaultFieldHandlers(String[] fromColumns,
			HashMap<Integer,Integer> iconMap) {
		FieldHandler[] handlers = new FieldHandler[fromColumns.length];
		for (int i=0; i<handlers.length-1; i++) {
			handlers[i] = defaultTextFieldHandler;
		}
		handlers[fromColumns.length-1] = new IconFieldHandler(iconMap);
		return handlers;
	}

	//Legacy constructor
	public IconCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, HashMap<Integer, Integer> iconMap) {
		this(context,layout,cursor,fromColumns,to,
				defaultFieldHandlers(fromColumns,iconMap));
	}

	//Newer constructor allows custom FieldHandlers.
	//Would be better to bundle fromColumn/to/fieldHandler for each field and pass a single array
	//of those objects, but the overhead of defining that value class in Java is not worth it.
	//If this were a Scala program, that would be a one-line case class.
	public IconCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, FieldHandler[] fieldHandlers) {
		super(context, layout, cursor, fromColumns, to);

		if (to.length<fromColumns.length){
			mTo2  = new int[fromColumns.length];
			for (int i=0; i<to.length; i++)
				mTo2[i] = to[i];
			mTo2[fromColumns.length-1] = R.id.icon;
		} else
			mTo2 = to;
		mFieldHandlers = fieldHandlers;
		if (cursor != null) {
			int i;
			int count = fromColumns.length;
			if (mFrom2 == null || mFrom2.length != count) {
				mFrom2 = new int[count];
			}
			for (i = 0; i < count; i++) {
				mFrom2[i] = cursor.getColumnIndexOrThrow(fromColumns[i]);
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// Log.w("VIEW", "newView");
		View v = super.newView(context, cursor, parent);
		final int[] to = mTo2;
		final int count = mFieldHandlers.length;
		final View[] holder = new View[count];
		// Log.d("ADAPTER", "count = "+count);

		for (int i = 0; i < count; i++) {
			holder[i] = v.findViewById(to[i]);
		}
		v.setTag(holder);

		return v;

	}

	public void setViewImage2(ImageView v, int value) {
		// Log.w("VIEW", "setViewImage2");
		v.setImageResource(value);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final View[] holder = (View[]) view.getTag();
		final int count = mFieldHandlers.length;
		final int[] from = mFrom2;

		for (int i = 0; i < count; i++) {
			mFieldHandlers[i].setViewValue(this,cursor,holder[i],from[i]);
		}

	}

}
