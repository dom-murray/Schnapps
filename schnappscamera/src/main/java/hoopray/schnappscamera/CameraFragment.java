package hoopray.schnappscamera;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Modified from https://github.com/googlesamples/android-Camera2Basic
 *
 * @author Marcus Hooper
 */
public class CameraFragment extends Fragment implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback
{
	/**
	 * Conversion from screen rotation to JPEG orientation.
	 */
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	private static final int REQUEST_CAMERA_PERMISSION = 1;
	private static final String FRAGMENT_DIALOG = "dialog";

	static
	{
		ORIENTATIONS.append(Surface.ROTATION_0, 90);
		ORIENTATIONS.append(Surface.ROTATION_90, 0);
		ORIENTATIONS.append(Surface.ROTATION_180, 270);
		ORIENTATIONS.append(Surface.ROTATION_270, 180);
	}

	/**
	 * Tag for the {@link Log}.
	 */
	private static final String TAG = "Camera2BasicFragment";

	/**
	 * Camera state: Showing camera preview.
	 */
	private static final int STATE_PREVIEW = 0;

	/**
	 * Camera state: Waiting for the focus to be locked.
	 */
	private static final int STATE_WAITING_LOCK = 1;

	/**
	 * Camera state: Waiting for the exposure to be precapture state.
	 */
	private static final int STATE_WAITING_PRECAPTURE = 2;

	/**
	 * Camera state: Waiting for the exposure state to be something other than precapture.
	 */
	private static final int STATE_WAITING_NON_PRECAPTURE = 3;

	/**
	 * Camera state: Picture was taken.
	 */
	private static final int STATE_PICTURE_TAKEN = 4;

	/**
	 * Max preview width that is guaranteed by Camera2 API
	 */
	private static final int MAX_PREVIEW_WIDTH = 1920;

	/**
	 * Max preview height that is guaranteed by Camera2 API
	 */
	private static final int MAX_PREVIEW_HEIGHT = 1080;

	private static boolean capturing;

