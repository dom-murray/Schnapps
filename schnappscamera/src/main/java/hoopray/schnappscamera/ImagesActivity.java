package hoopray.schnappscamera;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * @author Marcus Hooper
 */
class ImagesActivity extends AppCompatActivity
{
	public static final String INDEX = "index";
	public static final String PATH_LIST = "pathList";
	private ArrayList<String> pathList;
	private RecyclerView recyclerView;
	private ViewPager imagesPager;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_images);
		getWindow().setStatusBarColor(Color.BLACK);
		if(getSupportActionBar() != null)
		{
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle(R.string.images);
		}

		if(savedInstanceState == null)
			pathList = getIntent().getStringArrayListExtra(PATH_LIST);
		else
			pathList = savedInstanceState.getStringArrayList(PATH_LIST);


		imagesPager = (ViewPager) findViewById(R.id.images_pager);
		imagesPager.setAdapter(new ImagesPager(getSupportFragmentManager()));
		imagesPager.setOffscreenPageLimit(2);
		imagesPager.setCurrentItem(getIntent().getIntExtra(INDEX, 0));

		recyclerView = (RecyclerView) findViewById(R.id.photo_roll);
		recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		recyclerView.setAdapter(new PhotoAdapter());
		recyclerView.addItemDecoration(new DividerItemDecoration((int) getResources().getDisplayMetrics().density * 8));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(PATH_LIST, pathList);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		onBackPressed();
		return super.onOptionsItemSelected(item);
	}

	private class ImagesPager extends FragmentPagerAdapter
	{
		ImagesPager(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			return pathList.size();
		}

		@Override
		public Fragment getItem(int position)
		{
			return ImageFragment.newInstance(pathList.get(position));
		}
	}

	private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder>
	{

		@Override
		public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
		{
			return new PhotoViewHolder(getLayoutInflater().inflate(R.layout.photo_grid_item, parent, false));
		}

		@Override
		public void onBindViewHolder(PhotoViewHolder holder, int position)
		{
			holder.update(pathList.get(position));
		}

		@Override
		public int getItemCount()
		{
			return pathList.size();
		}
	}

	private class PhotoViewHolder extends RecyclerView.ViewHolder
	{
		public PhotoViewHolder(View itemView)
		{
			super(itemView);
		}

		public void update(String path)
		{
			((ImageView) itemView).setImageBitmap(Helpers.loadBitmapToSize(path, (int) getResources().getDisplayMetrics().density * 48));
			itemView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					imagesPager.setCurrentItem(getAdapterPosition());
				}
			});
		}
	}
}
