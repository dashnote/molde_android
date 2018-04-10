package com.limefriends.molde.menu_map;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.limefriends.molde.R;
import com.limefriends.molde.MoldeMainActivity;
import com.limefriends.molde.menu_map.entity.MoldeSearchMapHistoryEntity;
import com.limefriends.molde.menu_map.entity.MoldeSearchMapInfoEntity;
import com.limefriends.molde.menu_map.reportCard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MoldeMapFragment extends Fragment
        implements OnMapReadyCallback,
        MoldeMainActivity.onKeyBackPressedListener,
        MoldeMapReportPagerAdapterCallback {
    //맵 구성
    @BindView(R.id.map_ui)
    RelativeLayout map_ui;
    @BindView(R.id.map_view_layout)
    LinearLayout map_view_layout;

    @BindView(R.id.loc_search_bar)
    LinearLayout search_bar;
    @BindView(R.id.loc_search_input)
    TextView loc_search_input;

    //하단에 구성한 옵션
    @BindView(R.id.map_option_layout)
    RelativeLayout map_option_layout;
    @BindView(R.id.my_loc_button)
    ImageButton my_loc_button;
    @BindView(R.id.favorite_button)
    ImageButton favorite_button;
    @BindView(R.id.report_button)
    ImageButton report_button;

    //만약 로딩 중이거나 권한이 없을 때
    @BindView(R.id.map_view_progress)
    RelativeLayout map_view_progress;
    @BindView(R.id.progress_loading)
    ProgressBar progress_loading;
    @BindView(R.id.request_gps_button)
    Button request_gps_button;

    //신고 리스트 보여주는 리스트
    @BindView(R.id.report_card_layout)
    FrameLayout report_card_layout;
    @BindView(R.id.report_card_view)
    ViewPager report_card_view_pager;

    SupportMapFragment mapView;
    private static GoogleMap mMap;
    private String lat = "37.499597";
    private String lng = "127.031372";
    private String searchName = "";
    private String telNo = "";
    private int moveCnt = 0;
    private int animateUpCnt = 0;
    private boolean backChk = false;
    private boolean initChk = false;
    private boolean myLocChange = false;
    private LocationManager manager;
    private MyLocationListener myLocationListener;
    private boolean gpsEnable = false;
    private long gpsRequestTime = 0;
    private Marker myMarker;
    private ArrayList<Marker> reportInfohMarkers;
    private final int REQUEST_LOCATION = 1;
    private ArrayList<ReportCardItem> reportCardItemList;
    private ReportCardPagerAdapter reportCardAdapter;
    private ShadowTransformer reportCardShadowTransformer;

    public ArrayList<Long> beforeCallApplyMethodTimeList;
    private ArrayList<Integer> reportCardPositionList;
    private boolean firstPresentReportCard = false;

    public static MoldeMapFragment newInstance() {
        return new MoldeMapFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_fragment, container, false);
        ButterKnife.bind(this, view);
        mapView = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        mapView.getMapAsync(this);

        if (moveCnt == 0) {
            getMyLocation();
        }

        //뷰페이저어댑터 생성 및 콜백 지정
        reportInfohMarkers = new ArrayList<Marker>();
        reportCardItemList = new ArrayList<ReportCardItem>();
        reportCardAdapter = new ReportCardPagerAdapter(getContext());
        reportCardAdapter.setCallback(this);

        search_bar.setElevation(12);
        search_bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "검색 창으로 이동", Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.setClass(getContext(), MoldeSearchMapInfoActivity.class);
                intent.putExtra("searchName", searchName);
                startActivity(intent);
            }
        });

        map_ui.bringToFront();
        report_card_layout.setVisibility(View.INVISIBLE);

        my_loc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "위치 가져오기 기능", Toast.LENGTH_LONG).show();
                loc_search_input.setText(R.string.search);
                searchName = "검색하기";
                myLocChange = true;
                getMyLocation();
            }
        });
        favorite_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "즐겨찾기 페이지로 넘어가기", Toast.LENGTH_LONG).show();
            }
        });
        report_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "신고 페이지로 넘어가기", Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.setClass(getContext(), MoldeReportActivity.class);
                startActivity(intent);
            }
        });

        request_gps_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_LOCATION);
                    return;
                }
            }
        });

        initChk = true;
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        report_card_layout.setVisibility(View.INVISIBLE);
        if (getActivity() != null && getActivity() instanceof MoldeMainActivity) {
            MoldeSearchMapInfoEntity entity = ((MoldeMainActivity) getActivity()).getMapInfoResultData();
            MoldeSearchMapHistoryEntity historyEntity = ((MoldeMainActivity) getActivity()).getMapHistoryResultData();
            if (entity != null) {
                map_view_progress.setVisibility(View.INVISIBLE);
                //Toast.makeText(getContext(), entity.toString(), Toast.LENGTH_LONG).show();
                lat = entity.getMapLat();
                lng = entity.getMapLng();
                searchName = entity.getName();
                telNo = entity.getTelNo();
                loc_search_input.setText(searchName);
                report_card_layout.bringToFront();
                report_card_layout.setVisibility(View.VISIBLE);
                map_option_layout.setVisibility(View.INVISIBLE);
                initChk = false;
                if (MoldeSearchMapInfoActivity.checkBackPressed == false && backChk == false) {
                    //이곳에 좌표를 기준으로 검색 메서드를 추가해야함
                    moveCnt++;
                }
            } else if (entity == null && historyEntity != null) {
                map_view_progress.setVisibility(View.INVISIBLE);
                //Toast.makeText(getContext(), historyEntity.toString(), Toast.LENGTH_LONG).show();
                lat = historyEntity.getMapLat();
                lng = historyEntity.getMapLng();
                searchName = historyEntity.getName();
                telNo = historyEntity.getTelNo();
                loc_search_input.setText(searchName);
                report_card_layout.bringToFront();
                report_card_layout.setVisibility(View.VISIBLE);
                map_option_layout.setVisibility(View.INVISIBLE);
                initChk = false;
                if (MoldeSearchMapInfoActivity.checkBackPressed == false && backChk == false) {
                    //이곳에 좌표를 기준으로 검색 메서드를 추가해야함

                    moveCnt++;
                }

            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MoldeMainActivity) context).setOnKeyBackPressedListener(this);
        if (report_card_layout != null) {
            Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
            report_card_layout.startAnimation(trans_to_down);
            report_card_layout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng moveLoc = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(moveLoc, 17));

        if (searchName.equals("")) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            UiSettings uiSettings = mMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(false);
            uiSettings.setCompassEnabled(true);
            uiSettings.setMapToolbarEnabled(true);
            uiSettings.setMyLocationButtonEnabled(true);
        } else if (!searchName.equals("")) {
            if (searchName.charAt(searchName.length() - 1) == '동') {
                StringTokenizer placeInfo = new StringTokenizer(searchName, " ");
                String si = placeInfo.nextToken();
                String gu = placeInfo.nextToken();
                String dong = placeInfo.nextToken();
                if (gu.charAt(gu.length() - 1) == '구' && dong.charAt(dong.length() - 1) == '동') {
                    //주변 위치에 따라 검색해오기
                    Toast.makeText(getContext(), "시 구 동에 따라 안에 있는 정보들 보여주기", Toast.LENGTH_LONG).show();
                    /*Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
                    report_card_layout.startAnimation(trans_to_down);
                    report_card_layout.setVisibility(View.GONE);
                    report_card_layout.setClickable(false);
                    map_option_layout.setVisibility(View.VISIBLE);
                    backChk = true;*/
                    makeSearchMarkers(moveLoc);
                }
            } else {
                makeSearchMarker();
            }
        }


        /*********************** Map Click ***********************/
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (View.VISIBLE == report_card_layout.getVisibility()) {
                    Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
                    report_card_layout.startAnimation(trans_to_down);
                    report_card_layout.setVisibility(View.GONE);
                    report_card_layout.setClickable(false);
                    map_option_layout.setVisibility(View.VISIBLE);
                    backChk = true;
                }
            }
        });
        /*********************** Map long Click ***********************/
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                makeNewMarker(latLng);
            }
        });

        /*********************** Map detect Deselect Marker ***********************/
        mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {
                if (marker.getTitle().equals("내 위치") || marker.getTitle().equals("이름 없음")) {
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.my_location_icon));
                } else {
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon));
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                if (moveCnt > 0 && backChk == true) {
                    report_card_layout.setVisibility(View.VISIBLE);
                    map_option_layout.setVisibility(View.INVISIBLE);
                    Animation trans_to_up = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_up);
                    report_card_layout.startAnimation(trans_to_up);
                    report_card_layout.bringToFront();
                    backChk = false;
                }

                if (marker.getTitle().equals("내 위치")) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon(R.drawable.my_location_icon)));
                    if (report_card_layout.getVisibility() == View.VISIBLE) {
                        Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
                        report_card_layout.startAnimation(trans_to_down);
                        report_card_layout.setVisibility(View.GONE);
                        report_card_layout.setClickable(false);
                        map_option_layout.setVisibility(View.VISIBLE);
                        backChk = true;
                    }
                    return false;
                }

                if (marker.getTitle().equals("이름 없음")) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon(R.drawable.my_loc_button)));
                    if (report_card_layout.getVisibility() == View.VISIBLE) {
                        Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
                        report_card_layout.startAnimation(trans_to_down);
                        report_card_layout.setVisibility(View.GONE);
                        report_card_layout.setClickable(false);
                        map_option_layout.setVisibility(View.VISIBLE);
                        backChk = true;
                        Toast.makeText(getContext(), "정보 추가하는 창 띄우기", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "정보 추가하는 창 띄우기", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }

                if (reportInfohMarkers != null) {
                    for (int i = 0; i < reportInfohMarkers.size(); i++) {
                        if (marker.getTitle().equals(reportInfohMarkers.get(i).getTitle())) {
                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon(R.drawable.map_icon)));
                            report_card_view_pager.setCurrentItem(i, false);
                            return false;
                        }
                    }
                }
                return false;
            }
        });

    }

    private Bitmap resizeMapIcon(int imageId) {
        int resizeWidth = 120;
        Bitmap original = BitmapFactory.decodeResource(getResources(), imageId);
        double aspectRatio = (double) original.getHeight() / (double) original.getWidth();
        int targetHeight = (int) (resizeWidth * aspectRatio);
        Bitmap result = Bitmap.createScaledBitmap(original, resizeWidth, targetHeight, false);
        if (result != original) {
            original.recycle();
        }
        return result;
    }

    //GPS 권한체크 및 얻기
    public void onPermissionCheck(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    progress_loading.setVisibility(View.INVISIBLE);
                    request_gps_button.setVisibility(View.VISIBLE);
                    return;
                }
            }
            getMyLocation();
        } else {
            getMyLocation();
        }
    }

    public void getMyLocation() {
        manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        gpsEnable = false;
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsEnable = true;
        }
        if (gpsEnable) {
            final long minTime = 3000;
            final float minDistance = 100;
            if (myLocationListener == null) {
                myLocationListener = new MyLocationListener();
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_LOCATION);
                    return;
                }
            }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, myLocationListener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, myLocationListener);
            gpsRequestTime = System.currentTimeMillis();
        } else {
            Toast.makeText(getContext(), "GPS를 사용할 수 있도록 켜주셔야 사용이 가능합니다.", Toast.LENGTH_LONG).show();
            map_view_progress.setVisibility(View.INVISIBLE);
        }
    }

    public class MyLocationListener implements LocationListener {
        //위치정보 보여주기
        //구글 맵 이동
        int locChangeCount = 0;

        @Override
        public void onLocationChanged(Location location) {
            if (searchName.equals("") || myLocChange == true) {
                /*if(System.currentTimeMillis() - gpsRequestTime > 3000){
                    Toast.makeText(getContext(), "건물 안에서는 더 오랜 시간이 걸립니다", Toast.LENGTH_SHORT).show();
                }*/
                if (locChangeCount == 0) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng myLocation = new LatLng(latitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17));
                    if (myMarker != null) {
                        myMarker.remove();
                    }
                    myMarker = mMap.addMarker(
                            new MarkerOptions()
                                    .position(myLocation)
                                    .title("내 위치")
                                    //.snippet("내 정보")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_location_icon)
                                    )
                    );
                    locChangeCount++;
                    myLocChange = false;
                    if (moveCnt > 0) {
                        if (reportInfohMarkers.size() > 0 || reportCardItemList.size() > 0) {
                            for (Marker marker : reportInfohMarkers) {
                                marker.remove();
                            }
                            reportInfohMarkers.clear();
                            reportCardItemList.clear();
                            reportCardAdapter.removeAllCardItem();
                            report_card_view_pager.setAdapter(reportCardAdapter);
                        }
                        beforeCallApplyMethodTimeList = new ArrayList<Long>();
                        reportCardPositionList = new ArrayList<Integer>();
                        makeRandomMarkers(myLocation);

                        report_card_layout.setVisibility(View.VISIBLE);
                        map_option_layout.setVisibility(View.INVISIBLE);
                        Animation trans_to_up = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_up);
                        report_card_layout.startAnimation(trans_to_up);
                        report_card_layout.bringToFront();
                        report_card_layout.setClickable(true);
                        backChk = false;
                        initChk = false;

                        /*Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
                        report_card_layout.startAnimation(trans_to_down);
                        report_card_layout.setVisibility(View.GONE);
                        report_card_layout.setClickable(false);
                        map_option_layout.setVisibility(View.VISIBLE);
                        backChk = true;
                        initChk = true;
                        myMarker.showInfoWindow();
                        myMarker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon(R.drawable.my_location_icon)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));*/
                    }
                    moveCnt++;
                }
            }
            if(manager != null && myLocationListener != null){
                manager.removeUpdates(myLocationListener);
                myLocationListener = null;
                map_view_progress.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

    }

    //빈 정보의 마커 생성 및 후에 정보 추가 및 즐겨찾기
    public void makeNewMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("이름 없음");
        //markerOptions.snippet("정보 없음");
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_location_icon));
        Marker newMarker = mMap.addMarker(markerOptions);
    }

    //검색 후 하나의 마커 생성 및 검색정보 대입
    public void makeSearchMarker() {
        MarkerOptions markerOptions = new MarkerOptions();
        LatLng myLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        markerOptions.position(myLocation);
        markerOptions.title(searchName);
        //markerOptions.snippet(telNo);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon));
        Marker newMarker = mMap.addMarker(markerOptions);
        newMarker.showInfoWindow();
    }

    //검색 후 주변의 여러 신고장소 정보 신고 카드뷰에 추가 및 마커 추가
    public void makeSearchMarkers(LatLng moveLoc) {
        beforeCallApplyMethodTimeList = new ArrayList<Long>();
        reportCardPositionList = new ArrayList<Integer>();
        makeRandomMarkers(moveLoc);
    }

    @Override
    public void applyReportCardInfo(int position) {
        if (reportInfohMarkers != null) {
            if (initChk == false && backChk == false) {
                if (firstPresentReportCard == true) {
                    if (position == 1) {
                        Marker marker = reportInfohMarkers.get(0);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15));
                        marker.showInfoWindow();
                        firstPresentReportCard = false;
                        return;
                    }
                }
            }

            beforeCallApplyMethodTimeList.add(System.currentTimeMillis());
            reportCardPositionList.add(position);
            if (beforeCallApplyMethodTimeList.size() == 2) {
                if (beforeCallApplyMethodTimeList.get(1) - beforeCallApplyMethodTimeList.get(0) < 0.05) {
                    beforeCallApplyMethodTimeList.remove(1);
                    return;
                } else {
                    int currPosition = reportCardPositionList.get(0);
                    Marker marker = reportInfohMarkers.get(currPosition);
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon(R.drawable.map_icon)));
                    Log.e("p", currPosition + "");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15));
                    marker.showInfoWindow();
                    beforeCallApplyMethodTimeList.clear();
                    reportCardPositionList.clear();
                }
            }

        }
    }

    //랜덤으로 마커 정보 10개 추가 및 카드뷰 추가 - 테스트용
    public void makeRandomMarkers(LatLng moveLoc) {
        double start = -0.000000001;
        double end = 0.000000001;
        double rng = (end - start) + 0.01;
        Random randomGenerator = new Random();
        for (int i = 0; i < 10; i++) {
            double rndValLat = (randomGenerator.nextDouble() * rng) + start;
            double rndValLng = (randomGenerator.nextDouble() * rng) + start;
            LatLng latLng = new LatLng(moveLoc.latitude + rndValLat, moveLoc.longitude + rndValLng);

            Marker currMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(i + 1 + "번째")
                    //.snippet(latLng.latitude + ", " + latLng.longitude)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon))
            );
            reportInfohMarkers.add(currMarker);
            reportCardItemList.add(new ReportCardItem(currMarker.getTitle(), currMarker.getSnippet()));
            reportCardAdapter.addCardItem(new ReportCardItem(currMarker.getTitle(), currMarker.getSnippet()));
        }
        reportCardShadowTransformer = new ShadowTransformer(report_card_view_pager, reportCardAdapter);
        report_card_view_pager.setAdapter(reportCardAdapter);
        firstPresentReportCard = true;
        report_card_view_pager.setPageTransformer(false, reportCardShadowTransformer);
        report_card_view_pager.setOffscreenPageLimit(reportCardItemList.size());
        reportCardShadowTransformer.enableScaling(true);
    }

    @Override
    public void onBackKey() {
        if (backChk == false && initChk == false) {
            Animation trans_to_down = AnimationUtils.loadAnimation(getContext(), R.anim.trans_to_down);
            report_card_layout.startAnimation(trans_to_down);
            report_card_layout.setVisibility(View.GONE);
            report_card_layout.setClickable(false);
            map_option_layout.setVisibility(View.VISIBLE);
            backChk = true;
        }
    }

}
