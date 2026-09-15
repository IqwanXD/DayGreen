package pro.sketchware.activities.main.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.adapters.ProjectsAdapter;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.library.ProjectComparator;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import a.a.a.DB;
import a.a.a.GB;
import a.a.a.lC;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FilesTools;
import extensions.anbui.daydream.setup.DRSetup;
import mod.hey.studios.project.ProjectTracker;
import mod.hey.studios.project.backup.BackupFactory;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.tyron.backup.SingleCopyTask;
import pro.sketchware.R;
import pro.sketchware.activities.about.AboutActivity;
import pro.sketchware.lib.base.BasePermissionAppCompatActivity;
import pro.sketchware.lib.base.BottomSheetDialogView;
import pro.sketchware.utility.DataResetter;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;
import ru.iqwanoino.SwipeRefreshLayout; // Import SwipeRefreshLayout kustom Anda

public class MainActivity extends BasePermissionAppCompatActivity {

    // Komponen UI dari XML Kustom Anda
    private DrawerLayout drawerLayout;
    private RecyclerView listProject;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView drawerToggleBtn, searchInputBtn, clearInputBtn;
    private EditText inputSearch;
    
    private LinearLayout fabMenu, fabCreate, fabRestore;
    private boolean isFabMenuOpen = false;
    private boolean isSearchOpen = false;

    // Logika Proyek
    private DB u;
    private DB projectPreference;
    private Snackbar storageAccessDenied;
    private BackupRestoreManager backupRestoreManager;
    public static boolean needRefreshProjectList = false;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<HashMap<String, Object>> projectsList = new ArrayList<>();
    private ProjectsAdapter projectsAdapter;

