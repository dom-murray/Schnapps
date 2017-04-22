package hoopray.schnapps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import hoopray.schnappscamera.Schnapps;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
	private static final int REQUEST_CAMERA_PERMISSION = 1;
	private static final String FRAGMENT_DIALOG = "dialog";
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
				requestCameraPermission();
			}
		});
	}

	/**
	 * Shows OK/Cancel confirmation dialog about camera permission.
	 */
	public static class ConfirmationDialog extends DialogFragment
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			return new AlertDialog.Builder(getActivity())
					.setMessage("Schapps App needs permission to access the camer in order to take photos")
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							ActivityCompat.requestPermissions(getActivity(),
									new String[]{Manifest.permission.CAMERA},
									REQUEST_CAMERA_PERMISSION);
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							Activity activity = getActivity();
							if(activity != null)
								activity.finish();
						}
					})
					.create();
		}
	}

	private void requestCameraPermission()
	{
		if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
			new ConfirmationDialog().show(getFragmentManager(), FRAGMENT_DIALOG);
		else
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		if(requestCode == REQUEST_CAMERA_PERMISSION)
		{
			if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
				Log.e("Schnapps Activity", "Camera Permission Denied");
			else
				Schnapps.openCamera(MainActivity.this);
		}
		else
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
