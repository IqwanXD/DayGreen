package ru.iqwanoino;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Build;

public class SwipeRefreshLayout extends FrameLayout {
	
	public interface OnRefreshListener {
		void onRefresh();
	}
	
	private static final float DRAG_RATE = 0.45f; 
	private static final int ANIM_DURATION = 250;
	private static final int DEFAULT_REFRESH_TRIGGER_DP = 72; 
	
	// Konstanta Ukuran Standar
	public static final int DEFAULT = 1; // Ukuran standar (~40dp)
	public static final int LARGE = 0;   // Ukuran besar (~56dp)
	
	private View targetView;
	private FrameLayout indicatorContainer; 
	private ProgressBar progressBar;
	
	private int touchSlop;
	private float initialDownY;
	private boolean isBeingDragged;
	private boolean refreshing;
	private OnRefreshListener listener;
	
	private int refreshTrigger; 
	private int indicatorTotalSize;
	private int size = DEFAULT;
	
	// Menyimpan drawable background untuk memudahkan penggantian warna
	private GradientDrawable circleBg;
	
	public SwipeRefreshLayout(Context context) {
		this(context, null);
	}
	
	public SwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	private void init(Context ctx) {
		touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		float density = getResources().getDisplayMetrics().density;
		
		refreshTrigger = (int) (DEFAULT_REFRESH_TRIGGER_DP * density + 0.5f);
		int circleSize = (int) (40 * density); 
		int progressSize = (int) (24 * density); 
		indicatorTotalSize = circleSize;
		
		// 1. Membuat Kontainer Melingkar (M3 Floating Surface)
		indicatorContainer = new FrameLayout(getContext());
		circleBg = new GradientDrawable();
		circleBg.setShape(GradientDrawable.OVAL);
		circleBg.setColor(Color.parseColor("#FFFFFF")); // Default putih bersih
		indicatorContainer.setBackground(circleBg);
		
		if (Build.VERSION.SDK_INT >= 21) {
			indicatorContainer.setElevation(6 * density);
		}
		
		// 2. Membuat Indikator Progres M3 Sekunder
		progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleSmall);
		progressBar.setIndeterminate(true);
		if (Build.VERSION.SDK_INT >= 21) {
			progressBar.getIndeterminateDrawable().setTint(Color.parseColor("#000000")); // Default hitam M3
		}
		
		FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(progressSize, progressSize);
		progressLp.gravity = android.view.Gravity.CENTER;
		indicatorContainer.addView(progressBar, progressLp);
		
		LayoutParams containerLp = new LayoutParams(circleSize, circleSize);
		containerLp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
		containerLp.topMargin = -circleSize; 
		addView(indicatorContainer, containerLp);
		
		indicatorContainer.setVisibility(View.GONE);
		indicatorContainer.setScaleX(0f);
		indicatorContainer.setScaleY(0f);
		
