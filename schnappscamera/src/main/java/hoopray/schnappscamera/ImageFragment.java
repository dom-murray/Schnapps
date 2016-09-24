package hoopray.schnappscamera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * @author Marcus Hooper
 */
public class ImageFragment extends Fragment
{
	private static final String FILE_PATH = "file_path";

	public static ImageFragment newInstance(String filePath)
	{
		Bundle args = new Bundle();
		args.putString(FILE_PATH, filePath);
		ImageFragment fragment = new ImageFragment();
		fragment.setArguments(args);
		return fragment;
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
		Picasso.with(view.getContext()).load(new File(getArguments().getString(FILE_PATH))).fit().into(imageView);
	}
}
