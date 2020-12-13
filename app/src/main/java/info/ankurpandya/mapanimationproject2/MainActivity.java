package info.ankurpandya.mapanimationproject2;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.maps.model.JointType.ROUND;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static int STEPS = 100;
    private static int SPEED = 5000;
    private static int JUMP = 3;

    private View btnRefresh;
    private View btnSubmit;
    private GoogleMap mMap;
    private ToggleButton togglePickup;
    private ToggleButton toggleDrop;

    private LatLng origin = null;
    private List<LatLng> destinations = new ArrayList<>();
    private boolean pickOriginEnabled = false;
    private boolean pickDestinationEnabled = false;

    private List<Marker> markers = new ArrayList<>();
    private List<Marker> luggages = new ArrayList<>();
    private List<Polyline> greyPolyLines = new ArrayList<>();
    //private List<List<LatLng>> listLatLng = new ArrayList();

    private ValueAnimator animator = null;

    Animator.AnimatorListener polyLineAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            try {
                for (Marker luggage : luggages) {
                    luggage.setPosition(origin);
                }
                //animator.addListener(polyLineAnimationListener);
                animator.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSubmit = findViewById(R.id.btn_submit);
        btnRefresh = findViewById(R.id.btn_refresh);
        togglePickup = findViewById(R.id.toggle_pickup);
        toggleDrop = findViewById(R.id.toggle_drop);

        togglePickup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                pickOriginEnabled = checked;
                if (checked) {
                    toggleDrop.setChecked(false);
                }
            }
        });

        toggleDrop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                pickDestinationEnabled = checked;
                if (checked) {
                    togglePickup.setChecked(false);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnSubmit.setOnClickListener(view -> startMap());

        btnRefresh.setOnClickListener(view -> resetMap());
    }

    private void startMap() {
        //Intent intent = new Intent(this, MapsActivity.class);
        //startActivity(intent);
        if (origin == null) {
            showToast("Choose the pickup");
        } else if (destinations.isEmpty()) {
            showToast("Choose a destination atleast");
        } else {
            togglePickup.setChecked(false);
            toggleDrop.setChecked(false);
            startAnim();
        }
    }

    private void startAnim() {
        if (origin == null || destinations.isEmpty()) {
            return;
        }

        for (LatLng destination : destinations) {
            //Adding 10 lat longs between origin and destination
            List<LatLng> latLngs = new ArrayList<>();

            double xDiff = (destination.latitude - origin.latitude) / (STEPS * 1.0);
            double yDiff = (destination.longitude - origin.longitude) / (STEPS * 1.0);
            for (int i = 0; i < STEPS + 1; i++) {
                double xPos = origin.latitude + (xDiff * i);
                double yPos = origin.longitude + (yDiff * i);
                double position = Math.abs(yPos - origin.longitude) * 180.0 / Math.abs(destination.longitude - origin.longitude);
                double jump = JUMP * Math.sin(
                        Math.toRadians(position)
                );
                LatLng latLng = new LatLng(
                        xPos + jump,
                        yPos
                );
                latLngs.add(latLng);
            }


            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(
                    R.drawable.ic_img_luggage
            );
            luggages.add(
                    mMap.addMarker(new MarkerOptions().position(origin).title("-").icon(icon))
            );

//            PolylineOptions lineOptions = null;
//            lineOptions = new PolylineOptions();
//            lineOptions.width(10);
//            lineOptions.color(Color.BLACK);
//            //lineOptions.color(ContextCompat.getColor(this, R.color.black));
//            lineOptions.startCap(new SquareCap());
//            lineOptions.endCap(new SquareCap());
//            lineOptions.jointType(ROUND);
//            //Polyline polyline = mMap.addPolyline(lineOptions);
//            //polyline.setPoints(latLngs); //ToDo - Temprry
//            //blackPolyLine.add(polyline);
//            blackPolyLine.add(mMap.addPolyline(lineOptions));

            PolylineOptions greyOptions = new PolylineOptions();
            greyOptions.width(10);
            greyOptions.color(Color.GRAY);
            greyOptions.startCap(new SquareCap());
            greyOptions.endCap(new SquareCap());
            greyOptions.jointType(ROUND);
            //greyPolyLine.add(mMap.addPolyline(greyOptions));
            Polyline polyline = mMap.addPolyline(greyOptions);
            polyline.setPoints(latLngs);
            greyPolyLines.add(polyline);
        }
        animatePolyLine();
    }

    private void animatePolyLine() {
        if (origin == null || destinations.isEmpty()) {
            return;
        }

        safeCancelAnimation();

        animator = ValueAnimator.ofInt(0, STEPS);
        animator.setDuration(SPEED);
        //animator.setInterpolator(new LinearInterpolator());
        //animator.setInterpolator(new OvershootInterpolator());
        //animator.setInterpolator(new BounceInterpolator());
        //animator.setInterpolator(new AccelerateInterpolator());
        //animator.setInterpolator(new AccelerateDecelerateInterpolator());
        //animator.setInterpolator(new AnticipateInterpolator());
        //animator.setInterpolator(new CycleInterpolator());
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                if (animator == null) {
                    return;
                }
                for (int i = 0; i < destinations.size(); i++) {
                    LatLng destination = destinations.get(i);
                    Marker luggage = luggages.get(i);
                    int animatedValue = (int) animator.getAnimatedValue();

                    double xDiff = (destination.latitude - origin.latitude) / (STEPS * 1.0);
                    double yDiff = (destination.longitude - origin.longitude) / (STEPS * 1.0);

                    double xPos = origin.latitude + (xDiff * animatedValue);
                    double yPos = origin.longitude + (yDiff * animatedValue);
                    double position = Math.abs(yPos - origin.longitude) * 180.0 / Math.abs(destination.longitude - origin.longitude);
                    double jump = JUMP * Math.sin(
                            Math.toRadians(position)
                    );
                    LatLng latLng = new LatLng(
                            xPos + jump,
                            yPos
                    );
//                    LatLng latLng = new LatLng(
//                            origin.latitude + (xDiff * animatedValue),
//                            origin.longitude + (yDiff * animatedValue)
//                    );
                    luggage.setPosition(latLng);
                }
            }
        });
        animator.addListener(polyLineAnimationListener);
        animator.start();
    }

    private void safeCancelAnimation() {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
            animator = null;
        }
    }

    private void resetMap() {
        safeCancelAnimation();
        origin = null;
        destinations.clear();
        markers.clear();
        togglePickup.setChecked(false);
        toggleDrop.setChecked(false);
        greyPolyLines.clear();
        luggages.clear();
        if (mMap != null) {
            mMap.clear();
            centerDefault();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.google_map_style));
            if (!success) {
                //Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            //Log.e(TAG, "Can't find style. Error: ", e);
        }

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        //centerMapTo(new LatLng(23.547883554721288, 78.94038893049311), 15);
        centerDefault();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (pickOriginEnabled) {
                    pickOrigin(latLng);
                } else if (pickDestinationEnabled) {
                    pickDesination(latLng);
                }
            }
        });

        //LatLng latLng = mMap.getCameraPosition().target;
    }

    private void centerDefault() {
        centerMapTo(new LatLng(23.547883554721288, 78.94038893049311), 5);
    }

    private void pickOrigin(LatLng latLng) {
        origin = latLng;
        mMap.addMarker(new MarkerOptions().position(latLng).title("Origin"));
        //pickOriginEnabled = false;
        togglePickup.setChecked(false);
    }

    private void pickDesination(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
        destinations.add(latLng);
    }

    private void centerMapTo(LatLng latLng, float zoomLevel) {
        if (mMap == null || latLng == null) {
            return;
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}