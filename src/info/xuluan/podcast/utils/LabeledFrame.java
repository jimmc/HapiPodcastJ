package info.xuluan.podcast.utils;

import info.xuluan.podcastj.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class LabeledFrame extends FrameLayout {
	private final String NAMESPACE="http://schemas.android.com/apk/res/info.xuluan.podcastj";
	private String frameLabel;
	private ViewGroup container;

	public LabeledFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		frameLabel = attrs.getAttributeValue(NAMESPACE, "frameLabel");
		if (frameLabel==null)
			frameLabel = "(none)";
		inflateOurLayout();
	}

	private void inflateOurLayout() {
		((Activity)getContext()).getLayoutInflater().inflate(R.layout.labeled_frame, this);
		//LayoutInflator.from(getContext()).inflate(R.layout.framed_grid, this);
		
		TextView tv = (TextView) findViewById(R.id.borderText);
		tv.setText(frameLabel);
		Drawable bg = this.getBackground();
		tv.setBackgroundDrawable(bg);

		container = (ViewGroup)this.findViewById(R.id.LabeledFrameContainer);
	}
	
	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params)
	{
		//Once we have finished inflating our own layout and have assigned our container view,
		//anything else we add gets added to the designated container instead.
		if (container!=null)
			container.addView(child, index, params);
		else
			super.addView(child, index, params);
	} 
}
