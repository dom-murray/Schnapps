package hoopray.schnappscamera;

import android.app.Activity;
import android.os.Bundle;

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
		getFragmentManager().beginTransaction().add(R.id.container, new CameraFragment()).commit();
	}
}
