package me.calebjones.spacelaunchnow.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.mrapp.android.preference.activity.PreferenceActivity;
import io.fabric.sdk.android.Fabric;
import jonathanfinerty.once.Once;
import me.calebjones.spacelaunchnow.BuildConfig;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.common.BaseActivity;
import me.calebjones.spacelaunchnow.common.customviews.generate.OnFeedbackListener;
import me.calebjones.spacelaunchnow.common.customviews.generate.Rate;
import me.calebjones.spacelaunchnow.content.database.ListPreferences;
import me.calebjones.spacelaunchnow.content.database.SwitchPreferences;
import me.calebjones.spacelaunchnow.content.events.FilterViewEvent;
import me.calebjones.spacelaunchnow.ui.changelog.ChangelogActivity;
import me.calebjones.spacelaunchnow.ui.intro.OnboardingActivity;
import me.calebjones.spacelaunchnow.ui.main.launches.LaunchesViewPager;
import me.calebjones.spacelaunchnow.ui.main.missions.MissionFragment;
import me.calebjones.spacelaunchnow.ui.main.next.NextLaunchFragment;
import me.calebjones.spacelaunchnow.ui.main.vehicles.VehiclesViewPager;
import me.calebjones.spacelaunchnow.ui.settings.SettingsActivity;
import me.calebjones.spacelaunchnow.ui.settings.fragments.AppearanceFragment;
import me.calebjones.spacelaunchnow.ui.supporter.SupporterActivity;
import me.calebjones.spacelaunchnow.ui.supporter.SupporterHelper;
import me.calebjones.spacelaunchnow.utils.Utils;
import me.calebjones.spacelaunchnow.utils.customtab.CustomTabActivityHelper;
import timber.log.Timber;

public class MainActivity extends BaseActivity {

    private static final String NAV_ITEM_ID = "navItemId";
    private static final int REQUEST_CODE_INTRO = 1837;
    private static ListPreferences listPreferences;
    private final Handler mDrawerActionHandler = new Handler();
    @BindView(R.id.adView)
    AdView adView;
    @BindView(R.id.container)
    RelativeLayout container;
    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;
    private LaunchesViewPager mlaunchesViewPager;
    private MissionFragment mMissionFragment;
    private NextLaunchFragment mUpcomingFragment;
    private VehiclesViewPager mVehicleViewPager;
    private Toolbar toolbar;
    private Drawer result = null;
    private SharedPreferences sharedPref;
    private SwitchPreferences switchPreferences;
    private CustomTabActivityHelper customTabActivityHelper;
    private Context context;
    private boolean adviewEnabled = false;
    private Rate rate;

    static final int SHOW_INTRO = 1;

    private int mNavItemId;

    public int statusColor;

    public void mayLaunchUrl(Uri parse) {
        if (customTabActivityHelper.mayLaunchUrl(parse, null, null)) {
            Timber.v("mayLaunchURL Accepted - %s", parse.toString());
        } else {
            Timber.v("mayLaunchURL Denied - %s", parse.toString());
        }
    }

    public MainActivity() {
        super("Main Activity");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        if (!Fabric.isInitialized()) {
            Fabric.with(this, new Crashlytics());
        }
        if (!Once.beenDone(Once.THIS_APP_INSTALL, "showTutorial")) {
            startActivityForResult(new Intent(this, OnboardingActivity.class), SHOW_INTRO);
        }

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();

        if ("me.calebjones.spacelaunchnow.NIGHTMODE".equals(action)) {
            Intent sendIntent = new Intent(this, SettingsActivity.class);
            sendIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    AppearanceFragment.class.getName());
            startActivity(sendIntent);
        }

        Timber.d("Creating Preference instances.");
        listPreferences = ListPreferences.getInstance(this.context);
        switchPreferences = SwitchPreferences.getInstance(this.context);

        int m_theme;
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        this.context = getApplicationContext();
        customTabActivityHelper = new CustomTabActivityHelper();

        Timber.d("Checking if night mode active.");
        if (listPreferences.isNightModeActive(this)) {
            Timber.d("Night mode is active.");
            switchPreferences.setNightModeStatus(true);
            statusColor = ContextCompat.getColor(context, R.color.darkPrimary_dark);
        } else {
            Timber.d("Night mode is not active.");
            switchPreferences.setNightModeStatus(false);
            statusColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        }
        m_theme = R.style.LightTheme_NoActionBar;

        Timber.d("Checking if theme changed.");
        if (getSharedPreferences("theme_changed", 0).getBoolean("recreate", false)) {
            SharedPreferences.Editor editor = getSharedPreferences("theme_changed", 0).edit();
            editor.putBoolean("recreate", false);
            editor.apply();
            recreate();
        }

