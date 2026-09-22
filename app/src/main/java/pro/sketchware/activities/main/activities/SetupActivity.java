package pro.sketchware.activities.main.activities;

import android.animation.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    private CheckBox cbNotification;
    private CheckBox cbStorage;

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
        
        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.drawable.daygreen);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        welcomePage.addView(appIcon, new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80)));

        TextView appName = new TextView(this);
        appName.setText("DayGreen");
        appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        appName.setTypeface(null, Typeface.BOLD);
        appName.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams appNameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        appNameParams.topMargin = dpToPx(8);
        appNameParams.bottomMargin = dpToPx(20);
        welcomePage.addView(appName, appNameParams);

        // Container Outline untuk Kartu Informasi
        LinearLayout cardWelcome = createOutlineCard();
        
        addCardSection(cardWelcome, "Welcome to DayGreen", "#E0E0E0", 18, true);
        addCardSection(cardWelcome, "Sketchware Pro Mod", "#B0B0B0", 12, false);
        addSpace(cardWelcome, 12);

        addCardSection(cardWelcome, "Build Android apps with freedom!", "#F5F5F5", 14, true);
        addCardSection(cardWelcome, "The community-driven continuation of Sketchware Pro.", "#A0A0A0", 13, false);
        addSpace(cardWelcome, 12);

        addCardSection(cardWelcome, "Key Features:", "#E0E0E0", 14, true);
        addCardSection(cardWelcome, "No-code builder - Visual drag-and-drop interface", "#CCCCCC", 13, false);
        addCardSection(cardWelcome, "Modern components - Advanced UI toolkit", "#CCCCCC", 13, false);
        addCardSection(cardWelcome, "100% Free - Forever open source", "#CCCCCC", 13, false);
        addCardSection(cardWelcome, "Community driven - Made by developers", "#CCCCCC", 13, false);
        addSpace(cardWelcome, 12);

        addCardSection(cardWelcome, "Start building your ideas now!", "#F5F5F5", 14, true);
        addSpace(cardWelcome, 8);
        addCardSection(cardWelcome, "Open Source • Community Maintained • Based on Sketchware Pro", "#888888", 11, false);

        ScrollView welcomeScroll = new ScrollView(this);
        welcomeScroll.addView(cardWelcome);
        welcomePage.addView(welcomeScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- PAGE 2: Permission Page ---
        LinearLayout permissionPage = createBasePageLayout();

        ImageView permIcon = new ImageView(this);
        permIcon.setImageResource(R.drawable.daygreen); // Ganti dengan R.drawable.ic_permission jika ada
        permIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        permissionPage.addView(permIcon, new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80)));

        TextView permTitle = new TextView(this);
        permTitle.setText("App Permissions");
        permTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        permTitle.setTypeface(null, Typeface.BOLD);
        permTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams permTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        permTitleParams.topMargin = dpToPx(8);
        permTitleParams.bottomMargin = dpToPx(24);
        permissionPage.addView(permTitle, permTitleParams);

        LinearLayout cardPerm = createOutlineCard();
        cbNotification = createPermissionItem("Notifications", cardPerm);
        addSpace(cardPerm, 12);
        cbStorage = createPermissionItem("Storage Access", cardPerm);

        permissionPage.addView(cardPerm, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- PAGE 3: Finish Page ---
        LinearLayout finishPage = createBasePageLayout();

        ImageView finishIcon = new ImageView(this);
        finishIcon.setImageResource(R.drawable.daygreen); // Ganti dengan R.drawable.ic_smile jika ada
        finishIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        finishPage.addView(finishIcon, new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80)));

        TextView finishTitle = new TextView(this);
        finishTitle.setText("All Set!");
        finishTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        finishTitle.setTypeface(null, Typeface.BOLD);
        finishTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams finishTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        finishTitleParams.topMargin = dpToPx(8);
        finishTitleParams.bottomMargin = dpToPx(24);
        finishPage.addView(finishTitle, finishTitleParams);

        LinearLayout cardFinish = createOutlineCard();
        addCardSection(cardFinish, "Setup Complete!", "#E0E0E0", 18, true);
        addSpace(cardFinish, 8);
        addCardSection(cardFinish, "Congratulations! You are ready to create and share your great experiences with DayGreen.", "#CCCCCC", 14, false);

        finishPage.addView(cardFinish, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Adding pages to ViewPager
        setuppages.addView(welcomePage);
        setuppages.addView(permissionPage);
        setuppages.addView(finishPage);
        background.addView(setuppages);

        // --- Bottom Navigation Bar ---
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Previous Button
        prevbtn = new LinearLayout(this);
        prevbtn.setOrientation(LinearLayout.VERTICAL);
        prevbtn.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));
        prevbtn.setGravity(Gravity.CENTER);
        
        previcon = new ImageView(this);
        previcon.setImageResource(R.drawable.arrow_back_24px);
        previcon.setColorFilter(Color.parseColor("#889E97"));
        prevbtn.addView(previcon, new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        prevbtn.setVisibility(View.GONE);

        // Spacer
        LinearLayout spacer = new LinearLayout(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);

        // Next / Done Button
        nextbtn = new LinearLayout(this);
        nextbtn.setOrientation(LinearLayout.HORIZONTAL);
        nextbtn.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));
        nextbtn.setGravity(Gravity.CENTER);

        nexticon = new ImageView(this);
        nexticon.setRotation((float)(180));
        nexticon.setImageResource(R.drawable.arrow_back_24px);
        nexticon.setColorFilter(Color.parseColor("#A3F1D7"));
        nextbtn.addView(nexticon, new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));

        nextText = new TextView(this);
        nextText.setText("Done");
        nextText.setTextColor(Color.parseColor("#A3F1D7"));
        nextText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nextText.setTypeface(null, Typeface.BOLD);
        nextText.setVisibility(View.GONE);
        nextbtn.addView(nextText);

        bottomNav.addView(prevbtn);
        bottomNav.addView(spacer, spacerParams);
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

        // Styling Tombol Navigation
        GradientDrawable prevBg = new GradientDrawable();
        prevBg.setCornerRadius(dpToPx(16));
        prevBg.setColor(Color.parseColor("#2A3D37"));
        prevbtn.setBackground(prevBg);

        GradientDrawable nextBg = new GradientDrawable();
        nextBg.setCornerRadius(dpToPx(16));
        nextBg.setColor(Color.parseColor("#005140"));
        nextbtn.setBackground(nextBg);

        // Click Listener Tombol
        prevbtn.setOnClickListener(v -> {
            int current = setuppages.getCurrentItem();
            if (current > 0) {
                setuppages.setCurrentItem(current - 1, true);
            }
        });

        nextbtn.setOnClickListener(v -> {
            int current = setuppages.getCurrentItem();
            if (current < 2) {
                setuppages.setCurrentItem(current + 1, true);
            } else {
                // Halaman Terakhir -> Simpan status & pindah Activity
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply();
                navigateToMain();
            }
        });

        // Sync ViewPager Scroll
        setuppages.addOnPageChangeListener(new InoViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset) {}

            @Override
            public void onPageSelected(int position) {
                // Tombol Back
                prevbtn.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

                // Switch antara Icon Panah dan Teks "Done"
                if (position == 2) {
                    nexticon.setVisibility(View.GONE);
                    nextText.setVisibility(View.VISIBLE);
                } else {
                    nexticon.setVisibility(View.VISIBLE);
                    nextText.setVisibility(View.GONE);
                }
            }
        });
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
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        return page;
    }

    private LinearLayout createOutlineCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Background Transparan dengan Stroke/Outline Border
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#141D1A"));
        drawable.setCornerRadius(dpToPx(12));
        drawable.setStroke(dpToPx(1), Color.parseColor("#2A3D37")); // Border outline
        card.setBackground(drawable);
        return card;
    }

    private CheckBox createPermissionItem(String title, LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#F5F5F5"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        CheckBox cb = new CheckBox(this);

        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(tv, tvParams);
        row.addView(cb);

        parent.addView(row);
        return cb;
    }

    private void addCardSection(LinearLayout card, String text, String colorHex, int spSize, boolean isBold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize);
        if (isBold) tv.setTypeface(null, Typeface.BOLD);
        card.addView(tv);
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