    // Launcher untuk membuka pengaturan proyek
    public final ActivityResultLauncher<Intent> openProjectSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                String sc_id = data.getStringExtra("sc_id");
                if (data.getBooleanExtra("is_new", false)) {
                    toDesignActivity(sc_id);
                    addProject(sc_id);
                } else {
                    updateProject(sc_id);
                }
            }
        }
    });

    private final OnBackPressedCallback closeDrawer = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isFabMenuOpen) {
                closeFabMenu();
                return;
            }
            if (isSearchOpen) {
                closeSearchBar();
                return;
            }
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return;
            }
            setEnabled(false);
            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeNoContrast();
        
        // Gunakan layout XML kustom Anda (pastikan namanya sesuai, misal: activity_main)
        setContentView(R.layout.activity_main);

        u = new DB(getApplicationContext(), "U1");
        projectPreference = new DB(getApplicationContext(), "project");

        initUI();
        setupListeners();
        setupRecyclerView();
        setupSearchAndRefresh();

        // Setup awal permission
        boolean hasStorageAccess = isStoragePermissionGranted();
        if (!hasStorageAccess) {
            showNoticeNeedStorageAccess();
        } else {
            allFilesAccessCheck();
        }
        
        handleIntentData();

        // Backup Manager tidak lagi membutuhkan projectsFragment (gunakan 'null' atau sesuaikan konstruktor Anda)
        backupRestoreManager = new BackupRestoreManager(this, null); 
        Configs.mainActivity = this;
        DRSetup.startNow(this);
        
        getOnBackPressedDispatcher().addCallback(this, closeDrawer);
        
        // Memuat daftar proyek
        refreshProjectsList();
    }

    private void initUI() {
        drawerLayout = findViewById(R.id.drawer_layout);
        listProject = findViewById(R.id.list_project);
        swipeRefreshLayout = findViewById(R.id.swiperefresh_layout);
        
        drawerToggleBtn = findViewById(R.id.drawer_togglebtn);
        searchInputBtn = findViewById(R.id.search_inputbtn);
        clearInputBtn = findViewById(R.id.clear_inputbtn);
        inputSearch = findViewById(R.id.inputsearch);
        
        fabMenu = findViewById(R.id.fab_menu);
        fabCreate = findViewById(R.id.fab_create);
        fabRestore = findViewById(R.id.fab_restore);
        
        // Sesuaikan padding UI agar tidak tertutup System UI (Status Bar)
        LinearLayout mainScreen = findViewById(R.id.mainscreen);
        UI.addSystemWindowInsetToPadding(mainScreen, true, false, true, true);
    }

    private void setupListeners() {
        // Toggle Nav Drawer
        drawerToggleBtn.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // FAB Click Listeners
        fabMenu.setOnClickListener(v -> toggleFabMenu());
        fabCreate.setOnClickListener(v -> {
            closeFabMenu();
            toProjectSettingsActivity();
        });
        fabRestore.setOnClickListener(v -> {
            closeFabMenu();
            if (backupRestoreManager != null) {
                backupRestoreManager.restore();
            }
        });

        // Menutup FAB ketika area layar disentuh
        drawerLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isFabMenuOpen) closeFabMenu();
                clearSearchFocusAndHideKeyboard();
            }
            return false;
        });

        listProject.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if ((Math.abs(dx) > 0 || Math.abs(dy) > 0) && isFabMenuOpen) {
                    closeFabMenu();
                }
            }
        });
    }

    private void setupRecyclerView() {
        listProject.setLayoutManager(new LinearLayoutManager(this));
        // Jika perlu adapter kustom Anda, gunakan konstruktor baru (karena fragment dihapus)
        // Jika ProjectsAdapter butuh Activity (bukan Fragment), ganti 'this' (yang sebelumnya mengarah ke Fragment)
        projectsAdapter = new ProjectsAdapter(this, projectsList); 
        listProject.setAdapter(projectsAdapter);
        listProject.setHasFixedSize(true);
    }

    private void setupSearchAndRefresh() {
        // SwipeRefreshLayout Buatan Anda
        swipeRefreshLayout.setOnRefreshListener(this::refreshProjectsList);

        // Search Bar Logic
        searchInputBtn.setOnClickListener(v -> {
            isSearchOpen = true;
            inputSearch.setVisibility(View.VISIBLE);
            clearInputBtn.setVisibility(View.VISIBLE);
            inputSearch.requestFocus();
            showKeyboard(inputSearch);
        });

        clearInputBtn.setOnClickListener(v -> {
            inputSearch.setText("");
            closeSearchBar();
        });

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (projectsAdapter != null) {
                    projectsAdapter.filterData(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void closeSearchBar() {
        isSearchOpen = false;
        inputSearch.setText("");
        inputSearch.setVisibility(View.GONE);
        clearInputBtn.setVisibility(View.GONE);
        clearSearchFocusAndHideKeyboard();
        
        if (projectsAdapter != null) {
            projectsAdapter.filterData("");
        }
    }

    // ==========================================
    // MANAJEMEN PROYEK (Diadaptasi dari ProjectsFragment)
    // ==========================================
    public void refreshProjectsList() {
        if (!isStoragePermissionGranted()) {
            if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
            s(); // Tampilkan pesan storage denied
            return;
        }

        executorService.execute(() -> {
            List<HashMap<String, Object>> loadedProjects = lC.a();
            loadedProjects.sort(new ProjectComparator(
                    projectPreference.d("sortBy"), 
                    projectPreference.a("pinnedProject", "-1"))
            );

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProjectDiffCallback(projectsList, loadedProjects));

            runOnUiThread(() -> {
                if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
                projectsList.clear();
                projectsList.addAll(loadedProjects);
                diffResult.dispatchUpdatesTo(projectsAdapter);
                
                if (inputSearch != null && inputSearch.getVisibility() == View.VISIBLE) {
                    projectsAdapter.filterData(inputSearch.getText().toString());
                }
            });
        });
    }

    private void addProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> newProject = lC.b(sc_id);
            if (newProject != null) {
                runOnUiThread(() -> {
                    projectsList.add(0, newProject);
                    projectsAdapter.notifyDataSetChanged();
                    listProject.scrollToPosition(0);
                });
            }
        });
    }

    private void updateProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> updatedProject = lC.b(sc_id);
            if (updatedProject != null) {
                int index = IntStream.range(0, projectsList.size())
                        .filter(i -> projectsList.get(i).get("sc_id").equals(sc_id))
                        .findFirst().orElse(-1);
                if (index != -1) {
                    projectsList.set(index, updatedProject);
                    runOnUiThread(() -> projectsAdapter.notifyDataSetChanged());
                }
            }
        });
    }

    public void toDesignActivity(String sc_id) {
        Intent intent = new Intent(this, DesignActivity.class);
        ProjectTracker.setScId(sc_id);
        intent.putExtra("sc_id", sc_id);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public void toProjectSettingsActivity() {
        Intent intent = new Intent(this, MyProjectSettingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openProjectSettings.launch(intent);
    }

    // ==========================================
    // UI HELPER & ANIMASI FAB
    // ==========================================
    private void toggleFabMenu() {
        if (!isFabMenuOpen) showFabMenu();
        else closeFabMenu();
    }

    private void showFabMenu() {
        isFabMenuOpen = true;
        fabCreate.setVisibility(View.VISIBLE);
        fabRestore.setVisibility(View.VISIBLE);

        fabCreate.animate().translationY(0f).alpha(1.0f).setDuration(150).start();
        fabRestore.animate().translationY(0f).alpha(1.0f).setDuration(150).start();
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        fabCreate.animate().translationY(16f).alpha(0.0f).setDuration(150)
                .withEndAction(() -> fabCreate.setVisibility(View.GONE)).start();
        fabRestore.animate().translationY(16f).alpha(0.0f).setDuration(150)
                .withEndAction(() -> fabRestore.setVisibility(View.GONE)).start();
    }

    public void clearSearchFocusAndHideKeyboard() {
        if (inputSearch != null && inputSearch.hasFocus()) {
            inputSearch.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
            }
        }
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ==========================================
    // LIFECYCLE & PERMISSIONS
    // ==========================================
    @Override
    public void onResume() {
        super.onResume();
        long freeMegabytes = GB.c();
        if (freeMegabytes < 100 && freeMegabytes > 0) {
            showNoticeNotEnoughFreeStorageSpace();
        }
        if (isStoragePermissionGranted() && storageAccessDenied != null && storageAccessDenied.isShown()) {
            storageAccessDenied.dismiss();
        }
        
        if (needRefreshProjectList) {
            refreshProjectsList();
            needRefreshProjectList = false;
        }
    }

    @Override
    public void g(int i) {
        if (i == 9501) {
            allFilesAccessCheck();
            refreshProjectsList();
        }
    }

    @Override
    public void h(int i) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        startActivityForResult(intent, i);
    }

    @Override
    public void l() {}
    @Override
    public void m() {}

    private void allFilesAccessCheck() {
        if (Build.VERSION.SDK_INT > 29) {
            File optOutFile = new File(getFilesDir(), ".skip_all_files_access_notice");
            boolean granted = Environment.isExternalStorageManager();

            if (!optOutFile.exists() && !granted) {
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
                dialog.setIcon(R.drawable.ic_mtrl_warning);
                dialog.setTitle("Android 11 storage access");
                dialog.setMessage("Starting with Android 11, Sketchware Pro needs a new permission to avoid taking ages to build projects.");
                dialog.setPositiveButton(Helper.getResString(R.string.common_word_settings), (v, which) -> {
                    FileUtil.requestAllFilesAccessPermission(this);
                    v.dismiss();
                });
                dialog.setNegativeButton("Skip", null);
                dialog.setNeutralButton("Don't show anymore", (v, which) -> {
                    try {
                        optOutFile.createNewFile();
                    } catch (IOException e) {
                        Log.e("MainActivity", "Error: " + e.getMessage(), e);
                    }
                    v.dismiss();
                });
                dialog.show();
            }
        }
    }

    private void showNoticeNeedStorageAccess() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_message_permission_title_storage));
        dialog.setIcon(R.drawable.ic_mtrl_folder);
        dialog.setMessage(Helper.getResString(R.string.common_message_permission_need_load_project));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), (v, which) -> {
            v.dismiss();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 9501);
        });
        dialog.show();
    }

    private void showNoticeNotEnoughFreeStorageSpace() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_message_insufficient_storage_space_title));
        dialog.setIcon(R.drawable.disc_full_24px);
        dialog.setMessage(Helper.getResString(R.string.common_message_insufficient_storage_space));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    public void s() {
        if (storageAccessDenied == null || !storageAccessDenied.isShown()) {
            storageAccessDenied = Snackbar.make(findViewById(android.R.id.content), Helper.getResString(R.string.common_message_permission_denied), Snackbar.LENGTH_INDEFINITE);
            storageAccessDenied.setAction(Helper.getResString(R.string.common_word_settings), v -> {
                storageAccessDenied.dismiss();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 9501);
            });
            storageAccessDenied.setActionTextColor(Color.YELLOW);
            storageAccessDenied.show();
        }
    }

    private void handleIntentData() {
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            if (data != null) {
                new SingleCopyTask(this, new SingleCopyTask.CallBackTask() {
                    @Override public void onCopyPreExecute() {}
                    @Override public void onCopyProgressUpdate(int progress) {}
                    @Override public void onCopyPostExecute(@NonNull String path, boolean wasSuccessful, @NonNull String reason) {
                        if (wasSuccessful) {
                            BackupRestoreManager manager = new BackupRestoreManager(MainActivity.this, null);
                            if (BackupFactory.zipContainsFile(path, "local_libs")) {
                                new MaterialAlertDialogBuilder(MainActivity.this)
                                        .setTitle("Warning")
                                        .setMessage(BackupRestoreManager.getRestoreIntegratedLocalLibrariesMessage(false, -1, -1, null))
                                        .setPositiveButton("Copy", (dialog, which) -> manager.doRestore(path, true))
                                        .setNegativeButton("Don't copy", (dialog, which) -> manager.doRestore(path, false))
                                        .setNeutralButton(R.string.common_word_cancel, null).show();
                            } else {
                                manager.doRestore(path, true);
                            }
                            getIntent().setData(null);
                        } else {
                            SketchwareUtil.toastError("Failed to copy backup file to temporary location: " + reason, Toast.LENGTH_LONG);
                        }
                    }
                }).copyFile(data);
            }
        }
    }

    // ==========================================
    // UTILITY KELAS DIFF CALLBACK
    // ==========================================
    private static class ProjectDiffCallback extends DiffUtil.Callback {
        private final List<HashMap<String, Object>> oldList;
        private final List<HashMap<String, Object>> newList;

        public ProjectDiffCallback(List<HashMap<String, Object>> oldList, List<HashMap<String, Object>> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = (String) oldList.get(oldItemPosition).get("sc_id");
            String newId = (String) newList.get(newItemPosition).get("sc_id");
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
