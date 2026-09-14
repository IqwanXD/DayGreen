package pro.sketchware.activities.main.fragments.projects;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.adapters.ProjectsAdapter;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.library.ProjectComparator;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialFadeThrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import a.a.a.DA;
import a.a.a.DB;
import a.a.a.lC;
import extensions.anbui.daydream.project.RestoreProject;
import mod.hey.studios.project.ProjectTracker;
import pro.sketchware.R;
import pro.sketchware.activities.main.activities.MainActivity;
import pro.sketchware.databinding.MyprojectsBinding;
import pro.sketchware.utility.UI;

public class ProjectsFragment extends DA {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<HashMap<String, Object>> projectsList = new ArrayList<>();
    private MyprojectsBinding binding;
    private ProjectsAdapter projectsAdapter;
    public final ActivityResultLauncher<Intent> openProjectSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
    private DB preference;
    private SearchView projectsSearchView;
    private MenuProvider menuProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
    }

    @Override
    public void b(int requestCode) {
    }

    public void toDesignActivity(String sc_id) {
        Intent intent = new Intent(requireContext(), DesignActivity.class);
        ProjectTracker.setScId(sc_id);
        intent.putExtra("sc_id", sc_id);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
    }

    @Override
    public void c(int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    @Override
    public void d() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
    }

    @Override
    public void e() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
    }

    public void toProjectSettingsActivity() {
        Intent intent = new Intent(getActivity(), MyProjectSettingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openProjectSettings.launch(intent);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        binding = MyprojectsBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        preference = new DB(requireContext(), "project");

        binding.swipeRefresh.setOnRefreshListener(this::refreshProjectsList);

        // Menyembunyikan spinner default SwipeRefreshLayout agar LoadingIndicator yang tampil
        binding.swipeRefresh.setColorSchemeColors(Color.TRANSPARENT);
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(Color.TRANSPARENT);

        projectsAdapter = new ProjectsAdapter(this, projectsList);
        binding.myprojects.setAdapter(projectsAdapter);
        binding.myprojects.setHasFixedSize(true);

        // Auto close FAB menu saat RecyclerView di-scroll
        binding.myprojects.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).closeFabMenu();
                    }
                }
            }
        });

        binding.myprojects.post(this::refreshProjectsList);
        UI.addSystemWindowInsetToPadding(binding.specialActionContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.loadingContainer, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.titleContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.myprojects, true, false, true, true);

        binding.iconSort.setOnClickListener(v -> showProjectSortingDialog());

        RestoreProject.setupDropFileTo(getActivity(), binding.specialAction.getRoot());

        menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.projects_fragment_menu, menu);
                projectsSearchView = (SearchView) menu.findItem(R.id.searchProjects).getActionView();
                if (projectsSearchView != null) {
                    // Hilangkan underline (garis bawah) pada SearchView
                    View searchPlate = projectsSearchView.findViewById(androidx.appcompat.R.id.search_plate);
                    if (searchPlate != null) {
                        searchPlate.setBackgroundColor(Color.TRANSPARENT);
                    }

                    // Hilangkan ikon pencarian internalSearchView saat fokus
                    ImageView searchMagIcon = projectsSearchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
                    if (searchMagIcon != null) {
                        searchMagIcon.setImageDrawable(null);
                        searchMagIcon.setVisibility(View.GONE);
                    }

                    projectsSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextChange(String s) {
                            projectsAdapter.filterData(s);

                            if (s.isEmpty()) {
                                binding.specialActionContainer.setVisibility(View.VISIBLE);
                                binding.titleContainer.setVisibility(View.VISIBLE);
                            } else {
                                binding.specialActionContainer.setVisibility(View.GONE);
                                binding.titleContainer.setVisibility(View.GONE);
                            }

                            return false;
                        }

                        @Override
                        public boolean onQueryTextSubmit(String s) {
                            return false;
                        }
                    });
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        };

        requireActivity().addMenuProvider(menuProvider);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getActivity() == null) return;
        if (hidden) {
            requireActivity().removeMenuProvider(menuProvider);
        } else {
            requireActivity().addMenuProvider(menuProvider);
        }
    }

    public void refreshProjectsList() {
        if (!isAdded()) return;

        if (!c()) {
            if (binding.swipeRefresh.isRefreshing()) binding.swipeRefresh.setRefreshing(false);
            ((MainActivity) requireActivity()).s();
            return;
        }

        // Tampilkan LoadingIndicator kustom saat refresh
        if (binding.loadingContainer.getVisibility() != View.VISIBLE) {
            binding.loadingContainer.setVisibility(View.VISIBLE);
        }

        executorService.execute(() -> {
            List<HashMap<String, Object>> loadedProjects = lC.a();
            loadedProjects.sort(new ProjectComparator(preference.d("sortBy"), preference.a("pinnedProject", "-1")));

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProjectDiffCallback(projectsList, loadedProjects));

            requireActivity().runOnUiThread(() -> {
                if (binding.swipeRefresh.isRefreshing()) {
                    binding.swipeRefresh.setRefreshing(false);
                }
                
                // Sembunyikan LoadingIndicator setelah data selesai dimuat
                binding.loadingContainer.setVisibility(View.GONE);
                binding.myprojects.setVisibility(View.VISIBLE);

                projectsList.clear();
                projectsList.addAll(loadedProjects);
                diffResult.dispatchUpdatesTo(projectsAdapter);
                if (projectsSearchView != null)
                    projectsAdapter.filterData(projectsSearchView.getQuery().toString());
            });
        });
    }

    private void addProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> newProject = lC.b(sc_id);
            if (newProject != null) {
                requireActivity().runOnUiThread(() -> {
                    projectsList.add(0, newProject);
                    projectsAdapter.notifyDataSetChanged();
                    binding.myprojects.scrollToPosition(0);
                });
            }
        });
    }

    private void updateProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> updatedProject = lC.b(sc_id);
            if (updatedProject != null) {
                int index = IntStream.range(0, projectsList.size()).filter(i -> projectsList.get(i).get("sc_id").equals(sc_id)).findFirst().orElse(-1);
                if (index != -1) {
                    projectsList.set(index, updatedProject);
                    requireActivity().runOnUiThread(() -> projectsAdapter.notifyDataSetChanged());
                }
            }
        });
    }

    private void showProjectSortingDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        dialog.setTitle("Sort options");

        LinearLayout layout = new LinearLayout(requireActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        RadioGroup radioGroup = new RadioGroup(requireActivity());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton sortOldest = new RadioButton(requireActivity());
        sortOldest.setText("Oldest");

        RadioButton sortNewest = new RadioButton(requireActivity());
        sortNewest.setText("Newest");

        RadioButton sortAscName = new RadioButton(requireActivity());
        sortAscName.setText("aA - zZ");

        RadioButton sortDescName = new RadioButton(requireActivity());
        sortDescName.setText("zZ - aZ");

        RadioButton sortDefault = new RadioButton(requireActivity());
        sortDefault.setText("Default (by ID)");

        radioGroup.addView(sortOldest);
        radioGroup.addView(sortNewest);
        radioGroup.addView(sortAscName);
        radioGroup.addView(sortDescName);
        radioGroup.addView(sortDefault);

        layout.addView(radioGroup);

        int storedValue = preference.a("sortBy", ProjectComparator.DEFAULT);
        if (storedValue == (ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_ASCENDING)) {
            sortOldest.setChecked(true);
        } else if (storedValue == (ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_DESCENDING)) {
            sortNewest.setChecked(true);
        } else if (storedValue == (ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_ASCENDING)) {
            sortAscName.setChecked(true);
        } else if (storedValue == (ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_DESCENDING)) {
            sortDescName.setChecked(true);
        } else {
            sortDefault.setChecked(true);
        }

        dialog.setView(layout);
        dialog.setPositiveButton("Save", (v, which) -> {
            int sortValue = ProjectComparator.DEFAULT;

            if (sortOldest.isChecked()) {
                sortValue = ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_ASCENDING;
            } else if (sortNewest.isChecked()) {
                sortValue = ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_DESCENDING;
            } else if (sortAscName.isChecked()) {
                sortValue = ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_ASCENDING;
            } else if (sortDescName.isChecked()) {
                sortValue = ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_DESCENDING;
            } else if (sortDefault.isChecked()) {
                sortValue = ProjectComparator.DEFAULT;
            }

            preference.a("sortBy", sortValue, true);
            v.dismiss();
            refreshProjectsList();
        });
        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

    private static class ProjectDiffCallback extends DiffUtil.Callback {
        private final List<HashMap<String, Object>> oldList;
        private final List<HashMap<String, Object>> newList;

        public ProjectDiffCallback(List<HashMap<String, Object>> oldList, List<HashMap<String, Object>> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = (String) oldList.get(oldItemPosition).get("sc_id");
            String newId = (String) newList.get(newItemPosition).get("sc_id");
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            HashMap<String, Object> oldItem = oldList.get(oldItemPosition);
            HashMap<String, Object> newItem = newList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }
}
