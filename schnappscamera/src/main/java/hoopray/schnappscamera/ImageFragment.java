package hoopray.schnappscamera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * @author Marcus Hooper
 */
class ImageFragment extends Fragment
{
	private static final String FILE_PATH = "file_path";
	private String path;

	public static ImageFragment newInstance(String filePath)
	{
		Bundle args = new Bundle();
		args.putString(FILE_PATH, filePath);
		ImageFragment fragment = new ImageFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		path = getArguments().getString(FILE_PATH);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.image_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		ImageView imageView = (ImageView) view;
		imageView.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener(imageView, path));
	}

	private static class OnPreDrawListener implements ViewTreeObserver.OnPreDrawListener
	{
		private WeakReference<ImageView> imageViewWeakReference;
		private String path;

		OnPreDrawListener(ImageView imageView, String path)
		{
			imageViewWeakReference = new WeakReference<>(imageView);
			this.path = path;
		}

		@Override
		public boolean onPreDraw()
		{
			ImageView imageView = imageViewWeakReference.get();
			if(imageView != null)
			{
				imageView.getViewTreeObserver().removeOnPreDrawListener(this);
				Picasso.with(imageView.getContext()).load(new File(path))
						.resize(imageView.getWidth(), imageView.getHeight()).centerInside().into(imageView);
			}
			return true;
		}
	}
}
