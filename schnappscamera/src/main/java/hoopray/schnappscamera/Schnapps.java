package hoopray.schnappscamera;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

/**
 * @author Dominic Murray 3/12/2016.
 */

public class Schnapps
{
	private static final int SCHNAPPS_CODE = 3312;
	static final String PATH_LIST = "pathList";

	public static void openCamera(Activity callingActivity)
	{
		callingActivity.startActivityForResult(new Intent(callingActivity, CameraActivity.class), SCHNAPPS_CODE);
	}

	public static ArrayList<String> getPhotoPaths(int requestCode, int resultCode, Intent data)
	{
		if(requestCode != SCHNAPPS_CODE || resultCode != RESULT_OK)
			return null;

		return data.getStringArrayListExtra(PATH_LIST);
	}
}
