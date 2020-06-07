package com.shenkai.rulerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.shenkai.rulerview.R;
import com.shenkai.rulerview.utils.DensityUtils;


/**
 * Author:shenkai
 * Time:2020/6/5 9:46
 * Description:
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class TimeRulerView extends View implements View.OnScrollChangeListener {
    private static final String TAG = "TimeRulerView";

    private int mLineWidth;
    private int mBoldLineWidth;
    private int mLineHeight;
    private int mLineColor;
    private int mLineGap;
    private int mPointerWidth;
    private int mPointerHeight;
    private int mPointerColor;
    private int mTextSize;
    private int mTextColor;
    private int mTextMarginTop;
    private int mSelectedValue;
    private int mMinValue;
    private int mMaxValue;
    private int mStepValue;//步进值
    private int mStartSeriesValue;
    private int mEndSeriesValue;
    private Paint mLinePaint;
    private Paint mPointerPaint;
    private Paint mTextPaint;
    private float mTextHeight;//尺子刻度下方数字的高度

    private int mTotalLineCount;//刻度线总数量
    private int mSelectedSeriesValue;//最小值除以步进值和最大值除以步进值之间的一个值
    private int mOldSelectedSeriesValue;
    private int mExtraLineCount = 50;//多余线数量
    private SparseIntArray mScalePixelsMap = new SparseIntArray();//第几个刻度和对应的像素值
    private int mBaseLineOffset;//把正中间刻度线作为基准线
    private static final int SPACING_COUNT_BETWEEN_TEXT = 15;//文字之间的刻度尺间隔数量
    private float mLastX;

    private Scroller mScroller;//Scroller是一个专门用于处理滚动效果的工具类   用mScroller记录/计算View滚动的位置，再重写View的computeScroll()，完成实际的滚动
    private VelocityTracker mVelocityTracker;//主要用跟踪触摸屏事件（flinging事件和其他gestures手势事件）的速率。
    private int mMinVelocity;
    private int mMaxVelocity;
    private OnValueChangeListener mListener;

    public interface OnValueChangeListener {
        void onValueChange(int value);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public TimeRulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void init(Context context, AttributeSet attrs) {
        mScroller = new Scroller(context);
        mMinVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        mMaxVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        Log.i(TAG, "mMinVelocity:" + mMinVelocity + ",mMaxVelocity" + mMaxVelocity);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimeRulerView);
        mLineWidth = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_lineWidth, DensityUtils.dipToPx(0.5f));
        mBoldLineWidth = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_boldLineWidth, DensityUtils.dipToPx(2f));
        mLineHeight = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_lineHeight, DensityUtils.dipToPx(40));
        mLineColor = typedArray.getColor(R.styleable.TimeRulerView_lineColor, context.getColor(R.color.white));
        mLineGap = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_lineGap, DensityUtils.dipToPx(10));
        mPointerWidth = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_pointerWidth, DensityUtils.dipToPx(6));
        mPointerHeight = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_pointerHeight, DensityUtils.dipToPx(51));
        mPointerColor = typedArray.getColor(R.styleable.TimeRulerView_pointerColor, context.getColor(R.color.setting_bt_color));
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.TimeRulerView_rulerTextSize, DensityUtils.dipToPx(26));
        mTextColor = typedArray.getColor(R.styleable.TimeRulerView_rulerTextColor, context.getColor(R.color.white));
        mTextMarginTop = typedArray.getDimensionPixelOffset(R.styleable.TimeRulerView_textMarginTop, DensityUtils.dipToPx(23.5f));
        mSelectedValue = typedArray.getInt(R.styleable.TimeRulerView_selectedValue, 60);
        mMinValue = typedArray.getInt(R.styleable.TimeRulerView_minValue, 0);
        mMaxValue = typedArray.getInt(R.styleable.TimeRulerView_maxValue, 1440);
        mStepValue = typedArray.getInt(R.styleable.TimeRulerView_stepValue, 1);
        typedArray.recycle();

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStrokeWidth(mLineWidth);
        mLinePaint.setColor(context.getColor(R.color.white));

        mPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointerPaint.setStrokeWidth(mPointerWidth);
        mPointerPaint.setColor(mPointerColor);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextHeight = getFontHeight(mTextPaint);
        setOnScrollChangeListener(this);
    }

    public void setValue(int selectedValue, int minValue, int maxValue, int stepValue) {
        mSelectedValue = selectedValue;
        mMinValue = minValue;
        mMaxValue = maxValue;
        mStepValue = stepValue;
        mStartSeriesValue = minValue / stepValue;
        mEndSeriesValue = maxValue / stepValue;

        mTotalLineCount = (mMaxValue - mMinValue) / mStepValue + 1;
        mSelectedSeriesValue = mSelectedValue / mStepValue;
    }

    public void setOnValueChangeListener(OnValueChangeListener mListener) {
        this.mListener = mListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mLineGap = w / 36;
        mBaseLineOffset = w / 2;
        for (int i = mStartSeriesValue; i <= mEndSeriesValue; i++) {
            int pixels = (i - mStartSeriesValue) * mLineGap - mBaseLineOffset + mBaseLineOffset % mLineGap;
            if (pixels % mLineGap != 0) {
                pixels -= pixels % mLineGap;
            }
            mScalePixelsMap.put(i, pixels);
        }
        if (mScalePixelsMap.get(mSelectedSeriesValue) != 0) {
            scrollTo(mScalePixelsMap.get(mSelectedSeriesValue), 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int lineHeight = mLineHeight;
        int baseLineOffset = mBaseLineOffset;
        for (int i = mStartSeriesValue - mExtraLineCount; i < mEndSeriesValue + mExtraLineCount; i++) {
            int lineX = (i - mStartSeriesValue) * mLineGap;
            if (i >= mStartSeriesValue && i <= mEndSeriesValue) {
                if ((i * mStepValue) % SPACING_COUNT_BETWEEN_TEXT == 0) {//绘制大刻度线和线下面文字
                    mLinePaint.setStrokeWidth(mBoldLineWidth);
                    mLinePaint.setColor(mLineColor);
                    canvas.drawLine(lineX, getHeight() / 2 - lineHeight / 2, lineX, getHeight() / 2 + lineHeight / 2, mLinePaint);
                    String value = minuteToTime(mMinValue + i * mStepValue);
                    canvas.drawText(value, lineX - mTextPaint.measureText(value) / 2,
                            getHeight() / 2 + lineHeight / 2 + mTextMarginTop + mTextHeight, mTextPaint);
                } else {//绘制普通刻度线
                    mLinePaint.setStrokeWidth(mLineWidth);
                    mLinePaint.setColor(mLineColor);
                    canvas.drawLine(lineX, getHeight() / 2 - lineHeight / 2, lineX, getHeight() / 2 + lineHeight / 2, mLinePaint);
                }
            } else {
                mLinePaint.setStrokeWidth(mLineWidth);
                mLinePaint.setColor(getContext().getColor(R.color.color_222222));
                canvas.drawLine(lineX, getHeight() / 2 - lineHeight / 2, lineX, getHeight() / 2 + lineHeight / 2, mLinePaint);
            }
        }
        //绘制指示器
        int startX = baseLineOffset + getScrollX() - baseLineOffset % mLineGap;
        canvas.drawLine(startX, getHeight() / 2 - mPointerHeight / 2, startX,
                getHeight() / 2 + mPointerHeight / 2, mPointerPaint);
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        computeAndCallback(scrollX);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        int startSeriesValue = mStartSeriesValue;
        int endSeriesValue = mEndSeriesValue;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mScroller.abortAnimation();
                mLastX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float deltaX = moveX - mLastX;
                int scrollXDistance = (int) (getScrollX() - (deltaX / 3));
                if (scrollXDistance <= mScalePixelsMap.get(startSeriesValue)) {
                    scrollXDistance = mScalePixelsMap.get(startSeriesValue);
                }
                if (scrollXDistance >= mScalePixelsMap.get(endSeriesValue)) {
                    scrollXDistance = mScalePixelsMap.get(endSeriesValue);
                }
                scrollTo(scrollXDistance, 0);
                mLastX = moveX;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                countVelocityTracker();
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    //计算和回调
    private void computeAndCallback(int scrollX) {
        Log.w(TAG, "computeAndCallback:" + scrollX);
        mSelectedSeriesValue = getCurrentSeriesValue(scrollX);

        if (mSelectedSeriesValue >= mEndSeriesValue) {
            mSelectedSeriesValue = mEndSeriesValue;
        } else if (mSelectedSeriesValue <= mStartSeriesValue) {
            mSelectedSeriesValue = mStartSeriesValue;
        }
        if (mListener != null) {
            mListener.onValueChange(mSelectedSeriesValue * mStepValue);
        }
        if (mOldSelectedSeriesValue != mSelectedSeriesValue) {
            playSounds();
            mOldSelectedSeriesValue = mSelectedSeriesValue;
        }
    }

    private void playSounds() {
        Log.w(TAG, "playSounds:");
    }

    private int getCurrentSeriesValue(int scrollX) {
        int finalX = mBaseLineOffset + scrollX;
        if (finalX % mLineGap != 0) {
            finalX -= finalX % mLineGap;
        }
        return mMinValue / mStepValue + finalX / mLineGap;
    }

    private void countVelocityTracker() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);//初始化速率的单位 px/s
        float xVelocity = mVelocityTracker.getXVelocity(); //当前的速度
        Log.d(TAG, "xVelocity:" + xVelocity);
        if (Math.abs(xVelocity) > mMinVelocity * 30) {
            mScroller.fling(getScrollX(), getScrollY(), -(int) xVelocity, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
        } else {
            //手指抬起校正位置
//            mScroller.startScroll(getScrollX(), 0, mScalePixelsMap.get(mSelectedSeriesValue) - getScrollX(), 350);
//            invalidate();
            scrollTo(mScalePixelsMap.get(mSelectedSeriesValue), 0);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            Log.e(TAG, "currX:" + mScroller.getCurrX() + ",finalX:" + mScroller.getFinalX() + ",getScrollX:" + getScrollX());
            if (mScroller.getCurrX() == mScroller.getFinalX()) {
                scrollTo(mScalePixelsMap.get(mSelectedSeriesValue), 0);
            } else {
                int currX = mScroller.getCurrX();
                if (currX <= mScalePixelsMap.get(mStartSeriesValue)) {
                    currX = mScalePixelsMap.get(mStartSeriesValue);
                }
                if (currX >= mScalePixelsMap.get(mEndSeriesValue)) {
                    currX = mScalePixelsMap.get(mEndSeriesValue);
                }
                scrollTo(currX, 0);
                invalidate();
            }
        }
    }

    private float getFontHeight(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return fm.descent - fm.ascent;
    }

    private String minuteToTime(int minute) {
        int hour = minute / 60;
        minute = minute % 60;
        if (hour == 0) {
            hour = 12;
        } else if (hour > 12) {
            hour = hour - 12;
        }

        return hour + ":" + (minute < 10 ? "0" + minute : String.valueOf(minute));
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }
}
