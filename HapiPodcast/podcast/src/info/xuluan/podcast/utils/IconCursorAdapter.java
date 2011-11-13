package info.xuluan.podcast.utils;

import android.widget.SimpleCursorAdapter;
import android.database.Cursor;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.xuluan.podcast.R;

import java.util.HashMap;

public class IconCursorAdapter extends SimpleCursorAdapter {

	protected int[] mFrom2;
	protected int[] mTo2;

	HashMap<Integer, Integer> mIconMap = null;

	public IconCursorAdapter(Context context, int layout, Cursor cursor,
			String[] from, int[] to, HashMap<Integer, Integer> map) {
		super(context, layout, cursor, from, to);
		mTo2 = to;
		mIconMap = map;
		if (cursor != null) {
			int i;
			int count = from.length;
			if (mFrom2 == null || mFrom2.length != count) {
				mFrom2 = new int[count];
			}
			for (i = 0; i < count; i++) {
				mFrom2[i] = cursor.getColumnIndexOrThrow(from[i]);
			}
		}

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// Log.w("VIEW", "newView");
		View v = super.newView(context, cursor, parent);
		final int[] to = mTo2;
		final int count = to.length;
		final View[] holder = new View[count + 1];
		// Log.d("ADAPTER", "count = "+count);

		for (int i = 0; i < count; i++) {
			holder[i] = v.findViewById(to[i]);
		}
		holder[count] = v.findViewById(R.id.icon);
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
		final int count = mTo2.length;
		final int[] from = mFrom2;

		for (int i = 0; i < count + 1; i++) {
			final View v = holder[i];
			if (i == count) {
				View v_icon = view.findViewById(R.id.icon);
				int status = cursor.getInt(from[i]);

				setViewImage2((ImageView) v_icon, mIconMap.get(status));

				break;
			}
			if (v != null) {
				String text = cursor.getString(from[i]);

				if (text == null) {
					text = "";
				}

				if (v instanceof TextView) {
					setViewText((TextView) v, text);
				} else if (v instanceof ImageView) {
					setViewImage((ImageView) v, text);
				}
			}
		}

	}

}