	/**
	 * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
	 * {@link TextureView}.
	 */
	private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener()
	{
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height)
		{
			openCamera(width, height);
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height)
		{
			configureTransform(width, height);
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture texture)
		{
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture)
		{
		}
	};

	/**
	 * ID of the current {@link CameraDevice}.
	 */
	private String mCameraId;

	private View cameraButton;

	/**
	 * An {@link TextureView} for camera preview.
	 */
	private TextureView textureView;

	/**
	 * A {@link CameraCaptureSession} for camera preview.
	 */
	private CameraCaptureSession mCaptureSession;

	/**
	 * A reference to the opened {@link CameraDevice}.
	 */
	private CameraDevice mCameraDevice;

	/**
	 * The {@link android.util.Size} of camera preview.
	 */
	private Size mPreviewSize;

	/**
	 * A {@link android.support.v7.widget.RecyclerView} that displays a list of taken images
	 * to be returned through the intent result
	 */
	private RecyclerView mPhotoGrid;

	/**
	 * {@link android.support.v7.widget.RecyclerView.Adapter} an adapter for displaying
	 * the recently taken images
	 */

	private GridImageAdapter mPhotoAdapter;

	/**
	 * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
	 */
	private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback()
	{
		@Override
		public void onOpened(@NonNull CameraDevice cameraDevice)
		{
			// This method is called when the camera is opened.  We start camera preview here.
			mCameraOpenCloseLock.release();
			mCameraDevice = cameraDevice;
			createCameraPreviewSession();
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice cameraDevice)
		{
			mCameraOpenCloseLock.release();
			cameraDevice.close();
			mCameraDevice = null;
		}

		@Override
		public void onError(@NonNull CameraDevice cameraDevice, int error)
		{
			mCameraOpenCloseLock.release();
			cameraDevice.close();
			mCameraDevice = null;
			Activity activity = getActivity();
			if(null != activity)
			{
				activity.finish();
			}
		}
	};

	/**
	 * An additional thread for running tasks that shouldn't block the UI.
	 */
	private HandlerThread mBackgroundThread;

	/**
	 * A {@link Handler} for running tasks in the background.
	 */
	private Handler mBackgroundHandler;

	/**
	 * An {@link ImageReader} that handles still image capture.
	 */
	private ImageReader mImageReader;

	/**
	 * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
	 * still image is ready to be saved.
	 */
	private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener()
	{
		private int mCount = 0;

		@Override
		public void onImageAvailable(ImageReader reader)
		{
			String path = getActivity().getExternalFilesDir(null).getPath() + "/pic" + mCount + ".jpg";
			mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), path, mCount, imageSavedListener));
			mCount++;
		}
	};

	private final ImageSaver.ImageSavedListener imageSavedListener = new ImageSaver.ImageSavedListener()
	{
		@Override
		public void imageSaved(final int position)
		{
			mPhotoGrid.post(new Runnable()
			{
				@Override
				public void run()
				{
					int count = mPhotoAdapter.getItemCount();
					if(position <= count)
						mPhotoAdapter.notifyItemChanged(position);

					if(count < 3) //Update the first image when there are 1 || 2 images stored
						mPhotoAdapter.notifyItemChanged(0);

					mPhotoGrid.scrollToPosition(position);
					animateCameraButton();
				}
			});
		}
	};

	/**
	 * {@link CaptureRequest.Builder} for the camera preview
	 */
	private CaptureRequest.Builder mPreviewRequestBuilder;

	/**
	 * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
	 */
	private CaptureRequest mPreviewRequest;

	/**
	 * The current state of camera state for taking pictures.
	 *
	 * @see #mCaptureCallback
	 */
	private int mState = STATE_PREVIEW;

	/**
	 * A {@link Semaphore} to prevent the app from exiting before closing the camera.
	 */
	private Semaphore mCameraOpenCloseLock = new Semaphore(1);

	/**
	 * Whether the current camera device supports Flash or not.
	 */
	private boolean mFlashSupported;

	/**
	 * Orientation of the camera sensor
	 */
	private int mSensorOrientation;

	/**
	 * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
	 */
	private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback()
	{
		private void process(CaptureResult result)
		{
			switch(mState)
			{
				case STATE_PREVIEW:
				{
					// We have nothing to do when the camera preview is working normally.
					break;
				}
				case STATE_WAITING_LOCK:
				{
					Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
					if(afState == null)
					{
						captureStillPicture();
					}
					else if(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState)
					{
						// CONTROL_AE_STATE can be null on some devices
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						if(aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED)
						{
							mState = STATE_PICTURE_TAKEN;
							captureStillPicture();
						}
						else
						{
							runPrecaptureSequence();
						}
					}
					break;
				}
				case STATE_WAITING_PRECAPTURE:
				{
					// CONTROL_AE_STATE can be null on some devices
					Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
					if(aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED)
					{
						mState = STATE_WAITING_NON_PRECAPTURE;
					}
					break;
				}
				case STATE_WAITING_NON_PRECAPTURE:
				{
					// CONTROL_AE_STATE can be null on some devices
					Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
					if(aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE)
					{
						mState = STATE_PICTURE_TAKEN;
						captureStillPicture();
					}
					break;
				}
			}
		}

		@Override
		public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult)
		{
			process(partialResult);
		}

		@Override
		public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
		{
			process(result);
		}
	};

	/**
	 * Shows a {@link Toast} on the UI thread.
	 *
	 * @param text The message to show
	 */
	private void showToast(final String text)
	{
		final Activity activity = getActivity();
		if(activity != null)
			activity.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
				}
			});
	}

	/**
	 * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
	 * is at least as large as the respective texture view size, and that is at most as large as the
	 * respective max size, and whose aspect ratio matches with the specified value. If such size
	 * doesn't exist, choose the largest one that is at most as large as the respective max size,
	 * and whose aspect ratio matches with the specified value.
	 *
	 * @param choices           The list of sizes that the camera supports for the intended output
	 *                          class
	 * @param textureViewWidth  The width of the texture view relative to sensor coordinate
	 * @param textureViewHeight The height of the texture view relative to sensor coordinate
	 * @param maxWidth          The maximum width that can be chosen
	 * @param maxHeight         The maximum height that can be chosen
	 * @param aspectRatio       The aspect ratio
	 * @return The optimal {@code Size}, or an arbitrary one if none were big enough
	 */
	private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio)
	{
		// Collect the supported resolutions that are at least as big as the preview Surface
		List<Size> bigEnough = new ArrayList<>();
		// Collect the supported resolutions that are smaller than the preview Surface
		List<Size> notBigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for(Size option : choices)
		{
			if(option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
					option.getHeight() == option.getWidth() * h / w)
			{
				if(option.getWidth() >= textureViewWidth &&
						option.getHeight() >= textureViewHeight)
				{
					bigEnough.add(option);
				}
				else
				{
					notBigEnough.add(option);
				}
			}
		}

		// Pick the smallest of those big enough. If there is no one big enough, pick the
		// largest of those not big enough.
		if(bigEnough.size() > 0)
		{
			return Collections.min(bigEnough, new CompareSizesByArea());
		}
		else if(notBigEnough.size() > 0)
		{
			return Collections.max(notBigEnough, new CompareSizesByArea());
		}
		else
		{
			Log.e(TAG, "Couldn't find any suitable preview size");
			return choices[0];
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.camera_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		cameraButton = view.findViewById(R.id.picture);
		cameraButton.setOnClickListener(this);
		textureView = (TextureView) view.findViewById(R.id.texture);
		mPhotoGrid = (RecyclerView) view.findViewById(R.id.stored_images);

		mPhotoGrid.setLayoutManager(getLayoutManager());
		mPhotoGrid.getItemAnimator().setChangeDuration(0);
		mPhotoAdapter = new GridImageAdapter();
		mPhotoGrid.setAdapter(mPhotoAdapter);

		if(mPhotoAdapter.displayingImages())
		{
			int sw = getScreenWidth();
			int fourFourDp = (int) getResources().getDisplayMetrics().density * 44;
			int eightyDp = (int) getResources().getDisplayMetrics().density * 80;
			cameraButton.setTranslationY(cameraButton.getY() - fourFourDp);
			cameraButton.setTranslationX(cameraButton.getX() + (sw / 2) - eightyDp);
		}
	}

	private GridLayoutManager getLayoutManager()
	{
		return new GridLayoutManager(getActivity(), getSpanCount(), LinearLayoutManager.VERTICAL, false);
	}

	private int getScreenWidth()
	{
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.x;
	}

	private int getSpanCount()
	{
		return (int) (getScreenWidth() / (getResources().getDisplayMetrics().density * 80));
	}

	protected class GridImageAdapter extends RecyclerView.Adapter<GridImageAdapter.ImageViewHolder>
	{
		@Override
		public GridImageAdapter.ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
		{
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_image, parent, false);
			return new ImageViewHolder(view);
		}

		@Override
		public void onBindViewHolder(GridImageAdapter.ImageViewHolder holder, int position)
		{
			holder.bind(getFile(position), position);
		}

		@Override
		public int getItemCount()
		{
			File dir = getActivity().getExternalFilesDir(null);
			return dir == null ? 0 : dir.listFiles().length;
		}

		public String getFile(int position)
		{
			File dir = getActivity().getExternalFilesDir(null);
			if(dir == null)
				return "";

			return dir.listFiles()[position].getPath();
		}

		public boolean displayingImages()
		{
			return getItemCount() > 0;
		}

		public class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
		{
			private ImageView mImageView;
			private ImageView mDeleteView;
			private int position;

			public ImageViewHolder(View itemView)
			{
				super(itemView);
				mImageView = (ImageView) itemView.findViewById(R.id.image);
				mDeleteView = (ImageView) itemView.findViewById(R.id.delete);
				mImageView.setOnClickListener(this);
			}

			/**
			 * Binds the image to the view
			 *
			 * @param image The path to the image
			 */
			public void bind(String image, final int position)
			{
				this.position = position;
				if(TextUtils.isEmpty(image))
				{
					mImageView.setImageResource(0);
					return;
				}

				int eighty = (int) getResources().getDisplayMetrics().density * 80;
				Picasso.with(mImageView.getContext()).load(new File(image))
						.memoryPolicy(MemoryPolicy.NO_CACHE)
						.resize(eighty, eighty).centerInside().into(mImageView);

				mDeleteView.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						view.getContext().getExternalFilesDir(null).listFiles()[position].delete();
						notifyItemRemoved(position);

						int count = getItemCount();
						if(count > 1)
							notifyItemRangeChanged(position, count - position);
						else
							notifyItemChanged(0);
					}
				});

				int scale = getItemCount() < 2 ? 0 : 1;
				if(mDeleteView.getScaleX() != scale)
					mDeleteView.animate().scaleX(scale).scaleY(scale).start();
			}

			@Override
			public void onClick(View view)
			{
				ActivityOptionsCompat optionsCompat;

				Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
				optionsCompat = ActivityOptionsCompat.makeThumbnailScaleUpAnimation(view,
						bitmap, (int) view.getX(), (int) view.getY());

				Intent intent = new Intent(getView().getContext(), ImagesActivity.class);
				intent.putExtra(ImagesActivity.INDEX, position);
//				optionsCompat = ActivityOptionsCompat.makeClipRevealAnimation(view,
//						(int) view.getX(), (int) view.getY(), view.getWidth(), view.getHeight());

//				ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), mImageView, mImageView.getTransitionName());
				ActivityCompat.startActivity(getActivity(), intent, optionsCompat.toBundle());
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		((GridLayoutManager) mPhotoGrid.getLayoutManager()).setSpanCount(getSpanCount());
	}

	@Override
	public void onResume()
	{
		super.onResume();
		startBackgroundThread();

		// When the screen is turned off and turned back on, the SurfaceTexture is already
		// available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
		// a camera and start preview from here (otherwise, we wait until the surface is ready in
		// the SurfaceTextureListener).
		if(textureView.isAvailable())
		{
			openCamera(textureView.getWidth(), textureView.getHeight());
		}
		else
		{
			textureView.setSurfaceTextureListener(mSurfaceTextureListener);
		}
	}

	@Override
	public void onPause()
	{
		closeCamera();
		stopBackgroundThread();
		super.onPause();
	}

	private void requestCameraPermission()
	{
		if(FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
		{
			new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
		}
		else
		{
			FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
					REQUEST_CAMERA_PERMISSION);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults)
	{
		if(requestCode == REQUEST_CAMERA_PERMISSION)
		{
			if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
			{
				//TODO request permission
//				ErrorDialog.newInstance(getString(R.string.request_permission))
//						.show(getChildFragmentManager(), FRAGMENT_DIALOG);
			}
		}
		else
		{
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	/**
	 * Sets up member variables related to camera.
	 *
	 * @param width  The width of available size for camera preview
	 * @param height The height of available size for camera preview
	 */
	private void setUpCameraOutputs(int width, int height)
	{
		Activity activity = getActivity();
		CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
		try
		{
			for(String cameraId : manager.getCameraIdList())
			{
				CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

				// We don't use a front facing camera in this sample.
				Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
				if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
				{
					continue;
				}

				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if(map == null)
				{
					continue;
				}

				// For still image captures, we use the largest available size.
				Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
				mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 10);
				mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

				// Find out if we need to swap dimension to get the preview size relative to sensor
				// coordinate.
				int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
				//noinspection ConstantConditions
				mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
				boolean swappedDimensions = false;
				switch(displayRotation)
				{
					case Surface.ROTATION_0:
					case Surface.ROTATION_180:
						if(mSensorOrientation == 90 || mSensorOrientation == 270)
						{
							swappedDimensions = true;
						}
						break;
					case Surface.ROTATION_90:
					case Surface.ROTATION_270:
						if(mSensorOrientation == 0 || mSensorOrientation == 180)
						{
							swappedDimensions = true;
						}
						break;
					default:
						Log.e(TAG, "Display rotation is invalid: " + displayRotation);
				}

				Point displaySize = new Point();
				activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
				int rotatedPreviewWidth = width;
				int rotatedPreviewHeight = height;
				int maxPreviewWidth = displaySize.x;
				int maxPreviewHeight = displaySize.y;

				if(swappedDimensions)
				{
					rotatedPreviewWidth = height;
					rotatedPreviewHeight = width;
					maxPreviewWidth = displaySize.y;
					maxPreviewHeight = displaySize.x;
				}

				if(maxPreviewWidth > MAX_PREVIEW_WIDTH)
				{
					maxPreviewWidth = MAX_PREVIEW_WIDTH;
				}

				if(maxPreviewHeight > MAX_PREVIEW_HEIGHT)
				{
					maxPreviewHeight = MAX_PREVIEW_HEIGHT;
				}

				// Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
				// bus' bandwidth limitation, resulting in gorgeous previews but the storage of
				// garbage capture data.
				mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
						rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
						maxPreviewHeight, largest);

				// Check if the flash is supported.
				Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
				mFlashSupported = available == null ? false : available;

				mCameraId = cameraId;
				return;
			}
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
		catch(NullPointerException e)
		{
			// Currently an NPE is thrown when the Camera2API is used but not supported on the
			// device this code runs.
			//TODO handle this error
//			ErrorDialog.newInstance(getString(R.string.camera_error))
//					.show(getChildFragmentManager(), FRAGMENT_DIALOG);
		}
	}

	/**
	 * Opens the camera specified.
	 */
	private void openCamera(int width, int height)
	{
		if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED)
		{
			requestCameraPermission();
			return;
		}
		setUpCameraOutputs(width, height);
		configureTransform(width, height);
		Activity activity = getActivity();
		CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
		try
		{
			if(!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
			{
				throw new RuntimeException("Time out waiting to lock camera opening.");
			}
			manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
		}
	}

	/**
	 * Closes the current {@link CameraDevice}.
	 */
	private void closeCamera()
	{
		try
		{
			mCameraOpenCloseLock.acquire();
			if(null != mCaptureSession)
			{
				mCaptureSession.close();
				mCaptureSession = null;
			}
			if(null != mCameraDevice)
			{
				mCameraDevice.close();
				mCameraDevice = null;
			}
			if(null != mImageReader)
			{
				mImageReader.close();
				mImageReader = null;
			}
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
		}
		finally
		{
			mCameraOpenCloseLock.release();
		}
	}

	/**
	 * Starts a background thread and its {@link Handler}.
	 */
	private void startBackgroundThread()
	{
		mBackgroundThread = new HandlerThread("CameraBackground");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	/**
	 * Stops the background thread and its {@link Handler}.
	 */
	private void stopBackgroundThread()
	{
		mBackgroundThread.quitSafely();
		try
		{
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new {@link CameraCaptureSession} for camera preview.
	 */
	private void createCameraPreviewSession()
	{
		try
		{
			SurfaceTexture texture = textureView.getSurfaceTexture();
			assert texture != null;

			// We configure the size of default buffer to be the size of camera preview we want.
			texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

			// This is the output Surface we need to start preview.
			Surface surface = new Surface(texture);

			// We set up a CaptureRequest.Builder with the output Surface.
			mPreviewRequestBuilder
					= mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			mPreviewRequestBuilder.addTarget(surface);

			// Here, we create a CameraCaptureSession for camera preview.
			mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
					new CameraCaptureSession.StateCallback()
					{

						@Override
						public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
						{
							// The camera is already closed
							if(null == mCameraDevice)
							{
								return;
							}

							// When the session is ready, we start displaying the preview.
							mCaptureSession = cameraCaptureSession;
							try
							{
								// Auto focus should be continuous for camera preview.
								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
								// Flash is automatically enabled when necessary.
								setAutoFlash(mPreviewRequestBuilder);

								// Finally, we start displaying the camera preview.
								mPreviewRequest = mPreviewRequestBuilder.build();
								mCaptureSession.setRepeatingRequest(mPreviewRequest,
										mCaptureCallback, mBackgroundHandler);
							}
							catch(CameraAccessException e)
							{
								e.printStackTrace();
							}
						}

						@Override
						public void onConfigureFailed(
								@NonNull CameraCaptureSession cameraCaptureSession)
						{
							showToast("Failed");
						}
					}, null
			);
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Configures the necessary {@link android.graphics.Matrix} transformation to `textureView`.
	 * This method should be called after the camera preview size is determined in
	 * setUpCameraOutputs and also the size of `textureView` is fixed.
	 *
	 * @param viewWidth  The width of `textureView`
	 * @param viewHeight The height of `textureView`
	 */
	private void configureTransform(int viewWidth, int viewHeight)
	{
		Activity activity = getActivity();
		if(null == textureView || null == mPreviewSize || null == activity)
		{
			return;
		}
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		Matrix matrix = new Matrix();
		RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
		RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
		float centerX = viewRect.centerX();
		float centerY = viewRect.centerY();
		if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation)
		{
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
			float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
			matrix.postScale(scale, scale, centerX, centerY);
			matrix.postRotate(90 * (rotation - 2), centerX, centerY);
		}
		else if(Surface.ROTATION_180 == rotation)
		{
			matrix.postRotate(180, centerX, centerY);
		}
		textureView.setTransform(matrix);
	}

	/**
	 * Initiate a still image capture.
	 */
	private void takePicture()
	{
		lockFocus();
	}

	/**
	 * Lock the focus as the first step for a still image capture.
	 */
	private void lockFocus()
	{
		try
		{
			// This is how to tell the camera to lock focus.
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
			// Tell #mCaptureCallback to wait for the lock.
			mState = STATE_WAITING_LOCK;
			mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Run the precapture sequence for capturing a still image. This method should be called when
	 * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
	 */
	private void runPrecaptureSequence()
	{
		try
		{
			// This is how to tell the camera to trigger.
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
			// Tell #mCaptureCallback to wait for the precapture sequence to be set.
			mState = STATE_WAITING_PRECAPTURE;
			mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Capture a still picture. This method should be called when we get a response in
	 * {@link #mCaptureCallback} from both {@link #lockFocus()}.
	 */
	private void captureStillPicture()
	{
		if(capturing)
			return;

		try
		{
			capturing = true;
			final Activity activity = getActivity();
			if(null == activity || null == mCameraDevice)
				return;
			// This is the CaptureRequest.Builder that we use to take a picture.
			final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureBuilder.addTarget(mImageReader.getSurface());

			// Use the same AE and AF modes as the preview.
			captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			setAutoFlash(captureBuilder);

			// Orientation
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

			CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback()
			{
				@Override
				public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
											   @NonNull TotalCaptureResult result)
				{
					showToast("Saved: new photo");// + mFile);
					Log.d(TAG, "Saved new photo");//mFile.toString());
					unlockFocus();
					capturing = false;
				}
			};

			mCaptureSession.stopRepeating();
			mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves the JPEG orientation from the specified screen rotation.
	 *
	 * @param rotation The screen rotation.
	 * @return The JPEG orientation (one of 0, 90, 270, and 360)
	 */
	private int getOrientation(int rotation)
	{
		// Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
		// We have to take that into account and rotate JPEG properly.
		// For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
		// For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
		return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
	}

	/**
	 * Unlock the focus. This method should be called when still image capture sequence is
	 * finished.
	 */
	private void unlockFocus()
	{
		try
		{
			// Reset the auto-focus trigger
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
			setAutoFlash(mPreviewRequestBuilder);
			mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
			// After this, the camera will go back to the normal state of preview.
			mState = STATE_PREVIEW;
			mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
		}
		catch(CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View view)
	{
		takePicture();
	}

	private void animateCameraButton()
	{
		if(cameraButton.getTranslationY() != 0)
			return;

		Path animationPath = new Path();
		int cy = (int) cameraButton.getY();
		int cx = (int) cameraButton.getX();
		animationPath.moveTo(cameraButton.getX(), cy);
		int sw = getScreenWidth();
		int fourFourDp = (int) getResources().getDisplayMetrics().density * 44;
		int eightyDp = (int) getResources().getDisplayMetrics().density * 80;

		animationPath.cubicTo(cx, cy, sw - eightyDp / 2, cy, sw - eightyDp, cy - fourFourDp);
		Animator pathAnimator = ObjectAnimator.ofFloat(cameraButton, View.X, View.Y, animationPath);
		pathAnimator.setDuration(500);
		pathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		pathAnimator.start();
	}

	private void setAutoFlash(CaptureRequest.Builder requestBuilder)
	{
//		if(mFlashSupported)
//		{
//			requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//					CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//		}
	}

	/**
	 * Saves a JPEG {@link Image} into the specified {@link File}.
	 */
	private static class ImageSaver implements Runnable
	{
		/**
		 * The JPEG image
		 */
		private final Image mImage;

		private final String mFilePath;
		private ImageSavedListener mListener;
		private int mPosition;

		public ImageSaver(Image image, String filePath, int position, ImageSavedListener listener)
		{
			mImage = image;
			mFilePath = filePath;
			mListener = listener;
			mPosition = position;
		}

		@Override
		public void run()
		{
			File file = new File(mFilePath);
			ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			FileOutputStream output = null;
			try
			{
				output = new FileOutputStream(file);
				output.write(bytes);
				mListener.imageSaved(mPosition);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				mImage.close();
				if(null != output)
				{
					try
					{
						output.close();
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}

		public interface ImageSavedListener
		{
			void imageSaved(int position);
		}
	}

	/**
	 * Compares two {@code Size}s based on their areas.
	 */
	static class CompareSizesByArea implements Comparator<Size>
	{
		@Override
		public int compare(Size lhs, Size rhs)
		{
			// We cast here to ensure the multiplications won't overflow
			return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
					(long) rhs.getWidth() * rhs.getHeight());
		}

	}

	/**
	 * Shows an error message dialog.
	 */
	public static class ErrorDialog extends DialogFragment
	{
		private static final String ARG_MESSAGE = "message";

		public static ErrorDialog newInstance(String message)
		{
			ErrorDialog dialog = new ErrorDialog();
			Bundle args = new Bundle();
			args.putString(ARG_MESSAGE, message);
			dialog.setArguments(args);
			return dialog;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			final Activity activity = getActivity();
			return new AlertDialog.Builder(activity)
					.setMessage(getArguments().getString(ARG_MESSAGE))
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							activity.finish();
						}
					})
					.create();
		}
	}

	/**
	 * Shows OK/Cancel confirmation dialog about camera permission.
	 */
	public static class ConfirmationDialog extends DialogFragment
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			final Fragment parent = getParentFragment();
			return new AlertDialog.Builder(getActivity())
//					.setMessage(R.string.request_permission) TODO request permission
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							FragmentCompat.requestPermissions(parent,
									new String[]{Manifest.permission.CAMERA},
									REQUEST_CAMERA_PERMISSION);
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							Activity activity = parent.getActivity();
							if(activity != null)
							{
								activity.finish();
							}
						}
					})
					.create();
		}
	}
}