		setClipToPadding(false);
		setWillNotDraw(false);
	}
	
	// --- METHOD UNTUK KUSTOMISASI WARNA M3 ---
	
	/**
* Mengubah warna latar belakang/background kontainer melingkar indikator.
* @param color Nilai warna (misal: Color.BLACK atau Color.parseColor("#FFFFFF"))
*/	
	public void setIndicatorBackgroundColor(int color) {
		if (circleBg != null) {
			circleBg.setColor(color);
		}
	}
	
	/**
* Mengubah warna putaran animasi ProgressBar di dalam lingkaran.
* @param color Nilai warna aksen (misal: Color.WHITE atau themeHelper.getColor("colorPrimary"))
*/	
	public void setProgressColor(int color) {
		if (progressBar != null && progressBar.getIndeterminateDrawable() != null) {
			if (Build.VERSION.SDK_INT >= 21) {
				progressBar.getIndeterminateDrawable().setTint(color);
			} else {
				progressBar.getIndeterminateDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
			}
		}
	}
	
	// --- METHOD BARU UNTUK UKURAN (SIZE) ---
	
	/**
* Mengubah ukuran ke tipe preset bawaan.
* @param size Gunakan SwipeRefreshLayout.DEFAULT atau SwipeRefreshLayout.LARGE
*/	
	public void setSize(int size) {
		if (size != DEFAULT && size != LARGE) {
			return;
		}
		this.size = size;
		updatePresetSizes();
	}
	
	/**
* Mengatur ukuran kustom dalam unit DP secara manual.
* @param circleSizeDp Diameter lingkaran luar (container) dalam DP.
* @param progressSizeDp Diameter progress putaran dalam DP.
* @param triggerDp Batas jarak tarik dalam DP untuk memicu refresh.
*/	
	public void setCustomSizeDp(int circleSizeDp, int progressSizeDp, int triggerDp) {
		float density = getResources().getDisplayMetrics().density;
		int circlePx = (int) (circleSizeDp * density + 0.5f);
		int progressPx = (int) (progressSizeDp * density + 0.5f);
		int triggerPx = (int) (triggerDp * density + 0.5f);
		
		applyCustomSizes(circlePx, progressPx, triggerPx);
	}
	
	/**
* Mengatur ukuran kustom dalam unit Pixel secara manual.
* @param circleSizePx Diameter lingkaran luar dalam Pixel.
* @param progressSizePx Diameter progress putaran dalam Pixel.
* @param triggerPx Batas jarak tarik dalam Pixel untuk memicu refresh.
*/	
	public void setCustomSizePx(int circleSizePx, int progressSizePx, int triggerPx) {
		applyCustomSizes(circleSizePx, progressSizePx, triggerPx);
	}
	
	private void updatePresetSizes() {
		float density = getResources().getDisplayMetrics().density;
		int circleSize;
		int progressSize;
		
		if (size == LARGE) {
			circleSize = (int) (56 * density + 0.5f);
			progressSize = (int) (32 * density + 0.5f);
			refreshTrigger = (int) (120 * density + 0.5f);
		} else {
			circleSize = (int) (40 * density + 0.5f);
			progressSize = (int) (24 * density + 0.5f);
			refreshTrigger = (int) (DEFAULT_REFRESH_TRIGGER_DP * density + 0.5f);
		}
		
		applyCustomSizes(circleSize, progressSize, refreshTrigger);
	}
	
	private void applyCustomSizes(int circlePx, int progressPx, int triggerPx) {
		this.refreshTrigger = triggerPx;
		this.indicatorTotalSize = circlePx;
		
		// Update LayoutParams lingkaran luar
		ViewGroup.LayoutParams containerLp = indicatorContainer.getLayoutParams();
		if (containerLp != null) {
			containerLp.width = circlePx;
			containerLp.height = circlePx;
			if (containerLp instanceof LayoutParams) {
				((LayoutParams) containerLp).topMargin = -circlePx;
			}
			indicatorContainer.setLayoutParams(containerLp);
		}
		
		// Update LayoutParams Progress bar di dalamnya
		ViewGroup.LayoutParams progressLp = progressBar.getLayoutParams();
		if (progressLp != null) {
			progressLp.width = progressPx;
			progressLp.height = progressPx;
			progressBar.setLayoutParams(progressLp);
		}
		
		// Reset/reposisi jika sedang tidak me-refresh
		if (!refreshing) {
			indicatorContainer.setTranslationY(0);
		} else {
			animateIndicatorTo(refreshTrigger);
		}
	}
	
	// --- AKHIR DARI METHOD UKURAN ---
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		if (getChildCount() > 1) {
			for (int i = 0; i < getChildCount(); i++) {
				View ch = getChildAt(i);
				if (ch != indicatorContainer) {
					targetView = ch;
					break;
				}
			}
		} else if (getChildCount() == 1 && getChildAt(0) != indicatorContainer) {
			targetView = getChildAt(0);
		}
	}
	
	public void setTargetView(View v) {
		this.targetView = v;
	}
	
	public void setOnRefreshListener(OnRefreshListener l) {
		this.listener = l;
	}
	
	public boolean isRefreshing() {
		return refreshing;
	}
	
	public void setRefreshing(boolean refreshing) {
		if (this.refreshing == refreshing) return;
		this.refreshing = refreshing;
		
		if (refreshing) {
			indicatorContainer.setVisibility(View.VISIBLE);
			indicatorContainer.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
			animateIndicatorTo(refreshTrigger);
		} else {
			indicatorContainer.animate()
			.scaleX(0f)
			.scaleY(0f)
			.setDuration(ANIM_DURATION)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					indicatorContainer.setVisibility(View.GONE);
					animateIndicatorTo(0);
					indicatorContainer.animate().setListener(null);
				}
			}).start();
		}
	}
	
	private void animateIndicatorTo(int toOffset) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(indicatorContainer, "translationY", indicatorContainer.getTranslationY(), toOffset);
		anim.setDuration(ANIM_DURATION);
		anim.setInterpolator(new DecelerateInterpolator());
		anim.start();
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!isEnabled() || canChildScrollUp() || refreshing) {
			return false;
		}
		
		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			initialDownY = ev.getY();
			isBeingDragged = false;
			break;
			case MotionEvent.ACTION_MOVE:
			float y = ev.getY();
			float yDiff = y - initialDownY;
			if (yDiff > touchSlop && !isBeingDragged) {
				isBeingDragged = true;
				indicatorContainer.setVisibility(View.VISIBLE);
			}
			break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			isBeingDragged = false;
			break;
		}
		return isBeingDragged;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!isEnabled() || canChildScrollUp() || refreshing) {
			return false;
		}
		
		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			initialDownY = ev.getY();
			return true;
			case MotionEvent.ACTION_MOVE: {
				float y = ev.getY();
				float dy = (y - initialDownY) * DRAG_RATE;
				if (dy < 0) return false;
				
				float maxDrag = refreshTrigger * 1.5f;
				if (dy > maxDrag) dy = maxDrag;
				
				moveIndicator((int) dy);
				return true;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				float y = ev.getY();
				float dy = (y - initialDownY) * DRAG_RATE;
				
				if (dy > refreshTrigger) {
					setRefreshing(true);
					if (listener != null) {
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								listener.onRefresh();
							}
						}, 100);
					}
				} else {
					indicatorContainer.animate().scaleX(0f).scaleY(0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							indicatorContainer.setVisibility(View.GONE);
							moveIndicator(0);
							indicatorContainer.animate().setListener(null);
						}
					}).start();
				}
				isBeingDragged = false;
				return true;
			}
		}
		return super.onTouchEvent(ev);
	}
	
	private void moveIndicator(int offset) {
		indicatorContainer.setTranslationY(offset);
		
		if (offset > 0) {
			float progress = (float) offset / refreshTrigger;
			float scale = Math.min(1.0f, progress);
			indicatorContainer.setScaleX(scale);
			indicatorContainer.setScaleY(scale);
			
			progressBar.setRotation(offset * 2.0f); 
		}
	}
	
	public boolean canChildScrollUp() {
		if (targetView == null) return true;
		if (targetView instanceof ListView) {
			ListView lv = (ListView) targetView;
			if (lv.getChildCount() == 0) return false;
			if (lv.getFirstVisiblePosition() > 0) return true;
			View firstChild = lv.getChildAt(0);
			if (firstChild == null) return false;
			return firstChild.getTop() < lv.getPaddingTop();
		} else {
			return targetView.canScrollVertically(-1);
		}
	}
}
