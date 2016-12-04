package hoopray.schnapps;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import hoopray.schnappscamera.Schnapps;

public class MainActivity extends AppCompatActivity
{
	private ArrayList<String> pathList = new ArrayList<>();
	private PhotoAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.photo_grid);
		adapter = new PhotoAdapter();
		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

		findViewById(R.id.open_camera_button).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Schnapps.openCamera(MainActivity.this);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		ArrayList<String> photoPaths = Schnapps.getPhotoPaths(requestCode, resultCode, data);
		if(photoPaths != null)
		{
			int count = pathList.size();
			pathList.addAll(photoPaths);
			adapter.notifyItemRangeInserted(count, photoPaths.size());
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
			int size = (int) getResources().getDisplayMetrics().density * 48;
			Picasso.with(MainActivity.this).load(new File(path))
					.resize(size, size).centerInside().into(((ImageView) itemView));
		}
	}
}
