package hoopray.schnappscamera;

import android.os.Build;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * @author Marcus Hooper
 */
public class HeaderViewPagerTransformer implements ViewPager.PageTransformer
{
	final float MIN = 0.8f;

	@Override
	public void transformPage(View page, float position)
	{
		float scale = Math.abs(position * 0.9f);
		scale = 1 - scale; //Reversing scale so closer to zero is bigger (0 is center)
		scale = Math.max(scale, MIN);
		page.setScaleY(scale);
		page.setScaleX(scale);
		request(page);
	}

	private void request(View page)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			page.getParent().requestLayout();
	}
}