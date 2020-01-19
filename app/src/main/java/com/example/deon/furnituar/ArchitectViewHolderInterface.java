package com.example.deon.furnituar;

import android.location.LocationListener;

import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;

public interface ArchitectViewHolderInterface {

	public static final int CULLING_DISTANCE_DEFAULT_METERS = 50 * 1000;
	
	 
	public String getARchitectWorldPath();
	
	 
	public ArchitectUrlListener getUrlListener();
	
	 
	public int getContentViewId();
	
	 
	public String getWikitudeSDKLicenseKey();
	
	 
	public int getArchitectViewId();

	 
	public ILocationProvider getLocationProvider(final LocationListener locationListener);
	
	 
	public SensorAccuracyChangeListener getSensorAccuracyListener();
	
	 
	public float getInitialCullingDistanceMeters();
	
	 
	public static interface ILocationProvider {

		 
		public void onResume();

		 
		public void onPause();

	}

}
