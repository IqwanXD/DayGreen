package pro.sketchware.activities.main.fragments.projects;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.besome.sketch.adapters.ProjectsAdapter;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.library.ProjectComparator;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
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
    private EditText searchEditText;
    private View searchMagIcon;

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
        binding.swipeRefresh.setColorSchemeColors(MaterialColors.getColor(requireContext(), R.attr.colorPrimary, 0));
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(MaterialColors.getColor(requireContext(), R.attr.colorSurfaceContainer, 0));

        projectsAdapter = new ProjectsAdapter(this, projectsList);
        binding.myprojects.setAdapter(projectsAdapter);
        binding.myprojects.setHasFixedSize(true);
        applyUniformCornerRadiusToRecyclerView();

        binding.myprojects.post(this::refreshProjectsList);
        UI.addSystemWindowInsetToPadding(binding.specialActionContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.loadingContainer, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.titleContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.myprojects, true, false, true, true);

        View iconSort = requireActivity().findViewById(R.id.icon_sort);
        if (iconSort != null) {
            iconSort.setOnClickListener(v -> showProjectSortingDialog());
        }

        RestoreProject.setupDropFileTo(getActivity(), binding.specialAction.getRoot());

        searchEditText = requireActivity().findViewById(R.id.search_edit_text);
        searchMagIcon = requireActivity().findViewById(R.id.search_mag_icon);

        if (searchEditText != null) {
            searchEditText.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (searchMagIcon != null) {
                    searchMagIcon.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
                }
            });

            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (projectsAdapter != null) {
                        projectsAdapter.filterData(s.toString());
                    }
                    if (binding.specialActionContainer != null && binding.titleContainer != null) {
                        if (s.toString().isEmpty()) {
                            binding.specialActionContainer.setVisibility(View.VISIBLE);
                            binding.titleContainer.setVisibility(View.VISIBLE);
                        } else {
                            binding.specialActionContainer.setVisibility(View.GONE);
                            binding.titleContainer.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void applyUniformCornerRadiusToRecyclerView() {
        if (binding.myprojects instanceof com.google.android.material.card.MaterialCardView) {
            ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                    .setAllCorners(CornerFamily.ROUNDED, getResources().getDisplayMetrics().density * 32)
                    .build();
            ((com.google.android.material.card.MaterialCardView) binding.myprojects).setShapeAppearanceModel(shape);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getActivity() == null) return;
    }

    public void refreshProjectsList() {
        if (!isAdded()) return;

        if (!c()) {
            if (binding.swipeRefresh.isRefreshing()) binding.swipeRefresh.setRefreshing(false);
            ((MainActivity) requireActivity()).s();
            return;
        }

        executorService.execute(() -> {
            List<HashMap<String, Object>> loadedProjects = lC.a();
            loadedProjects.sort(new ProjectComparator(preference.d("sortBy"), preference.a("pinnedProject", "-1")));

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProjectDiffCallback(projectsList, loadedProjects));

            requireActivity().runOnUiThread(() -> {
                if (binding.swipeRefresh.isRefreshing()) binding.swipeRefresh.setRefreshing(false);
                if (binding.loadingContainer.getVisibility() == View.VISIBLE) {
                    binding.loadingContainer.setVisibility(View.GONE);
                    binding.myprojects.setVisibility(View.VISIBLE);
                }
                projectsList.clear();
                projectsList.addAll(loadedProjects);
                diffResult.dispatchUpdatesTo(projectsAdapter);
                if (searchEditText != null)
                    projectsAdapter.filterData(searchEditText.getText().toString());
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

        LinearLayout container = new LinearLayout(requireActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        RadioGroup radioGroup = new RadioGroup(requireActivity());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton sortByName = new RadioButton(requireActivity());
        sortByName.setText("aA - zZ (Name Ascending)");
        sortByName.setId(View.generateViewId());

        RadioButton sortByNameDesc = new RadioButton(requireActivity());
        sortByNameDesc.setText("zZ - aA (Name Descending)");
        sortByNameDesc.setId(View.generateViewId());

        RadioButton sortByIDAsc = new RadioButton(requireActivity());
        sortByIDAsc.setText("Oldest (ID Ascending)");
        sortByIDAsc.setId(View.generateViewId());

        RadioButton sortByIDDesc = new RadioButton(requireActivity());
        sortByIDDesc.setText("Newest (ID Descending)");
        sortByIDDesc.setId(View.generateViewId());

        RadioButton sortByDefaultID = new RadioButton(requireActivity());
        sortByDefaultID.setText("Default (dari id)");
        sortByDefaultID.setId(View.generateViewId());

        radioGroup.addView(sortByName);
        radioGroup.addView(sortByNameDesc);
        radioGroup.addView(sortByIDAsc);
        radioGroup.addView(sortByIDDesc);
        radioGroup.addView(sortByDefaultID);
        container.addView(radioGroup);

        int storedValue = preference.a("sortBy", ProjectComparator.DEFAULT);
        if ((storedValue & ProjectComparator.SORT_BY_NAME) == ProjectComparator.SORT_BY_NAME) {
            if ((storedValue & ProjectComparator.SORT_ORDER_ASCENDING) == ProjectComparator.SORT_ORDER_ASCENDING) {
                sortByName.setChecked(true);
            } else {
                sortByNameDesc.setChecked(true);
            }
        } else if ((storedValue & ProjectComparator.SORT_BY_ID) == ProjectComparator.SORT_BY_ID) {
            if ((storedValue & ProjectComparator.SORT_ORDER_ASCENDING) == ProjectComparator.SORT_ORDER_ASCENDING) {
                sortByIDAsc.setChecked(true);
            } else {
                sortByIDDesc.setChecked(true);
            }
        } else {
            sortByDefaultID.setChecked(true);
        }

        dialog.setView(container);
        dialog.setPositiveButton("Save", (v, which) -> {
            int sortValue = 0;
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId == sortByName.getId()) {
                sortValue = ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_ASCENDING;
            } else if (checkedId == sortByNameDesc.getId()) {
                sortValue = ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_DESCENDING;
            } else if (checkedId == sortByIDAsc.getId()) {
                sortValue = ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_ASCENDING;
            } else if (checkedId == sortByIDDesc.getId()) {
                sortValue = ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_DESCENDING;
            } else {
                sortValue = ProjectComparator.DEFAULT;
            }
            preference.a("sortBy", sortValue, true);
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
