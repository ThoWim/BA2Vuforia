package at.fhooe.mc.ba2.wimmer.vuforia;

import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.MyRenderer;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour.CloudRecognitionBehaviour;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour.CylinderTargetBehaviour;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour.ImageTargetBehaviour;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour.MultiTargetBehaviour;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour.NullBehaviour;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.Texture;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.VuforiaSampleGLView;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.DataSet;
import com.qualcomm.vuforia.ImageTracker;
import com.qualcomm.vuforia.PIXEL_FORMAT;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.STORAGE_TYPE;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.TargetFinder;
import com.qualcomm.vuforia.TargetSearchResult;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vec2I;
import com.qualcomm.vuforia.VideoBackgroundConfig;
import com.qualcomm.vuforia.VideoMode;
import com.qualcomm.vuforia.Vuforia;
import com.qualcomm.vuforia.Vuforia.UpdateCallbackInterface;

public class MainActivity extends Activity implements UpdateCallbackInterface {

	private static final String LOGTAG = "ImageTargets";

	// Access keys for the cloud databse
	private static final String kAccessKey = "fda945d97210ddda29a57429b1d50b1cc17c1d27";
	private static final String kSecretKey = "37d618ce9b92f2c7ae23fbc520037b73fc13d913";

	// Vuforia Memberts
	private TrackerManager mTrackerManager;
	private ImageTracker mTracker;
	private MyRenderer mRenderer;
	private VuforiaSampleGLView mGlView;

	// Helper members
	private DataSet mCurrentDataset;
	private boolean mExtendedTracking = false;
	private Vector<Texture> mTextures;
	private boolean mCameraRunning = false;
	private ProgressDialog mProgressDialog;
	private boolean mInitialized = false;

	// Display size of the device:
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;

	// Member for custom layout
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
	private CharSequence mTitle;
	private CharSequence mDrawerTitle;
	private String[] mTitles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showProgressDialog(true);
		setContentView(R.layout.main);

		initBA2Layout();

		// Query display dimensions:
		DisplayMetrics metrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;

		// Load any sample specific textures:
		mTextures = new Vector<Texture>();
		loadTextures();

		Vuforia.registerCallback(this);

