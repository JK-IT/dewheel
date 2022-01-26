package desoft.studio.dewheel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.SphericalUtil
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.cameraIdleEvents
import desoft.studio.dewheel.DataKenter.WedaKontrol
import desoft.studio.dewheel.Kluster.KiveEvent
import desoft.studio.dewheel.Kluster.KlusterRenderer
import desoft.studio.dewheel.kata.FireEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis


class LiveWheelActivity : AppCompatActivity(), OnMapReadyCallback {

    private val TAG = "-des- <<-++ LIVE WHEEL ACTIVITY ++->>";
    private val wekontrol : WedaKontrol by viewModels { WedaKontrol.DataWheelKontrolFactory((application as Wapplication).repo) }

    private val permlauncher = KF_PERM_LAUNCHER();
    private val locasetlauncher = KF_LOCA_SETT_LAUNCHER();
    private lateinit var fusedLocation : FusedLocationProviderClient;
    private var locaSetReqBuilder = LocationSettingsRequest.Builder();
    private var useRealocation : Location? = null;
    private var accurLocaquest = com.google.android.gms.location.LocationRequest.create().apply {
        priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;
        interval = 5000;
    }
    private var baLocaquest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        interval = 900000;
        fastestInterval = 60000;
    }
    private var tokeSrc = CancellationTokenSource();
    private lateinit var pKgeoder : Geocoder;
    private var kgmap : GoogleMap?=null;
    private lateinit var clusterManag : ClusterManager<KiveEvent>;
    private lateinit var clusRnder : ClusterRenderer<KiveEvent>;
    private var camIdleBool = false;
    private lateinit var setclient : SettingsClient ;
    private var currinsets : WindowInsetsCompat? =null;

    private  lateinit var uiHandler : Handler;

    private lateinit var uiLocationRationalGrp : LinearLayout;
    private lateinit var uiMapgrp : CoordinatorLayout;
    private lateinit var uiMapfrag : SupportMapFragment;

    private lateinit var uiGrantbtn : Button;
    private lateinit var uiOpenSetbtn: Button;
    private lateinit var uiCancelbtn : Button;
    /**
    *   *                     onCreate
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        pKgeoder = Geocoder(this);
        uiHandler = Handler(Looper.getMainLooper());
        /*disable drawing in cutout mode layout*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            /*window?.insetsController?.let {
                //it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
            }*/
            window.statusBarColor = Color.TRANSPARENT;
            WindowCompat.setDecorFitsSystemWindows(window, false);
        } else {
            @Suppress("DEPRECATION")
            window?.apply {
                clearFlags(/*WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or*/ WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                statusBarColor = Color.TRANSPARENT
                /*navigationBarColor= Color.TRANSPARENT*/
            }
        }
        Log.i(TAG, "onCreate: GETTING WINDOW INSETS");
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView){ _, insets ->
            //Log.d(TAG, "onCreate: Insets value ${insets.getInsets(WindowInsetsCompat.Type.systemBars())}");
            (findViewById<LinearLayout>(R.id.livevnt_map_grp)).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                //bottomMargin = insets.systemWindowInsetBottom
                bottomMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            }
            (findViewById<LinearLayout>(R.id.livevnt_location_content_perm_grp)).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            }
            currinsets = insets;
            //insets.consumeSystemWindowInsets();
            WindowInsetsCompat.CONSUMED;
        }
        setContentView(R.layout.activity_live_wheel);

        setclient = LocationServices.getSettingsClient(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        KF_SETUP_VIEW();
    }

    /**
    * *                     onStart
    */
    override fun onStart() {
        super.onStart();
    }
    /**
    * *                         onResume
    */
    override fun onResume() {
        super.onResume();
        Log.w(TAG, "onResume: ");
    }
    /**
    * *                         onPause
    */
    override fun onPause() {
        Log.w(TAG, "onPause: ");
        super.onPause();
    }
    /**
    * *                         onStop
    */
    override fun onStop() {
        Log.w(TAG, "onStop: ");
        super.onStop();
    }
    /**
    * *                             onDestroy
    */
    override fun onDestroy() {
        Log.w(TAG, "onDestroy: ");
        super.onDestroy()
    }
    // + --------->>-------->>--------->>*** -->>----------->>>>

    /**
    * *                     KF_SETUP_VIEW
    */
    private fun KF_SETUP_VIEW()
    {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                uiMapfrag = supportFragmentManager.findFragmentById(R.id.goo_maps) as SupportMapFragment;
                Log.d(TAG, "KF_SETUP_VIEW: start getting kgmap");
                 var ellapse = measureTimeMillis {
                    kgmap = uiMapfrag?.awaitMap();
                }
                Log.d(TAG, "KF_SETUP_VIEW: Map is null ? ${kgmap == null} and ellapse $ellapse, current insets ${currinsets?.getInsets(WindowInsetsCompat.Type.systemBars())}");
                kgmap?.let {
                    permlauncher.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION));
                    it.setPadding(0, currinsets?.getInsets(WindowInsetsCompat.Type.systemBars())?.top?: 144, 0, currinsets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom?: 144);
                    it.setOnMyLocationButtonClickListener {
                        permlauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION , android.Manifest.permission.ACCESS_COARSE_LOCATION));
                        false;
                    }
                }
            }
        }

        uiMapgrp = findViewById(R.id.livevnt_map_grp);
        uiLocationRationalGrp = findViewById(R.id.livevnt_location_perm_grp);
        //uiLocationRationalGrp.visibility = View.GONE;
        

        uiGrantbtn = findViewById(R.id.livevnt_grant_btn);
        uiGrantbtn.setOnClickListener {
            permlauncher.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION));
        }
        uiOpenSetbtn = findViewById(R.id.livevnt_opensettings_btn);
        uiOpenSetbtn.setOnClickListener {
            var inte = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            var uii = Uri.fromParts("package", packageName, null);
            inte.setData(uii);
            startActivity(inte);
        }
        uiCancelbtn = findViewById(R.id.livevnt_cancel_btn);
        uiCancelbtn.setOnClickListener {
            //KF_SHOW_LOCATION_RATIONALE(false);
            finish();
        }
    }

    /**
    * *                 KF_PERM_LAUNCHER
    */
    @SuppressLint("MissingPermission")
    private fun KF_PERM_LAUNCHER() : ActivityResultLauncher<Array<String>>
    {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permit ->
            var granted = true;
            for(ele in permit.entries) {
                granted = granted && ele.value;
            }
            if(granted == true) {
                Log.i(TAG, "KF_PERM_LAUNCHER: kgmap is null ? ${kgmap == null}");
                clusterManag = ClusterManager(this, kgmap);
                clusRnder = KlusterRenderer<KiveEvent>(this, kgmap!!, clusterManag);
                clusterManag.renderer = clusRnder;
                KF_GET_FRESH_LOCATION();
            } else {
                Log.i(TAG, "KF_PERM_LAUNCHER:  permission denied");
                KF_SHOW_LOCATION_RATIONALE(true);
                if(shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==  false
                    || shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) == false) {
                    KF_SIMPLE_GPS_SETTINGS_DIALOG();
                }
            }
        }
    }

    /**
    * *                         KF_LOCA_SETT_LAUNCHER
    */
    private fun KF_LOCA_SETT_LAUNCHER(): ActivityResultLauncher<IntentSenderRequest>
    {
        return registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
            when(it.resultCode) {
                Activity.RESULT_OK -> {
                    Toast.makeText(this, "Getting current location", Toast.LENGTH_SHORT).show();
                    KF_GET_FRESH_LOCATION();
                }
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "KF_LOCA_SETT_LAUNCHER: User canceled to enable location settings");
                }
            }
        }
    }
    /**
    * *                     KF_SHOW_LOCAION_RATIONALE
    */
    private fun KF_SHOW_LOCATION_RATIONALE(show: Boolean = false)
    {
        if(show) {
            uiLocationRationalGrp.visibility = View.VISIBLE;
        } else {
            uiLocationRationalGrp.visibility = View.GONE;
        }
    }

    /**
    * *                 KF_SIMPLE_GPS_SETTINGS_DIALOG
    */
    private fun KF_SIMPLE_GPS_SETTINGS_DIALOG()
    {
        var aledi = AlertDialog.Builder(this).setMessage("Please enable location services under settings. Would you like to go to settings?")
            .setPositiveButton("Settings"){ dia, _ ->
                var sete = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
                    var uii = Uri.fromParts("package", packageName, null);
                    it.setData(uii);
                }
                startActivity(sete);
            }
            .setNegativeButton("Cancel"){ dia , _ ->
                dia.dismiss();
            }
            .setCancelable(true).create();
        aledi.show();
    }
    /**
    * *                     onMapReady
    */
    @SuppressLint("MissingPermission")
    override fun onMapReady(gmap: GoogleMap) {
        kgmap = gmap;
        /*kgmap.isMyLocationEnabled = true;
        kgmap.uiSettings.apply {
            isCompassEnabled = true;
        }
        if(currinsets != null) {
            Log.i(TAG, "onMapReady: Current top inset ${currinsets!!.getInsets(WindowInsetsCompat.Type.statusBars())};")
            kgmap.setPadding(0,
                (currinsets!!.getInsets(WindowInsetsCompat.Type.systemBars()).top?: 0), 10,0);
        }
        else {
            kgmap.setPadding(0, 144, 10,0);
        }
        kgmap.setOnMyLocationButtonClickListener {
            permlauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION , android.Manifest.permission.ACCESS_COARSE_LOCATION));
            false;
        }
        //clusterManag = ClusterManager(this, kgmap);
        tempmanag = ClusterManager<TempItem>(this, kgmap);
        kgmap.setOnCameraIdleListener(tempmanag);
        kgmap.setOnMarkerClickListener(tempmanag);*/
    }

    /**
    * *                     KF_CENTER_TO_BOUND
     * . calculate the bound from latlng center and radius
    */
    private fun KF_CENTER_TO_BOUND(center: LatLng, radi: Double) : LatLngBounds
    {
        var discentcorner : Double = radi * Math.sqrt(2.0);
        var necorner = SphericalUtil.computeOffset(center, discentcorner, 45.0);
        var swcorner = SphericalUtil.computeOffset(center, discentcorner, 225.0);
        return LatLngBounds(swcorner, necorner);
    }

    /**
    * *                         KF_GET_FRESH_LOCATION
    */
    @SuppressLint("MissingPermission")
    private fun KF_GET_FRESH_LOCATION()
    {
        locaSetReqBuilder.addLocationRequest(accurLocaquest);
        setclient.checkLocationSettings(locaSetReqBuilder.build())
            .addOnSuccessListener {
                //. get fresh location
                KF_SHOW_LOCATION_RATIONALE(false);
                fusedLocation.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, tokeSrc.token)
                    .addOnSuccessListener {
                        Log.i(TAG, "KF_GET_FRESH_LOCATION: Current location ${it.longitude}, ${it.latitude}");
                        useRealocation = it;
                        kgmap?.let {
                            lifecycleScope.launch{
                                kgmap?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().apply {
                                    zoom(17f);
                                    target(LatLng(useRealocation?.latitude?: 0.0, useRealocation?.longitude?: 0.0));
                                }.build()));
                                if(kgmap?.isMyLocationEnabled == false) {
                                    kgmap?.isMyLocationEnabled = true;
                                }
                                /*var ciropt = CircleOptions().center(LatLng(useRealocation?.latitude?: 0.0, useRealocation?.longitude?: 0.0)).radius(200.0).strokeWidth(1f).strokeColor(R.color.quantum_bluegrey50)
                                kgmap.addCircle(ciropt);*/
                            }
                        }
                        if(camIdleBool == false ) {
                            camIdleBool = true;
                            lifecycleScope.launch{
                                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                    kgmap?.cameraIdleEvents()?.collect {
                                        var screegion = kgmap?.projection?.visibleRegion;
                                        Log.i(TAG, "Idle- center of screen on map ${screegion?.latLngBounds?.center}");

                                    }
                                }
                            }
                        }
                    }
            }.addOnFailureListener {
                KF_SHOW_LOCATION_RATIONALE(true);
                if(it is ResolvableApiException) {
                    locasetlauncher.launch(IntentSenderRequest.Builder(it.resolution).build());
                }
            }
    }

    // + OBSERVER CREATION --------->>-------->>--------->>*** -->>----------->>>>

    private val temprealWatcher : Observer<FireEvent> = object: Observer<FireEvent> {
        override fun onChanged(t: FireEvent?) {
            t?.let {
                var kievnt = KiveEvent(pKgeoder, t);
            }
        }

    }
    /*inner class TempItem(var tite : String, var snip : String, var pos: LatLng) : ClusterItem {
        override fun getPosition(): LatLng {
            return pos;
        }

        override fun getTitle(): String? {
            return tite.toString();
        }

        override fun getSnippet(): String? {
            return snip.toString();
        }

    }*/
}