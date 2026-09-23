package pro.sketchware.activities.main.activities;

import android.animation.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import ru.inoui.InoViewPager;
import pro.sketchware.R;

public class SetupActivity extends Activity {

    private static final String PREFS_NAME = "SetupPrefs";
    private static final String KEY_SETUP_COMPLETED = "is_completed";

    private LinearLayout background;
    private InoViewPager setuppages;
    private LinearLayout prevbtn;
    private LinearLayout nextbtn;
    private ImageView previcon;
    private ImageView nexticon;
    private TextView nextText;
    private ProgressBar loadingIndicator;

    private Switch swNotification;
    private Switch swStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cek apakah setup sudah pernah diselesaikan sebelumnya
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SETUP_COMPLETED, false)) {
            navigateToMain();
            return;
        }

        // 2. Membangun UI secara murni dari Java
        buildLayoutUI();
        
        // 3. Logika navigasi & listener
        initializeLogic();
    }

    private void buildLayoutUI() {
        // Parent Container (Full Screen Background)
        background = new LinearLayout(this);
        background.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        background.setBackgroundColor(Color.parseColor("#0F1513"));
        background.setOrientation(LinearLayout.VERTICAL);
        background.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        // ViewPager
        setuppages = new InoViewPager(this);
        LinearLayout.LayoutParams vpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        vpParams.bottomMargin = dpToPx(16);
        setuppages.setLayoutParams(vpParams);

        // --- PAGE 1: Welcome Page ---
        LinearLayout welcomePage = createBasePageLayout();
        
        // App Icon
        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.drawable.daygreen);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(100), dpToPx(100));
        iconParams.bottomMargin = dpToPx(24);
        welcomePage.addView(appIcon, iconParams);

        // App Name
        TextView appName = new TextView(this);
        appName.setText("DayGreen");
        appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        appName.setTypeface(null, Typeface.BOLD);
        appName.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams appNameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        appNameParams.bottomMargin = dpToPx(32);
        welcomePage.addView(appName, appNameParams);

        // Welcome Text
        TextView welcomeTitle = new TextView(this);
        welcomeTitle.setText("Welcome to Sketchware!");
        welcomeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        welcomeTitle.setTypeface(null, Typeface.BOLD);
        welcomeTitle.setTextColor(Color.WHITE);
        welcomeTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams welcomeTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        welcomeTitleParams.bottomMargin = dpToPx(12);
        welcomePage.addView(welcomeTitle, welcomeTitleParams);

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("A place to build and realize dreams");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subtitle.setTextColor(Color.parseColor("#A3F1D7"));
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.bottomMargin = dpToPx(48);
        welcomePage.addView(subtitle, subtitleParams);

        // Add spacer untuk push content ke atas
        View spacer = new View(this);
        welcomePage.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        setuppages.addView(welcomePage);

        // --- PAGE 2: Permission Setup Page ---
        LinearLayout permissionPage = createBasePageLayout();

        // App Icon
        ImageView permIcon = new ImageView(this);
        permIcon.setImageResource(R.drawable.daygreen);
        permIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams permIconParams = new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80));
        permIconParams.bottomMargin = dpToPx(16);
        permissionPage.addView(permIcon, permIconParams);

        // Permission Title
        TextView permTitle = new TextView(this);
        permTitle.setText("Permission Setup");
        permTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        permTitle.setTypeface(null, Typeface.BOLD);
        permTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams permTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        permTitleParams.bottomMargin = dpToPx(32);
        permissionPage.addView(permTitle, permTitleParams);

        // Permission Items Container
        LinearLayout permContainer = new LinearLayout(this);
        permContainer.setOrientation(LinearLayout.VERTICAL);
        permContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Storage Permission Card
        LinearLayout storageCard = createPermissionCard("📁", "Storage Access", "Allow access to your files");
        swStorage = (Switch) storageCard.getTag();
        permContainer.addView(storageCard);

        addSpace(permContainer, 16);

        // Notification Permission Card
        LinearLayout notifCard = createPermissionCard("💻", "Device Settings", "Allow device configuration access");
        swNotification = (Switch) notifCard.getTag();
        permContainer.addView(notifCard);

        permissionPage.addView(permContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Spacer
        View spacer2 = new View(this);
        permissionPage.addView(spacer2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        setuppages.addView(permissionPage);

        // --- PAGE 3: Finish Page ---
        LinearLayout finishPage = createBasePageLayout();

        ImageView finishIcon = new ImageView(this);
        finishIcon.setImageResource(R.drawable.daygreen);
        finishIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams finishIconParams = new LinearLayout.LayoutParams(dpToPx(100), dpToPx(100));
        finishIconParams.bottomMargin = dpToPx(24);
        finishPage.addView(finishIcon, finishIconParams);

        TextView finishTitle = new TextView(this);
        finishTitle.setText("All Set! ✨");
        finishTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        finishTitle.setTypeface(null, Typeface.BOLD);
        finishTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams finishTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        finishTitleParams.bottomMargin = dpToPx(16);
        finishPage.addView(finishTitle, finishTitleParams);

        TextView finishSubtitle = new TextView(this);
        finishSubtitle.setText("Ready to create amazing experiences");
        finishSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        finishSubtitle.setTextColor(Color.parseColor("#A3F1D7"));
        finishSubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams finishSubtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        finishSubtitleParams.bottomMargin = dpToPx(48);
        finishPage.addView(finishSubtitle, finishSubtitleParams);

        // Loading indicator (hidden by default)
        loadingIndicator = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        loadingIndicator.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            loadingIndicator.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#A3F1D7")));
        }
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
                dpToPx(48), dpToPx(48));
        loadingParams.gravity = Gravity.CENTER_HORIZONTAL;
        loadingParams.bottomMargin = dpToPx(32);
        loadingIndicator.setVisibility(View.GONE);
        finishPage.addView(loadingIndicator, loadingParams);

        // Add spacer untuk push content ke atas
        View spacer3 = new View(this);
        finishPage.addView(spacer3, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        setuppages.addView(finishPage);

        background.addView(setuppages);

        // --- Bottom Navigation Bar ---
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bottomNav.setGravity(Gravity.CENTER_VERTICAL);
        bottomNav.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));

        // Previous Button
        prevbtn = new LinearLayout(this);
        prevbtn.setOrientation(LinearLayout.VERTICAL);
        prevbtn.setGravity(Gravity.CENTER);
        prevbtn.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        previcon = new ImageView(this);
        previcon.setRotation(180);
        previcon.setImageResource(R.drawable.arrow_back_24px);
        previcon.setColorFilter(Color.parseColor("#889E97"));
        prevbtn.addView(previcon, new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        prevbtn.setVisibility(View.GONE);

        // Spacer
        LinearLayout spacer4 = new LinearLayout(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        spacer4.setLayoutParams(spacerParams);

        // Next / Done Button
        nextbtn = new LinearLayout(this);
        nextbtn.setOrientation(LinearLayout.HORIZONTAL);
        nextbtn.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12));
        nextbtn.setGravity(Gravity.CENTER);

        nexticon = new ImageView(this);
        nexticon.setRotation(180);
        nexticon.setImageResource(R.drawable.arrow_back_24px);
        nexticon.setColorFilter(Color.parseColor("#A3F1D7"));
        nextbtn.addView(nexticon, new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));

        nextText = new TextView(this);
        nextText.setText("Done");
        nextText.setTextColor(Color.parseColor("#A3F1D7"));
        nextText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nextText.setTypeface(null, Typeface.BOLD);
        nextText.setVisibility(View.GONE);
        LinearLayout.LayoutParams nextTextParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nextTextParams.leftMargin = dpToPx(8);
        nextbtn.addView(nextText, nextTextParams);

        bottomNav.addView(prevbtn);
        bottomNav.addView(spacer4);
        bottomNav.addView(nextbtn);

        background.addView(bottomNav);

        setContentView(background);
    }

    private void initializeLogic() {
        // Status & Navigation Bar Transparan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            background.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(dpToPx(8), insets.getSystemWindowInsetTop(), dpToPx(8), insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        // Styling Tombol Navigation dengan Material 3 Style
        GradientDrawable prevBg = new GradientDrawable();
        prevBg.setCornerRadius(dpToPx(12));
        prevBg.setColor(Color.parseColor("#1B2B28"));
        prevbtn.setBackground(prevBg);

        GradientDrawable nextBg = new GradientDrawable();
        nextBg.setCornerRadius(dpToPx(12));
        nextBg.setColor(Color.parseColor("#004D40"));
        nextbtn.setBackground(nextBg);

        // Animasi pada button - Scale and Ripple
        setupButtonAnimations(prevbtn);
        setupButtonAnimations(nextbtn);

        // Click Listener Tombol
        prevbtn.setOnClickListener(v -> {
            animateButtonPress(prevbtn);
            int current = setuppages.getCurrentItem();
            if (current > 0) {
                setuppages.setCurrentItem(current - 1, true);
            }
        });

        nextbtn.setOnClickListener(v -> {
            animateButtonPress(nextbtn);
            int current = setuppages.getCurrentItem();
            if (current < 2) {
                setuppages.setCurrentItem(current + 1, true);
            } else {
                // Tampilkan loading indicator
                showLoadingAndFinish();
            }
        });

        // Sync ViewPager Scroll
        setuppages.addOnPageChangeListener(new InoViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset) {}

            @Override
            public void onPageSelected(int position) {
                // Animasi tombol back
                if (position == 0) {
                    fadeOut(prevbtn);
                } else {
                    fadeIn(prevbtn);
                }

                // Switch antara Icon Panah dan Teks "Done"
                if (position == 2) {
                    fadeOut(nexticon);
                    fadeIn(nextText);
                } else {
                    fadeIn(nexticon);
                    fadeOut(nextText);
                }
            }
        });
    }

    private void showLoadingAndFinish() {
        loadingIndicator.setVisibility(View.VISIBLE);
        
        // Simulasi loading selama 2 detik
        background.postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply();
            navigateToMain();
        }, 2000);
    }

    private void animateButtonPress(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 0.95f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 0.95f, 1.0f);
        
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();
    }

    private void setupButtonAnimations(View button) {
    button.setOnHoverListener((v, event) -> {
        if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) {
            ObjectAnimator elevation = ObjectAnimator.ofFloat(v, "elevation", 0f, dpToPx(8));
            elevation.setDuration(150);
            elevation.start();
        } else if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
            ObjectAnimator elevation = ObjectAnimator.ofFloat(v, "elevation", v.getElevation(), 0f);
            elevation.setDuration(150);
            elevation.start();
        }
        return false;
    });
    }

    private void fadeOut(View view) {
        if (view.getVisibility() != View.GONE) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", view.getAlpha(), 0f);
            alpha.setDuration(200);
            alpha.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                }
            });
            alpha.start();
        }
    }

    private void fadeIn(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            alpha.setDuration(200);
            alpha.start();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent();
        intent.setClassName(this, "pro.sketchware.activities.main.activities.MainActivity");
        startActivity(intent);
        finish();
    }

    // --- HELPER METODE UI ---

    private LinearLayout createBasePageLayout() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        page.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16));
        return page;
    }

    private LinearLayout createPermissionCard(String emoji, String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Background dengan outline Material 3 style
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#141D1A"));
        drawable.setCornerRadius(dpToPx(12));
        drawable.setStroke(dpToPx(1), Color.parseColor("#2A3D37"));
        card.setBackground(drawable);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(cardParams);

        // Icon Container
        LinearLayout iconContainer = new LinearLayout(this);
        iconContainer.setOrientation(LinearLayout.VERTICAL);
        iconContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconContainerParams = new LinearLayout.LayoutParams(
                dpToPx(56), dpToPx(56));
        iconContainerParams.rightMargin = dpToPx(16);

        // Icon Background
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#1B2B28"));
        iconBg.setCornerRadius(dpToPx(12));
        iconContainer.setBackground(iconBg);

        TextView iconEmoji = new TextView(this);
        iconEmoji.setText(emoji);
        iconEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        iconContainer.addView(iconEmoji, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        card.addView(iconContainer, iconContainerParams);

        // Text Container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textContainer.setLayoutParams(textContainerParams);

        // Title
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(Color.WHITE);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textContainer.addView(titleText);

        // Description
        TextView descText = new TextView(this);
        descText.setText(description);
        descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        descText.setTextColor(Color.parseColor("#889E97"));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dpToPx(4);
        descText.setLayoutParams(descParams);
        textContainer.addView(descText, descParams);

        card.addView(textContainer, textContainerParams);

        // Switch Material 3 Style
        Switch switchView = new Switch(this);
        switchView.setTrackDrawable(createMaterial3SwitchTrack());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchView.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#A3F1D7")));
            switchView.setTrackTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A3D37")));
        }
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switchParams.leftMargin = dpToPx(12);
        card.addView(switchView, switchParams);

        // Store switch reference di tag
        card.setTag(switchView);

        return card;
    }

    private GradientDrawable createMaterial3SwitchTrack() {
        GradientDrawable track = new GradientDrawable();
        track.setCornerRadius(dpToPx(12));
        track.setColor(Color.parseColor("#2A3D37"));
        return track;
    }

    private void addSpace(LinearLayout parent, int dp) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(dp)));
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