		// Starts the initialization in the backgrounds
		new InitTask().execute();
	}

	/**
	 * Initializes the activities layout.
	 */
	private void initBA2Layout() {
		mTitles = getResources().getStringArray(R.array.drawer_items_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		NavigationDrawerAdapter adapter = new NavigationDrawerAdapter(this,
				R.layout.drawer_item, mTitles);
		mDrawerList.setAdapter(adapter);

		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mTitle = mDrawerTitle = getTitle();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getActionBar().setTitle(mTitle);
			}

			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(mDrawerTitle);
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			// Stop cloud recognition
			mTracker.getTargetFinder().stop();
			mTracker.getTargetFinder().clearTrackables();

			// Activate local dataSet
			mTracker.activateDataSet((mCurrentDataset));

			switch (position) {
			case 0:
				mRenderer.setBehaviour(ImageTargetBehaviour.class);
				break;
			case 1:
				mRenderer.setBehaviour(CylinderTargetBehaviour.class);
				break;
			case 2:
				mRenderer.setBehaviour(MultiTargetBehaviour.class);
				break;
			case 3:
				// Start cloud based recognition
				mTracker.deactivateDataSet(mCurrentDataset);
				mTracker.getTargetFinder().startRecognition();
				mRenderer.setBehaviour(CloudRecognitionBehaviour.class);
				break;
			}

			mDrawerList.setItemChecked(position, true);
			setTitle(mTitles[position]);
			mDrawerLayout.closeDrawer(mDrawerList);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
		case R.id.action_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mGlView != null) {
			mGlView.setVisibility(View.INVISIBLE);
			mGlView.onPause();
		}

		if (mInitialized) {

			if (mTracker != null) {
				mTracker.stop();
			}
			CameraDevice.getInstance().stop();
			CameraDevice.getInstance().deinit();
		}

		Vuforia.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		Vuforia.onResume();

		if (mInitialized) {
			this.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
		}

		if (mGlView != null) {
			mGlView.setVisibility(View.VISIBLE);
			mGlView.onResume();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mInitialized) {

			if (mTracker != null) {
				mTracker.stop();
			}
			CameraDevice.getInstance().stop();
			CameraDevice.getInstance().deinit();
		}

		if (mTrackerManager != null) {
			mTrackerManager.deinitTracker(ImageTracker.getClassType());
		}

		Vuforia.deinit();
	}

	public void showProgressDialog(boolean show) {
		if (show) {
			mProgressDialog = ProgressDialog.show(MainActivity.this,
					"Please wait...", "Loading in progress...");
		} else {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
			}
		}
	}

	/**
	 * Async initialization task
	 */
	private class InitTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// Do the initialization task in the background
			int progress = 0;
			Vuforia.setInitParameters(MainActivity.this, Vuforia.GL_20);
			do {
				progress = Vuforia.init();
			} while (progress >= 0 && progress < 100);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mTrackerManager = TrackerManager.getInstance();

			// Trying to initialize the image tracker
			mTracker = (ImageTracker) mTrackerManager.initTracker(ImageTracker
					.getClassType());
			if (mTracker != null) {
				new TrackerDataLoadingTask().execute("Targets.xml");
			}
		}
	}

	/**
	 * Async task for the data loading
	 */
	private class TrackerDataLoadingTask extends
			AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			// Init local tracker database
			if (mTracker == null)
				return false;

			if (mCurrentDataset == null)
				mCurrentDataset = mTracker.createDataSet();

			if (mCurrentDataset == null)
				return false;

			if (!mCurrentDataset.load(params[0],
					STORAGE_TYPE.STORAGE_APPRESOURCE))
				return false;

			if (!mTracker.activateDataSet(mCurrentDataset))
				return false;

			for (int count = 0; count < mCurrentDataset.getNumTrackables(); count++) {
				Trackable trackable = mCurrentDataset.getTrackable(count);
				String name = "Current Dataset : " + trackable.getName();
				trackable.setUserData(name);
				Log.d(LOGTAG, "UserData:Set the following user data "
						+ (String) trackable.getUserData());
			}

			// Init cloud recognition
			TargetFinder targetFinder = mTracker.getTargetFinder();

			if (targetFinder.startInit(kAccessKey, kSecretKey)) {
				targetFinder.waitUntilInitFinished();
			}

			int resultCode = targetFinder.getInitState();
			if (resultCode != TargetFinder.INIT_SUCCESS) {
				if (resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION) {
					Log.d(LOGTAG, "Update Error: No network connection");
				} else {
					Log.d(LOGTAG, "Update Error: Service not available");
				}

				Log.e(LOGTAG, "Failed to initialize target finder.");
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				new TrackerDataLoadingTask().execute("Targets.xml");
				initializationDone();
			} else {
				Log.d(LOGTAG, "Error with loading tracker data");
			}
		}

	}

	/**
	 * Gets called when the TrackerDataLoadingTask is finished
	 */
	private void initializationDone() {
		mInitialized = true;
		initAR();

		mRenderer.mIsActive = true;

		// Now add the GL surface view. It is important
		// that the OpenGL ES surface view gets added
		// BEFORE the camera is started and video
		// background is configured.
		FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_frame);
		contentFrame.addView(mGlView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		// // Sets the UILayout to be drawn in front of the camera
		// mUILayout.bringToFront();
		//
		// // Sets the layout background to transparent
		// mUILayout.setBackgroundColor(Color.TRANSPARENT);

		this.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);

		boolean result = CameraDevice.getInstance().setFocusMode(
				CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

		if (!result) {
			Log.e(LOGTAG, "Unable to enable continuous autofocus");
		}
	}

	/**
	 * Initializes AR components.
	 */
	private void initAR() {
		// Create OpenGL ES view:
		int depthSize = 16;
		int stencilSize = 0;
		boolean translucent = Vuforia.requiresAlpha();

		mGlView = new VuforiaSampleGLView(this);
		mGlView.init(translucent, depthSize, stencilSize);

		mRenderer = new MyRenderer(this);
		mRenderer.setTextures(mTextures);
		mRenderer.setBehaviour(NullBehaviour.class);
		mGlView.setRenderer(mRenderer);
	}

	/**
	 * We want to load specific textures from the APK, which we will later use
	 * for rendering.
	 */
	private void loadTextures() {
		mTextures.add(Texture.loadTextureFromApk("TextureTeapotBlue.png",
				getAssets()));
		mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png",
				getAssets()));
		mTextures.add(Texture.loadTextureFromApk("TextureWireframe.png",
				getAssets()));
		mTextures.add(Texture.loadTextureFromApk("sphere.png", getAssets()));
	}

	/**
	 * Starts Vuforia, initialize and starts the camera and start the trackers.
	 */
	public void startAR(int camera) {
		String error;
		if (mCameraRunning) {
			error = "Camera already running, unable to open again";
			Log.e(LOGTAG, error);
		}

		if (!CameraDevice.getInstance().init(camera)) {
			error = "Unable to open camera device: " + camera;
			Log.e(LOGTAG, error);
		}

		configureVideoBackground();

		if (!CameraDevice.getInstance().selectVideoMode(
				CameraDevice.MODE.MODE_DEFAULT)) {
			error = "Unable to set video mode";
			Log.e(LOGTAG, error);
		}

		if (!CameraDevice.getInstance().start()) {
			error = "Unable to start camera device: " + camera;
			Log.e(LOGTAG, error);
		}

		Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

		startTracker();

		mCameraRunning = true;

		CameraDevice.getInstance().setFocusMode(
				(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO));
	}

	/**
	 * Starts the trackers.
	 */
	private boolean startTracker() {
		// Indicate if the trackers were started correctly
		boolean result = true;

		mTracker = (ImageTracker) mTrackerManager.getTracker(ImageTracker
				.getClassType());
		if (mTracker != null)
			mTracker.start();

		return result;
	}

	/**
	 * Configures the video mode and sets offsets for the camera's image
	 */
	private void configureVideoBackground() {
		VideoMode vm = CameraDevice.getInstance().getVideoMode(
				CameraDevice.MODE.MODE_DEFAULT);

		VideoBackgroundConfig config = new VideoBackgroundConfig();
		config.setEnabled(true);
		config.setSynchronous(true);
		config.setPosition(new Vec2I(0, 0));

		int xSize = mScreenWidth;
		int ySize = (int) (mScreenWidth * (vm.getWidth() / (float) vm
				.getHeight()));

		config.setSize(new Vec2I(xSize, ySize));

		Log.i(LOGTAG, "Configure Video Background : Video (" + vm.getWidth()
				+ " , " + vm.getHeight() + "), Screen (" + mScreenWidth + " , "
				+ mScreenHeight + "), mSize (" + xSize + " , " + ySize + ")");

		Renderer.getInstance().setVideoBackgroundConfig(config);

	}

	@Override
	public void QCAR_onUpdate(State arg0) {
		// Get the tracker manager:
		TrackerManager trackerManager = TrackerManager.getInstance();

		// Get the image tracker:
		ImageTracker imageTracker = (ImageTracker) trackerManager
				.getTracker(ImageTracker.getClassType());

		// Get the target finder:
		TargetFinder finder = imageTracker.getTargetFinder();

		// Check if there are new results available:
		final int statusCode = finder.updateSearchResults();

		// Show a message if we encountered an error:
		if (statusCode < 0) {
			Log.e(this.getClass().getName(),
					"Error occurred by updating TargetFinder. StatusCode: "
							+ statusCode);

		} else if (statusCode == TargetFinder.UPDATE_RESULTS_AVAILABLE) {
			// Process new search results
			if (finder.getResultCount() > 0) {
				TargetSearchResult result = finder.getResult(0);

				// Check if this target is suitable for tracking:
				if (result.getTrackingRating() > 0) {
					Trackable trackable = finder.enableTracking(result);

					if (mExtendedTracking)
						trackable.startExtendedTracking();
				}
			}
		}
	}
}