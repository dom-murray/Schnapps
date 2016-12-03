package hoopray.schnappscamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * @author Dominic Murray 3/12/2016.
 */

public class Helpers
{
	public static void calculateInSampleSize(String imagePath, BitmapFactory.Options options, int dimension)
	{
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, options);

		final int height = options.outHeight;
		final int width = options.outWidth;
		int sampleSize = 1;

		if(dimension >= 0 && (height > dimension || width > dimension))
		{
			final int heightRatio = Math.round((float) height / (float) dimension);
			final int widthRatio = Math.round((float) width / (float) dimension);
			sampleSize = Math.min(widthRatio, heightRatio);
		}

		options.inSampleSize = sampleSize;
		options.inJustDecodeBounds = false;
	}

	public static Bitmap loadBitmapToSize(String path, int dimension)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		Helpers.calculateInSampleSize(path, options, dimension);
		return BitmapFactory.decodeFile(path, options);
	}
}
