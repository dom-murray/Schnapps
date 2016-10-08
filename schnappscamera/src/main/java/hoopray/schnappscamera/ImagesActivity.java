package hoopray.schnappscamera;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import com.merhold.extensiblepageindicator.ExtensiblePageIndicator;

import java.io.File;

/**
 * @author Marcus Hooper
 */
public class ImagesActivity extends AppCompatActivity
{
	public static final String INDEX = "index";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_images);
		setTitle(getString(R.string.images));

		ViewPager imagesPager = (ViewPager) findViewById(R.id.images_pager);
		imagesPager.setAdapter(new ImagesPager(getSupportFragmentManager(), this));
		imagesPager.setOffscreenPageLimit(2);

		ExtensiblePageIndicator indicator = (ExtensiblePageIndicator) findViewById(R.id.pager_indicator);
		indicator.initViewPager(imagesPager);

		imagesPager.setCurrentItem(getIntent().getIntExtra(INDEX, 0));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		onBackPressed();
		return super.onOptionsItemSelected(item);
	}

	private static class ImagesPager extends FragmentPagerAdapter
	{
		private File[] images;

		public ImagesPager(FragmentManager fm, Context context)
		{
			super(fm);
			this.images = context.getExternalFilesDir(null).listFiles();
		}

		@Override
		public int getCount()
		{
			return images.length;
		}

		@Override
		public Fragment getItem(int position)
		{
			return ImageFragment.newInstance(images[position].getPath());
		}
	}
}
