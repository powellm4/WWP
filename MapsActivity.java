package powellmarshall3.bellinghambathrooms;


        import android.content.Context;
        import android.content.IntentSender;
        import android.content.SharedPreferences;
        import android.location.Location;
        import android.os.Build;
        import android.os.Bundle;
        import android.preference.PreferenceManager;
        import android.provider.Settings;
        import android.support.v4.app.FragmentActivity;
        import android.text.TextUtils;
        import android.util.Log;

        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.api.GoogleApiClient;
        import com.google.android.gms.location.LocationListener;
        import com.google.android.gms.location.LocationRequest;
        import com.google.android.gms.location.LocationServices;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.BitmapDescriptor;
        import com.google.android.gms.maps.model.BitmapDescriptorFactory;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.MarkerOptions;

        import java.util.Scanner;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String TAG = "MARSHALL";
    private LocationRequest mLocationRequest;
    SharedPreferences sharedPreferences;
    private int locationCount =0;
    private boolean markersOnMap = false;



    private void handleNewLocation(Location location){
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng;
        latLng= new LatLng(currentLatitude,currentLongitude);
        /* mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 19)); */

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        /* debug use only! */
        boolean isFirstRun = sharedPreferences.getBoolean("firstRun",true);
        if (isFirstRun)
        {
            loadFromResRaw();
        }
        setUpMapIfNeeded();
        mMap.setMyLocationEnabled(true);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);
        LatLng latLng = new LatLng(48.737026, -122.485413);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
    }




    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();


        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }



    /**
     *      drawMarker(point)
     *      args: point (lat,long)
     *      places marker on map at coordinates (arg0)
     */
    private void drawMarker(double lat, double lng){
        String name;
        String info;
        name = sharedPreferences.getString(lat+","+lng,"");
        info = sharedPreferences.getString(lat+","+lng+"i","");
        int id = getResources().getIdentifier(name, "drawable", this.getPackageName());
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(id);
        MarkerOptions markerOptions = new MarkerOptions();
        LatLng point = new LatLng(lat,lng);
        markerOptions.position(point);
        markerOptions.icon(icon);
        markerOptions.title(info);
        mMap.addMarker(markerOptions);
    }

    /**
     *  addMarkers()
     *  fetches all markers from sharedpreferences
     *  adds markers to the map
     */
    private void addMarkers()
    {
        locationCount = sharedPreferences.getInt("locationCount", 0);

        if (locationCount != 0) {
            double lat=0;
            double lng=0;
            for (int i = 0; i < locationCount; i++) {
                lat = getDouble(sharedPreferences,"lat" + i, 0);
                lng = getDouble(sharedPreferences, "lng" + i, 0);
                drawMarker(lat, lng);
            }
        }

    }

    /**
     *  loadFromResRaw()
     *  reads latitude and longitude values from resraw file "locations.txt"
     *  into sharedPreferences.
     *  Only used on first open of app
     */
    private void loadFromResRaw()
    {
        Log.d(TAG,"loadFromResRaw");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Scanner console = new Scanner(getResources().openRawResource(R.raw.locations));
        String pattern = "[A-Z]+";
        int i=0;
        while(console.hasNextLine())
        {

            double lat = Double.parseDouble(console.next());
            double lng = Double.parseDouble(console.next());

            String icon = console.next();
            String info = console.nextLine().trim();
            putDouble(editor, "lat" + i, lat);
            putDouble(editor, "lng" + i, lng);
            editor.putString(lat + "," + lng, icon);
            editor.putString(lat + "," + lng + "i", info);

            i++;
        }
        console.close();
        editor.putBoolean("firstRun", false);
        editor.putInt("locationCount", i);
        editor.commit();
    }

    /**
     *
     *   getDouble() and putDouble()
     *   used for ensuring precision in sharedpreferences for location coordines.
     *
     */
    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }
    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }



    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    private void setUpMap() {
        if (!markersOnMap) {
            addMarkers();
            markersOnMap = true;
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(location == null){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
        }
        else{
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }


    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }
}
