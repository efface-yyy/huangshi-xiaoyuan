package com.smart.hgnu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

/** Pulls at the top of a page refresh it without moving or resizing the fitted timetable. */
final class PullToRefreshLayout extends FrameLayout {
    private final Runnable onRefresh;
    private final ProgressBar indicator;
    private final int touchSlop;
    private final float threshold;
    private View content;
    private float startX, startY;
    private boolean dragging, refreshing;

    PullToRefreshLayout(Context context, Runnable onRefresh) {
        super(context);
        this.onRefresh = onRefresh;
        float density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        threshold = 64 * density;
        setContentDescription("下拉刷新");
        indicator = new ProgressBar(context);
        indicator.setContentDescription("正在同步数据");
        indicator.setIndeterminateTintList(ColorStateList.valueOf(UiComponents.GREEN));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xffffffff);
        background.setShape(GradientDrawable.OVAL);
        indicator.setBackground(background);
        int padding = Math.round(5 * density);
        indicator.setPadding(padding, padding, padding, padding);
        indicator.setElevation(4 * density);
        LayoutParams params =
                new LayoutParams(
                        Math.round(34 * density),
                        Math.round(34 * density),
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        params.topMargin = Math.round(8 * density);
        addView(indicator, params);
        indicator.setVisibility(GONE);
    }

    void setContent(View content) {
        this.content = content;
        addView(content, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
        indicator.setTranslationY(0);
        indicator.setAlpha(1);
        indicator.setVisibility(refreshing ? VISIBLE : GONE);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (refreshing || event.getPointerCount() != 1) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - startY;
                if (deltaY > touchSlop * 2
                        && deltaY > Math.abs(event.getX() - startX) * 1.5f
                        && !canScrollUpAt(content, startX, startY)) {
                    dragging = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                dragging = false;
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!dragging) return super.onTouchEvent(event);
        float distance = Math.max(0, event.getY() - startY);
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            indicator.setVisibility(VISIBLE);
            indicator.setAlpha(Math.min(1, distance / threshold));
            indicator.setTranslationY(Math.min(distance * .3f, threshold * .6f));
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            dragging = false;
            if (distance >= threshold) {
                setRefreshing(true);
                onRefresh.run();
            } else setRefreshing(false);
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            setRefreshing(false);
        }
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallow) {
        // A ScrollView at its top must still allow the parent to recognize the downward pull.
        if (disallow && !canScrollUpAt(content, startX, startY)) return;
        super.requestDisallowInterceptTouchEvent(disallow);
    }

    private boolean canScrollUpAt(View view, float x, float y) {
        if (view == null) return false;
        if (view.canScrollVertically(-1)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = group.getChildCount() - 1; index >= 0; index--) {
                View child = group.getChildAt(index);
                float childX = x - child.getX(), childY = y - child.getY();
                if (child.getVisibility() == VISIBLE
                        && childX >= 0
                        && childY >= 0
                        && childX < child.getWidth()
                        && childY < child.getHeight()) return canScrollUpAt(child, childX, childY);
            }
        }
        return false;
    }
}
