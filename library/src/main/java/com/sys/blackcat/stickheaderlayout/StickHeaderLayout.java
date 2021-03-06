package com.sys.blackcat.stickheaderlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;


public class StickHeaderLayout extends ViewGroup {

    /**
     * 向上滚动headView 保留的高度
     */
    private int retentionHeight = 0;
    /**
     * 是否第一次布局
     */
    private boolean firstLayout = true;

    private DragEdge dragEdge = DragEdge.None;
    private IpmlScrollChangListener scroll;

    private View headView;
    private int headHeight;

    private View titleView;
    private int titleHeight;

    private View contentView;
    private int contentHeight;


    private ViewDragHelper mDragHelper;

    public StickHeaderLayout(Context context) {
        this(context, null);
    }

    public StickHeaderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickHeaderLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StickHeaderLayout);
        retentionHeight = typedArray.getDimensionPixelSize(R.styleable.StickHeaderLayout_retentionHeight, 0);
        typedArray.recycle();
        mDragHelper = ViewDragHelper.create(this, 1f, callback);
    }

    private ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (scroll != null) {
                if (state == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    scroll.onStopScroll();
                }
                if (state == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    scroll.onStartScroll();
                }
            }
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == contentView;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int headViewTop = headView.getTop() + dy;
            if (headViewTop > 0) {
                headViewTop = 0;
            } else if (headViewTop < -headHeight + retentionHeight) {
                headViewTop = -headHeight + retentionHeight;
            }
            headView.layout(0, headViewTop, getMeasuredWidth(), headViewTop + headHeight);
            if (scroll != null) {
                scroll.onScrollChange(Math.abs(headViewTop), headHeight - retentionHeight);
            }
            int titleTop = titleView.getTop() + dy;
            if (titleTop > headHeight) {
                titleTop = headHeight;
            } else if (titleTop < retentionHeight) {
                titleTop = retentionHeight;
            }
            titleView.layout(0, titleTop, getMeasuredWidth(), titleTop + titleHeight);
            int contentTop = contentView.getTop() + dy;
            if (contentTop > headHeight + titleHeight) {
                contentTop = headHeight + titleHeight;
            } else if (contentTop < titleHeight + retentionHeight) {
                contentTop = titleHeight + retentionHeight;
            }
            return contentTop;
        }


        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (dragEdge == DragEdge.Bottom) {
                if (yvel < -mDragHelper.getMinVelocity())
                    mDragHelper.smoothSlideViewTo(contentView, 0, titleHeight + retentionHeight);
            } else if (dragEdge == DragEdge.Top) {
                if (yvel > mDragHelper.getMinVelocity())
                    mDragHelper.smoothSlideViewTo(contentView, 0, titleHeight + headHeight);
            }
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            int contentTop = contentView.getTop();
            titleView.layout(0, contentTop - titleHeight, titleView.getMeasuredWidth(), contentTop);
            headView.layout(0, contentTop - titleHeight - headHeight, titleView.getMeasuredWidth(), contentTop - titleHeight);
            if (scroll != null) {
                scroll.onScrollChange(Math.abs(headView.getTop()), headHeight - retentionHeight);
            }
        }
    };


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        headView = getChildAt(0);
        measureChild(headView, widthMeasureSpec, heightMeasureSpec);
        headHeight = headView.getMeasuredHeight();
        titleView = getChildAt(1);
        measureChild(titleView, widthMeasureSpec, heightMeasureSpec);
        titleHeight = titleView.getMeasuredHeight();
        contentHeight = height - titleHeight - retentionHeight;
        contentView = getChildAt(2);
        contentView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!firstLayout) {
            dragEdge = DragEdge.None;
            mIsBeingDragged = false;
            headView.layout(0, headView.getTop(), headView.getMeasuredWidth(), headView.getBottom());
            titleView.layout(0, titleView.getTop(), titleView.getMeasuredWidth(), titleView.getBottom());
            contentView.layout(0, contentView.getTop(), contentView.getMeasuredWidth(), contentView.getBottom());
            return;
        } else {
            firstLayout = false;
            headView.layout(0, 0, headView.getMeasuredWidth(), headHeight);
            titleView.layout(0, headHeight, titleView.getMeasuredWidth(), headHeight + titleHeight);
            contentView.layout(0, headHeight + titleHeight, contentView.getMeasuredWidth(), headHeight + titleHeight + contentHeight);
        }
    }

    private float sX = -1, sY = -1;
    private boolean mIsBeingDragged;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isTouchContentView(ev)) {
            return super.onTouchEvent(ev);
        }
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDragHelper.processTouchEvent(ev);
                if (mDragHelper.isViewUnder(titleView, (int) ev.getX(), (int) ev.getY()) || mDragHelper.isViewUnder(headView, (int) ev.getX(), (int) ev.getY())) {
                    return super.onInterceptTouchEvent(ev);
                }
                sX = ev.getRawX();
                sY = ev.getRawY();
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                checkCanDrag(ev);
                if (dragEdge == DragEdge.Left || dragEdge == DragEdge.Right || dragEdge == DragEdge.None) {
                    return false;
                }
                if (scroll != null && !scroll.isReadyForPull()) {
                    if (dragEdge == DragEdge.Bottom && titleView.getTop() == headHeight) {
                        return true;
                    }
                    return false;
                } else {
                    if (dragEdge == DragEdge.Bottom) {
                        if (titleView.getTop() == retentionHeight) {
                            mIsBeingDragged = false;
                            dragEdge = DragEdge.None;
                        }
                    } else if (dragEdge == DragEdge.Top) {
                        if (titleView.getTop() == headHeight) {
                            dragEdge = DragEdge.None;
                            mIsBeingDragged = false;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mDragHelper.processTouchEvent(ev);
                break;
            default:
                mDragHelper.processTouchEvent(ev);
                break;
        }
        return mIsBeingDragged;
    }

    private void checkCanDrag(MotionEvent ev) {
        float dx = ev.getRawX() - sX;
        float dy = ev.getRawY() - sY;
        float angle = Math.abs(dy / dx);
        angle = (float) Math.toDegrees(Math.atan(angle));
        if (angle < 45) {
            if (dx > 0) {
                dragEdge = DragEdge.Left;
            } else if (dx < 0) {
                dragEdge = DragEdge.Right;
            }
        } else {
            if (dy > 0) {
                dragEdge = DragEdge.Top;
            } else if (dy < 0) {
                dragEdge = DragEdge.Bottom;
            }
        }
        mIsBeingDragged = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchContentView(event)) {
            return super.onTouchEvent(event);
        }
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDragHelper.processTouchEvent(event);
            case MotionEvent.ACTION_MOVE: {
                checkCanDrag(event);
                if (mIsBeingDragged) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    try {
                        mDragHelper.processTouchEvent(event);
                    } catch (Exception e) {
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mDragHelper.processTouchEvent(event);
                break;
            default://handle other action, such as ACTION_POINTER_DOWN/UP
                mDragHelper.processTouchEvent(event);
        }
        return super.onTouchEvent(event) || mIsBeingDragged || action == MotionEvent.ACTION_DOWN;
    }

    /**
     * 判断触点是否在ContentView在
     */
    private boolean isTouchContentView(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (mDragHelper.isViewUnder(titleView, x, y) || mDragHelper.isViewUnder(headView, x, y)) {
            return false;
        }
        return true;
    }

    public enum DragEdge {
        None,
        Left,
        Top,
        Right,
        Bottom
    }

    /**
     * 设置向上滚动headView 保留的高度
     *
     * @param retentionHeight
     */
    public void setRetentionHeight(int retentionHeight) {
        this.retentionHeight = retentionHeight;
    }

    public void setScroll(IpmlScrollChangListener scroll) {
        this.scroll = scroll;
    }
}