        Timber.d("Setting theme.");
        setTheme(m_theme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Timber.d("Binding views.");
        ButterKnife.bind(this);

        Timber.d("Check if supporter.");
        if (!SupporterHelper.isSupporter()) {
            Timber.d("Loading ads.");
            adView.loadAd(new AdRequest.Builder().build());
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    Timber.d("Failed to load ads.");
                    adviewEnabled = false;
                    super.onAdFailedToLoad(i);
                }

                @Override
                public void onAdLoaded() {
                    Timber.d("Ad loaded successfully.");
                    adviewEnabled = true;
                    showAd();
                    super.onAdLoaded();
                }
            });
        } else {
            Timber.d("Hiding ads.");
            adviewEnabled = false;
            hideAd();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupWindowAnimations();
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // load saved navigation state if present
        if (null == savedInstanceState) {
            mNavItemId = R.id.menu_next_launch;
        } else {
            mNavItemId = savedInstanceState.getInt(NAV_ITEM_ID);
        }

        Timber.d("Building account header.");
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withCompactStyle(false)
                .withHeaderBackground(new ImageHolder("http://res.cloudinary.com/dnkkbfy3m/image/upload/v1462465326/navbar_one_sqfhes.png"))
                .withSavedInstance(savedInstanceState)
                .build();

        Timber.d("Building rate builder.");
        rate = new Rate.Builder(context)
                .setTriggerCount(10)
                .setMinimumInstallTime(TimeUnit.DAYS.toMillis(3))
                .setMessage(R.string.please_rate_short)
                .setFeedbackAction(new OnFeedbackListener() {
                    @Override
                    public void onFeedbackTapped() {
                        showFeedback();
                    }
                })
                .setSnackBarParent(coordinatorLayout)
                .build();


        Timber.d("Building DrawerBuilder");
        result = new DrawerBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withToolbar(toolbar)
                .withHasStableIds(true)
                .withAccountHeader(headerResult)
//                                )
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Home")
                                .withIcon(GoogleMaterial.Icon.gmd_home)
                                .withIdentifier(R.id.menu_next_launch)
                                .withSelectable(true),
                        new PrimaryDrawerItem().withName("Launches")
                                .withIcon(GoogleMaterial.Icon.gmd_assignment)
                                .withIdentifier(R.id.menu_launches)
                                .withSelectable(true),
                        new PrimaryDrawerItem().withName("Missions")
                                .withIcon(GoogleMaterial.Icon.gmd_satellite)
                                .withIdentifier(R.id.menu_missions)
                                .withSelectable(true),
                        new PrimaryDrawerItem().withName("Vehicles")
                                .withIcon(FontAwesome.Icon.faw_rocket)
                                .withIdentifier(R.id.menu_vehicle)
                                .withSelectable(true),
                        new PrimaryDrawerItem().withName("Settings")
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withIdentifier(R.id.menu_settings)
                                .withSelectable(true),
                        new DividerDrawerItem(),
                        new ExpandableDrawerItem().withName("Stay Connected").withIcon(CommunityMaterial.Icon.cmd_account).withDescription("Connect with the Community").withIdentifier(19).withSelectable(false).withSubItems(
                                new SecondaryDrawerItem()
                                        .withIcon(CommunityMaterial.Icon.cmd_discord)
                                        .withLevel(2)
                                        .withName("Discord")
                                        .withDescription("Join the Community")
                                        .withIdentifier(R.id.menu_discord)
                                        .withSelectable(false),
                                new SecondaryDrawerItem()
                                        .withIcon(CommunityMaterial.Icon.cmd_twitter)
                                        .withLevel(2)
                                        .withName("Twitter")
                                        .withDescription("Connect on Twitter")
                                        .withIdentifier(R.id.menu_twitter)
                                        .withSelectable(false),
                                new SecondaryDrawerItem()
                                        .withIcon(CommunityMaterial.Icon.cmd_facebook)
                                        .withLevel(2)
                                        .withName("Facebook")
                                        .withDescription("Follow on Facebook")
                                        .withIdentifier(R.id.menu_facebook)
                                        .withSelectable(false),
                                new SecondaryDrawerItem()
                                        .withIcon(CommunityMaterial.Icon.cmd_web)
                                        .withLevel(2)
                                        .withName("On the Web")
                                        .withDescription("Space Launch Now - Official Site")
                                        .withIdentifier(R.id.menu_website)
                                        .withSelectable(false)
                        ),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem()
                                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                                .withName("What's New?")
                                .withDescription("See the changelog.")
                                .withIdentifier(R.id.menu_new)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withIcon(GoogleMaterial.Icon.gmd_feedback)
                                .withName("Feedback")
                                .withDescription("Found a bug?")
                                .withIdentifier(R.id.menu_feedback)
                                .withSelectable(false)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            navigate((int) drawerItem.getIdentifier());
                        }
                        return false;
                    }
                }).build();

        Timber.d("If not Supporter add footer else thank the user!");
        if (!SupporterHelper.isSupporter()) {
            Timber.d("Adding footer.");
            result.addStickyFooterItem(
                    new PrimaryDrawerItem().withName("Become a Supporter")
                            .withDescription("Get Pro Features")
                            .withIcon(GoogleMaterial.Icon.gmd_mood)
                            .withIdentifier(R.id.menu_support)
                            .withSelectable(false));
        }

        if (SupporterHelper.isSupporter()) {
            Timber.d("Show thanks for support.");
            result.addStickyFooterItem(
                    new PrimaryDrawerItem().withName("Thank you for the support!")
                            .withIcon(GoogleMaterial.Icon.gmd_mood)
                            .withIdentifier(R.id.menu_support)
                            .withSelectable(false));
        }

        Timber.d("Navigate to initial fragment.");
        navigate(mNavItemId);
    }

    private void setupWindowAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide();
            slide.setDuration(1000);
            getWindow().setEnterTransition(slide);
            getWindow().setReturnTransition(slide);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.v("MainActivity onStart!");
        customTabActivityHelper.bindCustomTabsService(this);
        mayLaunchUrl(Uri.parse("https://launchlibrary.net/"));
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        Timber.v("MainActivity onStop!");
        customTabActivityHelper.unbindCustomTabsService(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void showWhatsNew() {
        Intent whatsNew = new Intent(this, ChangelogActivity.class);
        startActivity(whatsNew);
    }

    public void onResume() {
        super.onResume();
        Timber.v("onResume");
        if (rate != null) {
            rate.count();
        }
        if (BuildConfig.DEBUG) {
            showRemainingCount();
        }

        if (!Once.beenDone(Once.THIS_APP_VERSION, "showChangelog")) {
            if (!rate.isShown()) {
                Once.markDone("showChangelog");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showChangelogSnackbar();
                    }
                }, 1000);
            }
        }
        if (getSharedPreferences("theme_changed", 0).getBoolean("recreate", false)) {
            SharedPreferences.Editor editor = getSharedPreferences("theme_changed", 0).edit();
            editor.putBoolean("recreate", false);
            editor.apply();
            recreate();
        }

        if (listPreferences.isNightModeActive(this)) {
            switchPreferences.setNightModeStatus(true);
            statusColor = ContextCompat.getColor(context, R.color.darkPrimary_dark);
        } else {
            switchPreferences.setNightModeStatus(false);
            statusColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        }
    }

    private void showChangelogSnackbar() {
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "Updated to version " + Utils.getVersionName(context), Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setAction("Changelog", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showWhatsNew();
                    }
                });
        snackbar.show();
    }

    @SuppressLint("ShowToast")
    private synchronized void showRemainingCount() {
        int count = (int) rate.getRemainingCount();
        String message = String.format("%s more times until rate pop-up", count);
        Toast mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.setText(message);
        if (BuildConfig.DEBUG){
            mToast.show();
        }
    }

    public void setActionBarTitle(String title) {
        toolbar.setTitle(title);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getFragmentManager().getBackStackEntryCount() != 0) {
                getFragmentManager().popBackStack();
            } else {
                if (sharedPref.getBoolean("confirm_exit", false)) {
                    new MaterialDialog.Builder(this)
                            .title("Confirm Exit")
                            .negativeText("Cancel")
                            .positiveText("Exit")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    finish();
                                }
                            })
                            .show();
                } else {
                    super.onBackPressed();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (BuildConfig.DEBUG) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void navigate(final int itemId) {
        Timber.v("Navigate to %s", itemId);
        // perform the actual navigation logic, updating the main_menu content fragment etc
        FragmentManager fm = getSupportFragmentManager();
        switch (itemId) {
            case R.id.menu_next_launch:
                mNavItemId = R.id.menu_next_launch;
                // Check to see if we have retained the worker fragment.
                mUpcomingFragment = (NextLaunchFragment) fm.findFragmentByTag("NEXT_LAUNCH");

                // If not retained (or first time running), we need to create it.
                if (mUpcomingFragment == null) {
                    mUpcomingFragment = new NextLaunchFragment();
                    // Tell it who it is working with.
                    fm.beginTransaction().replace(R.id.flContent, mUpcomingFragment, "NEXT_LAUNCH").commit();
                }
                break;
            case R.id.menu_launches:
                mNavItemId = R.id.menu_launches;
                // Check to see if we have retained the worker fragment.
                mlaunchesViewPager = (LaunchesViewPager) fm.findFragmentByTag("LAUNCH_VIEWPAGER");

                // If not retained (or first time running), we need to create it.
                if (mlaunchesViewPager == null) {
                    mlaunchesViewPager = new LaunchesViewPager();
                    // Tell it who it is working with.
                    fm.beginTransaction().replace(R.id.flContent, mlaunchesViewPager, "LAUNCH_VIEWPAGER").commit();
                }
                if (rate != null) {
                    rate.showRequest();
                }

                break;
            case R.id.menu_missions:
                mNavItemId = R.id.menu_missions;
                setActionBarTitle("Missions");
                // Check to see if we have retained the worker fragment.
                mMissionFragment = (MissionFragment) fm.findFragmentByTag("MISSION_FRAGMENT");

                // If not retained (or first time running), we need to create it.
                if (mMissionFragment == null) {
                    mMissionFragment = new MissionFragment();
                    // Tell it who it is working with.
                    fm.beginTransaction().replace(R.id.flContent, mMissionFragment, "MISSION_FRAGMENT").commit();
                }
                if (rate != null) {
                    rate.showRequest();
                }

                break;
            case R.id.menu_vehicle:
                mNavItemId = R.id.menu_vehicle;
                setActionBarTitle("Vehicles");
                // Check to see if we have retained the worker fragment.
                mVehicleViewPager = (VehiclesViewPager) fm.findFragmentByTag("VEHICLE_VIEWPAGER");

                // If not retained (or first time running), we need to create it.
                if (mVehicleViewPager == null) {
                    mVehicleViewPager = new VehiclesViewPager();
                    // Tell it who it is working with.
                    fm.beginTransaction().replace(R.id.flContent, mVehicleViewPager, "VEHICLE_VIEWPAGER").commit();
                }
                if (rate != null) {
                    rate.showRequest();
                }

                break;
            case R.id.menu_launch:
                Utils.openCustomTab(this, getApplicationContext(), "https://launchlibrary.net/");
                break;
            case R.id.menu_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsIntent);
                break;
            case R.id.menu_new:
                showWhatsNew();
                break;
            case R.id.menu_support:
                Intent supportIntent = new Intent(this, SupporterActivity.class);
                startActivity(supportIntent);
                break;
            case R.id.menu_feedback:
                showFeedback();
                break;
            case R.id.menu_twitter:
                String url = "https://twitter.com/spacelaunchnow";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
            case R.id.menu_discord:
                String discordUrl = "https://discord.gg/WVfzEDW";
                Intent discordIntent = new Intent(Intent.ACTION_VIEW);
                discordIntent.setData(Uri.parse(discordUrl));
                startActivity(discordIntent);
                break;
            case R.id.menu_facebook:
                String facebookUrl = "https://www.facebook.com/spacelaunchnow/";
                Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
                facebookIntent.setData(Uri.parse(facebookUrl));
                startActivity(facebookIntent);
                break;
            case R.id.menu_website:
                String websiteUrl = "https://spacelaunchnow.me/";
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW);
                websiteIntent.setData(Uri.parse(websiteUrl));
                startActivity(websiteIntent);
                break;
            default:
                // ignore
                break;
        }
    }

    private void showFeedback() {
        new MaterialDialog.Builder(this)
                .title("Submit Feedback")
                .autoDismiss(true)
                .content("Feel free to submit bugs or feature requests for anything related to the app. If you found an issue with the launch data, the libraries at Launch Library that provide the data can be contacted via Discord or Reddit.")
                .neutralColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .negativeText("Launch Data")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String url = "https://www.reddit.com/r/LaunchLibrary/";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                })
                .positiveColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .positiveText("App Feedback")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.getBuilder()
                                .title("Need Support?")
                                .content("The fastest and most reliable way to get support is through Discord. If thats not an option feel free to email me directly.")
                                .neutralText("Email")
                                .negativeText("Cancel")
                                .positiveText("Discord")
                                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                                        intent.setData(Uri.parse("mailto:"));
                                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@calebjones.me"});
                                        intent.putExtra(Intent.EXTRA_SUBJECT, "Space Launch Now - Feedback");

                                        startActivity(Intent.createChooser(intent, "Email via..."));
                                    }
                                })
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                        String url = "https://discord.gg/WVfzEDW";
                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                        i.setData(Uri.parse(url));
                                        startActivity(i);
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                })
                .show();
    }

    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(NAV_ITEM_ID, mNavItemId);
    }

    private void hideAd() {
        adView.setVisibility(View.GONE);
    }

    private void showAd() {
        Timber.v("Showing Ad!");
        if (adviewEnabled && adView.getVisibility() == View.GONE) {
            adView.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FilterViewEvent event) {
        if (!SupporterHelper.isSupporter()) {
            if (event.isOpened) {
                hideAd();
            } else {
                showAd();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SHOW_INTRO) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Once.markDone("showTutorial");
                navigate(mNavItemId);
            }
        }
    }
}
