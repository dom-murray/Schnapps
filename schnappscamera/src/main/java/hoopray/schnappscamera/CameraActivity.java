package hoopray.schnappscamera;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;

import java.io.File;

/**
 * @author Marcus Hooper
 */
public class CameraActivity extends Activity
{
	public static final String PATH_LIST = "pathList";

	private HardwareVolumeListener hardwareVolumeListener = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		getWindow().setStatusBarColor(Color.BLACK);

		if(savedInstanceState == null)
			cleanUp();

		getFragmentManager().beginTransaction().add(R.id.container, new CameraFragment()).commit();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			if(hardwareVolumeListener != null)
				hardwareVolumeListener.onVolumeClicked();
		}
		return true;
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

	public void setHardwareVolumeListener(HardwareVolumeListener hardwareVolumeListener)
	{
		this.hardwareVolumeListener = hardwareVolumeListener;
	}

	public interface HardwareVolumeListener
	{
		void onVolumeClicked();
	}
}
