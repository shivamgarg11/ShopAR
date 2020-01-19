package com.example.deon.furnituar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.CaptureScreenCallback;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import com.wikitude.architect.StartupConfiguration.CameraPosition;

public class SampleCamActivity extends AbstractArchitectCamActivity implements SensorEventListener{


	private long lastCalibrationToastShownTimeMillis = System.currentTimeMillis();

	@Override
	public String getARchitectWorldPath() {
		return "render_furniture/index.html";
	}

	@Override
	public String getActivityTitle() {
		return "RenderFurniture";
	}

	@Override
	public int getContentViewId() {
		return R.layout.activity_render_furniture;
	}

	@Override
	public int getArchitectViewId() {
		return R.id.architectView;
	}

	@Override
	public String getWikitudeSDKLicenseKey() {
		return WikitudeSDKConstants.WIKITUDE_SDK_KEY;
	}

	@Override
	public SensorAccuracyChangeListener getSensorAccuracyListener() {
		return new SensorAccuracyChangeListener() {
			@Override
			public void onCompassAccuracyChanged( int accuracy ) {
				/* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
				if ( accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && SampleCamActivity.this != null && !SampleCamActivity.this.isFinishing() && System.currentTimeMillis() - SampleCamActivity.this.lastCalibrationToastShownTimeMillis > 5 * 1000) {
					Toast.makeText( SampleCamActivity.this, R.string.compass_accuracy_low, Toast.LENGTH_LONG ).show();
					SampleCamActivity.this.lastCalibrationToastShownTimeMillis = System.currentTimeMillis();
				}
			}
		};
	}

	@Override
	public ArchitectUrlListener getUrlListener() {
		return new ArchitectUrlListener() {

			@Override
			public boolean urlWasInvoked(String uriString) {
				Uri invokedUri = Uri.parse(uriString);

				// pressed "More" button on POI-detail panel
				if ("markerselected".equalsIgnoreCase(invokedUri.getHost())) {
					return false;
				}

				else if ("button".equalsIgnoreCase(invokedUri.getHost())) {
					SampleCamActivity.this.architectView.captureScreen(ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM_AND_WEBVIEW, new CaptureScreenCallback() {

						@Override
						public void onScreenCaptured(final Bitmap screenCapture) {
							final File screenCaptureFile = new File(Environment.getExternalStorageDirectory().toString(), "screenCapture_" + System.currentTimeMillis() + ".jpg");

							try {
								final FileOutputStream out = new FileOutputStream(screenCaptureFile);
								screenCapture.compress(Bitmap.CompressFormat.JPEG, 90, out);
								out.flush();
								out.close();

								final Intent share = new Intent(Intent.ACTION_SEND);
								share.setType("image/jpg");
								share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenCaptureFile));

								final String chooserTitle = "Share Snaphot";
								SampleCamActivity.this.startActivity(Intent.createChooser(share, chooserTitle));

							} catch (final Exception e) {
								SampleCamActivity.this.runOnUiThread(new Runnable() {

									@Override
									public void run() {
										Toast.makeText(SampleCamActivity.this, "Unexpected error, " + e, Toast.LENGTH_LONG).show();
									}
								});
							}
						}
					});
				}
				return true;
			}
		};
	}

	@Override
	public ILocationProvider getLocationProvider(final LocationListener locationListener) {
		return new LocationProvider(this, locationListener);
	}

	@Override
	public float getInitialCullingDistanceMeters() {
		return ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS;
	}

	@Override
	protected boolean hasGeo() {
		return true;
	}

	@Override
	protected boolean hasIR() {
		return true;
	}

	@Override
	protected CameraPosition getCameraPosition() {

		return CameraPosition.DEFAULT;
	}

	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;
	float azimuth;
	private String jsonString;

	public void onAccuracyChanged(Sensor sensor, int accuracy) { }

	float[] mGravity;
	float[] mGeomagnetic;
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				float newAzimuth = (orientation[0] < 0) ? 2 * (float)Math.PI + orientation[0] : orientation[0];
				float diffAzimuth = newAzimuth - getIntent().getExtras().getFloat(BrowseFurnitureActivity.AZIMUTH);
				azimuth = diffAzimuth;
				String[] callJSArg = {Float.toString(azimuth)};
				callJavaScript("World.setBearingExternally", callJSArg);
				try {
					writeBearingToJSON(azimuth);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String getJSON() {
		return jsonString;
	}

	ServerSocket serverSocket;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			WebView.setWebContentsDebuggingEnabled(true);
		}

		Thread socketServerThread = new Thread(new SocketServerThread(this));
		socketServerThread.start();
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		String[] filename = {"\"" + getIntent().getExtras().getString(BrowseFurnitureActivity.SELECTED_FURNITURE) + "\""};
		Log.d("selected_furniture", getIntent().getExtras().getString(BrowseFurnitureActivity.SELECTED_FURNITURE));
		callJavaScript("World.init", filename);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	private void writeBearingToJSON(Float azimuth) throws IOException {
		File file = new File(getExternalCacheDir(), "bearing.json");
		FileOutputStream stream = new FileOutputStream(file);
		jsonString = "{\"bearing\": " + azimuth + ",\n" +
				"\"selection\": \"" + getIntent().getExtras().getString(BrowseFurnitureActivity.SELECTED_FURNITURE)+ "\"}";
		try {
			stream.write(jsonString.getBytes());
		} finally {
			stream.close();
		}
	}

	@Override
	protected  void onDestroy() {
		super.onDestroy();

		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
