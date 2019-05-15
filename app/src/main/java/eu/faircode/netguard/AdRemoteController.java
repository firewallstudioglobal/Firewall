package eu.faircode.netguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class AdRemoteController {

//    TEST ADS
//    private static String AD_BANNER_GOOGLE = "ca-app-pub-3940256099942544/6300978111";
//    private static String AD_INTERSTITIAL_GOOGLE = "ca-app-pub-3940256099942544/8691691433";

    private static String AD_BANNER_GOOGLE = "ca-app-pub-8112267774166015/8774635848";
    private static String AD_INTERSTITIAL_GOOGLE = "ca-app-pub-8112267774166015/8759513341";

    private static String AD_BANNER_FACEBOOK = "622581381521756_622581964855031";
    private static String AD_INTERSTITIAL_FACEBOOK = "622581381521756_622582621521632";

    private static String FIREBASE_PATH_PREFIX = "/ADS/";


    public static class MyAd {
        public boolean isGoogle = true;
        public boolean isFacebook = false;

        public String banner = AD_BANNER_GOOGLE;
        public String interstitial = AD_INTERSTITIAL_GOOGLE;
        public String video;
    }

    public static interface MyAdCallback {
        public void onSuccess(MyAd myAd);
        public void onError(String error);
    }

    private static InterstitialAd googleInterstitialAd;
    private static com.facebook.ads.InterstitialAd facebookInterstitialAd;

    //--------------------------------------------------------------------//

    private static void readData(String path, final MyAdCallback callback){
        path = FIREBASE_PATH_PREFIX + path.replace(".", "_");

        Log.d("readData path:", path);

        FirebaseDatabase database = null;

        try {
            database = FirebaseDatabase.getInstance();
        } catch (Exception e) {

        }

        if(database == null){
            return;
        }

        DatabaseReference databaseReference = database.getReference(path);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("readData success:", dataSnapshot.toString());

                MyAd myAd = new MyAd();
                MyAd myAdFirebase = dataSnapshot.getValue(MyAd.class);

                if(myAdFirebase != null){
                    myAd = myAdFirebase;
                }

                callback.onSuccess(myAd);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("readData onCancelled:", databaseError.getDetails());
                callback.onError(databaseError.getDetails());
            }
        });
    }

    public static void showAdBannerFromFirebase(String path, final Activity activity, final ViewGroup adBannerContainer){
        readData(path, new MyAdCallback() {
            @Override
            public void onSuccess(MyAd myAd) {
                if(myAd.isGoogle){
                    showGoogleBannerAd(myAd.banner,  activity, adBannerContainer);
                } else {
                    showFacebookAdBanner(myAd.banner, activity, adBannerContainer);
                }
            }

            @Override
            public void onError(String error) {
                showGoogleBannerAd(AD_BANNER_GOOGLE, activity, adBannerContainer);
            }
        });
    }

    public static void showAdInterstitialFromFirebase(String path, final Activity activity){
        readData(path, new MyAdCallback() {
            @Override
            public void onSuccess(MyAd myAd) {
                if(myAd.isGoogle){
                    showGoogleIntertitialAd(myAd.interstitial, activity);
                } else {
                    showFacebookIntertitialAd(myAd.interstitial, activity);
                }
            }

            @Override
            public void onError(String error) {
                showGoogleIntertitialAd(AD_INTERSTITIAL_GOOGLE, activity);
            }
        });
    }

    public static void showAdInterstitialRepeatFromFirebase(String path, final Activity activity, final int seconds){
        readData(path, new MyAdCallback() {
            @Override
            public void onSuccess(MyAd myAd) {
                if(myAd.isGoogle){
                    showGoogleIntertitialAd(myAd.interstitial, activity);
                } else {
                    showFacebookIntertitialAdRepeat(myAd.interstitial, activity, seconds);
                }
            }

            @Override
            public void onError(String error) {
                showGoogleIntertitialAd(AD_INTERSTITIAL_GOOGLE, activity);
            }
        });
    }

    //--------------------------------------------------------------------//

    private static void initFacebookAd(Activity activity){
        com.facebook.ads.AudienceNetworkAds.initialize(activity);
        com.facebook.ads.AdSettings.addTestDevice("b7d0c7c6-d873-4af0-b7a7-4224d1284083");
    }

    public static void showFacebookAdBanner(String adId, final Activity activity, final ViewGroup adBannerContainer){
        initFacebookAd(activity);

        if(adId == null || adId.isEmpty()){
            adId = AD_BANNER_FACEBOOK;
        }

        com.facebook.ads.AdView adView = new com.facebook.ads.AdView(activity, adId, com.facebook.ads.AdSize.BANNER_HEIGHT_50);
        adBannerContainer.addView(adView);
        adView.loadAd();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        params.gravity = Gravity.CENTER;

        adView.setLayoutParams(params);

//        adBannerContainer.setVisibility(View.VISIBLE);

        adView.setAdListener(new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                adBannerContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAdLoaded(Ad ad) {
                adBannerContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdClicked(Ad ad) {

            }

            @Override
            public void onLoggingImpression(Ad ad) {

            }
        });
    }

    public static void showFacebookIntertitialAdRepeat(final String adId, final Activity activity, final int seconds) {
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d("FB ads", "showFacebookIntertitialAdRepeat");
                showFacebookIntertitialAd(adId, activity);
            }
        }, 0, seconds * 1000);
    }

    public static void showFacebookIntertitialAd(String adId, final Activity activity) {
        Log.e("Facebook_Ad", "showFacebookIntertitialAd.");

        initFacebookAd(activity);

        if(adId == null || adId.isEmpty()){
            adId = AD_INTERSTITIAL_FACEBOOK;
        }

        facebookInterstitialAd = new com.facebook.ads.InterstitialAd(activity, adId);
        // Set listeners for the Interstitial Ad

        facebookInterstitialAd.setAdListener(new com.facebook.ads.InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                // Interstitial ad displayed callback
                Log.e("Facebook_Ad", "Interstitial ad displayed.");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                // Interstitial dismissed callback
                Log.e("Facebook_Ad", "Interstitial ad dismissed.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Ad error callback
                Log.e("", "Interstitial ad failed to load: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Interstitial ad is loaded and ready to be displayed
                Log.d("Facebook_Ad", "Interstitial ad is loaded and ready to be displayed!");
                // Show the ad
//                facebookInterstitialAd.show();

                showDialog(activity, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if(facebookInterstitialAd.isAdLoaded()){
                            facebookInterstitialAd.show();
                        }
                        return null;
                    }
                });
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
                Log.d("Facebook_Ad", "Interstitial ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
                Log.d("Facebook_Ad", "Interstitial ad impression logged!");
            }
        });

        // For auto play video ads, it's recommended to load the ad
        // at least 30 seconds before it is shown
        facebookInterstitialAd.loadAd();
    }

    public static void showGoogleBannerAd(String adId, Activity activity, final ViewGroup adBannerContainer) {
        if(adId == null || adId.isEmpty()){
            adId = AD_BANNER_GOOGLE;
        }

        AdView adView = new AdView(activity);
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setAdUnitId(adId);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adBannerContainer.addView(adView);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        params.gravity = Gravity.CENTER;

        adView.setLayoutParams(params);

        adBannerContainer.setVisibility(View.INVISIBLE);

        adView.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                adBannerContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    public static void showGoogleIntertitialAd(String adId, final Activity activity) {
        if(adId == null || adId.isEmpty()){
            adId = AD_INTERSTITIAL_GOOGLE;
        }

        googleInterstitialAd = new InterstitialAd(activity);
        googleInterstitialAd.setAdUnitId(adId);
        googleInterstitialAd.loadAd(new AdRequest.Builder().build());

        googleInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                Log.d("showAd", "showGoogleIntertitialAd");
                showDialog(activity, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if(googleInterstitialAd.isLoaded()){
                            googleInterstitialAd.show();
                        }
                        return null;
                    }
                });
            }
        });
    }

    //--------------------------------------------------------------------//

    private static void showDialog(Activity activity, final Callable<Void> onOk){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("GET READY!")
                .setMessage("Time for a short sponsored session")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            onOk.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        try {
            builder.show();
        } catch (Exception e) {
            Log.d("showAdDialog error",e.getMessage());
        }
    }
}

