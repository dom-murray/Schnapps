package hoopray.schnappscamera;

import android.app.Activity;
import android.os.Bundle;

import java.io.File;

/**
 * @author Marcus Hooper
 */
public class CameraActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		if(savedInstanceState == null)
			cleanUp();

		getFragmentManager().beginTransaction().add(R.id.container, new CameraFragment()).commit();
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
}
