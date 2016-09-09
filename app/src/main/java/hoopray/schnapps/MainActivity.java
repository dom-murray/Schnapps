package hoopray.schnapps;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import hoopray.schnappscamera.CameraActivity;

public class MainActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		startActivity(new Intent(this, CameraActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}
}
