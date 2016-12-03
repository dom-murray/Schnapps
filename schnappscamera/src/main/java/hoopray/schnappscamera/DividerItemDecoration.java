package hoopray.schnappscamera;

import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * @author Dominic Murray 3/12/2016.
 */

class DividerItemDecoration extends RecyclerView.ItemDecoration
{
	private int orientation;
	private int dividerSize;

	public DividerItemDecoration(int dividerSize) {
		this.dividerSize = dividerSize;
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);

		if (parent.getChildAdapterPosition(view) == 0) {
			return;
		}

		orientation = ((LinearLayoutManager) parent.getLayoutManager()).getOrientation();
		if (orientation == LinearLayoutManager.HORIZONTAL) {
			outRect.left = dividerSize;
		} else if (orientation == LinearLayoutManager.VERTICAL) {
			outRect.top = dividerSize;
		}
	}
}
