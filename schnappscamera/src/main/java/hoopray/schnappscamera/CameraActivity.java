package hoopray.schnappscamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;

import java.io.File;
import java.util.ArrayList;

import static hoopray.schnappscamera.Schnapps.PATH_LIST;

/**
 * @author Marcus Hooper
 */
class CameraActivity extends Activity
{
	private HardwareVolumeListener hardwareVolumeListener = null;
	private ArrayList<String> pathList = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		getWindow().setStatusBarColor(Color.BLACK);

		if(savedInstanceState == null)
			cleanUp();
		else
			pathList = savedInstanceState.getStringArrayList(PATH_LIST);

		getFragmentManager().beginTransaction().add(R.id.container, new CameraFragment()).commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(PATH_LIST, pathList);
	}

	@Override
	public void finish()
	{
		if(pathList.size() == 0)
			setResult(RESULT_CANCELED);
		else
		{
			Intent data = new Intent();
			data.putExtra(PATH_LIST, pathList);
			setResult(RESULT_OK, data);
		}

		super.finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			if(hardwareVolumeListener != null)
			{
				hardwareVolumeListener.onVolumeClicked();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void cleanUp()
	{
		File imagesDir = getExternalFilesDir(null);
		imagesDir.mkdir();
		File[] files = imagesDir.listFiles();
		if(null != files)
			for(File file : files)
				file.delete();
	}

	public ArrayList<String> getPathList()
	{
		return pathList;
	}

	public void setHardwareVolumeListener(HardwareVolumeListener hardwareVolumeListener)
	{
		this.hardwareVolumeListener = hardwareVolumeListener;
	}

	public interface HardwareVolumeListener
	{
		void onVolumeClicked();
	}
}
