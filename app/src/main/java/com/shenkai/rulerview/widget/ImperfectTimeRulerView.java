package com.shenkai.rulerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import com.shenkai.rulerview.BaseApplication;
import com.shenkai.rulerview.R;
import com.shenkai.rulerview.utils.DensityUtils;


/**
 * Author:shenkai
 * Time:2020/5/13 9:37
 * Description:时间刻度尺控件
 */
public class ImperfectTimeRulerView extends View {
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
    private int mOldSelectedValue;
    private Paint mLinePaint;
    private Paint mPointerPaint;
    private Paint mTextPaint;
    private float mTextHeight;//尺子刻度下方数字的高度

    private int mTotalLineCount;//刻度线总数量
    private int mExtraLineCount = 50;//多余线数量
    private int mMaxOffset;//所有刻度共有多长
    private int mOffset;// 默认状态下，mSelectedValue所在的位置,位于尺子总刻度的位置
    private static final int SPACING_COUNT_BETWEEN_TEXT = 15;//文字之间的刻度尺间隔数量
    private int mLastX;
    private int mDeltaX;

    private OnValueChangeListener mListener;  // 滑动后数值回调

    private Scroller mScroller;//Scroller是一个专门用于处理滚动效果的工具类   用mScroller记录/计算View滚动的位置，再重写View的computeScroll()，完成实际的滚动
    private VelocityTracker mVelocityTracker;//主要用跟踪触摸屏事件（flinging事件和其他gestures手势事件）的速率。
    private int mMinVelocity;
    private int mMaxVelocity;
    private long lastKnobSoundTime;

    public ImperfectTimeRulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mScroller = new Scroller(context);
        mMinVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        mMaxVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimeRulerView);
        mLineWidth = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_lineWidth, DensityUtils.dipToPx(0.5f));
        mBoldLineWidth = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_boldLineWidth, DensityUtils.dipToPx(2f));
        mLineHeight = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_lineHeight, DensityUtils.dipToPx(40));
        mLineColor = typedArray.getColor(R.styleable.ImperfectTimeRulerView_lineColor, context.getColor(R.color.white));
        mLineGap = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_lineGap, DensityUtils.dipToPx(10));
        mPointerWidth = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_pointerWidth, DensityUtils.dipToPx(6));
        mPointerHeight = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_pointerHeight, DensityUtils.dipToPx(51));
        mPointerColor = typedArray.getColor(R.styleable.ImperfectTimeRulerView_pointerColor, context.getColor(R.color.setting_bt_color));
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.ImperfectTimeRulerView_rulerTextSize, DensityUtils.dipToPx(26));
        mTextColor = typedArray.getColor(R.styleable.ImperfectTimeRulerView_rulerTextColor, context.getColor(R.color.white));
        mTextMarginTop = typedArray.getDimensionPixelOffset(R.styleable.ImperfectTimeRulerView_textMarginTop, DensityUtils.dipToPx(23.5f));
        mSelectedValue = typedArray.getInt(R.styleable.ImperfectTimeRulerView_selectedValue, 60);
        mMinValue = typedArray.getInt(R.styleable.ImperfectTimeRulerView_minValue, 0);
        mMaxValue = typedArray.getInt(R.styleable.ImperfectTimeRulerView_maxValue, 1440);
        mStepValue = typedArray.getInt(R.styleable.ImperfectTimeRulerView_stepValue, 1);
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
    }

    public void setValue(int selectedValue, int minValue, int maxValue, int stepValue) {
        mSelectedValue = selectedValue;
        mMinValue = minValue;
        mMaxValue = maxValue;
        mStepValue = stepValue;

        mTotalLineCount = (mMaxValue - mMinValue) / mStepValue + 1;
        mMaxOffset = -(mTotalLineCount - 1) * mLineGap;
        mOffset = (mMinValue - mSelectedValue) / mStepValue * mLineGap;
        invalidate();
    }

    public void setOnValueChangeListener(OnValueChangeListener mListener) {
        this.mListener = mListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mLineGap = w / 36;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int lineHeight = mLineHeight;
        int pointerX = getWidth() / 2;
        for (int i = -mExtraLineCount; i < mTotalLineCount + mExtraLineCount; i++) {
            int lineX = pointerX + mOffset + i * mLineGap;
            if (lineX < 0 || lineX > getWidth()) {
                continue;//先画默认值在正中间，左右各一半的view。多余部分暂时不画(也就是从默认值在中间，画旁边左右的刻度线)
            }
            if (i >= 0 && i < mTotalLineCount) {
                if (i % SPACING_COUNT_BETWEEN_TEXT == 0) {
                    mLinePaint.setStrokeWidth(mBoldLineWidth);
                    mLinePaint.setColor(mLineColor);
                    canvas.drawLine(lineX, getHeight() / 2 - lineHeight / 2, lineX, getHeight() / 2 + lineHeight / 2, mLinePaint);
                    String value = minuteToTime(mMinValue + i * mStepValue);
                    canvas.drawText(value, lineX - mTextPaint.measureText(value) / 2,
                            getHeight() / 2 + lineHeight / 2 + mTextMarginTop + mTextHeight, mTextPaint);
                } else {
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
        canvas.drawLine(getWidth() / 2, getHeight() / 2 - mPointerHeight / 2, getWidth() / 2,
                getHeight() / 2 + mPointerHeight / 2, mPointerPaint);
    }

    private int mDownValue;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        int xPosition = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownValue = mSelectedValue;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastX = xPosition;
                mDeltaX = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                mDeltaX = xPosition - mLastX;
                updateMoveAndValue();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                countVelocityTracker();
                correctActionUP();
                recycleVelocityTracker();
                break;
        }
        mLastX = xPosition;
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {//mScroller.computeScrollOffset()返回 true表示滑动还没有结束
            if (mScroller.getCurrX() == mScroller.getFinalX()) {
                correctActionUP();
            } else {
                int currX = mScroller.getCurrX();
                mDeltaX = currX - mLastX;
                updateMoveAndValue();
                mLastX = currX;
            }
        }
    }

    private void countVelocityTracker() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity * 0.6f);//初始化速率的单位 px/s
        float xVelocity = mVelocityTracker.getXVelocity(); //当前的速度
        Log.d(TAG, "xVelocity:" + xVelocity);
        if (Math.abs(xVelocity) > mMinVelocity * 6) {
            mScroller.fling(getScrollX(), getScrollY(), (int) xVelocity, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
        }
    }

    //滑动过程中的操作
    private void updateMoveAndValue() {
        mOffset += Math.round(mDeltaX / 3.0f);
        if (mOffset <= mMaxOffset) {
            mOffset = mMaxOffset;
            mDeltaX = 0;
            mScroller.abortAnimation();
        } else if (mOffset >= 0) {
            mOffset = 0;
            mDeltaX = 0;
            mScroller.abortAnimation();
        }
        mSelectedValue = mMinValue + Math.round(Math.abs(mOffset) * 1.0f / mLineGap) * mStepValue;

        notifyValueChange();
        invalidate();
    }

    private void correctActionUP() {
        mOffset += Math.round(mDeltaX / 3.0f);
        if (mOffset <= mMaxOffset) {
            mOffset = mMaxOffset;
            mScroller.abortAnimation();
        } else if (mOffset >= 0) {
            mOffset = 0;
            mScroller.abortAnimation();
        }
        mLastX = 0;
        mDeltaX = 0;
        mSelectedValue = mMinValue + Math.round(Math.abs(mOffset) * 1.0f / mLineGap) * mStepValue;
        mOffset = (mMinValue - mSelectedValue) / mStepValue * mLineGap;

        notifyValueChange();
        invalidate();
    }

    private void notifyValueChange() {
        //滑动声音播放
        if (mOldSelectedValue != mSelectedValue) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastKnobSoundTime >= 50) {
//                AudioPlayUtil.play(AudioPlayUtil.AudioName.COMMON_KNOB_TURN);
                lastKnobSoundTime = currentTimeMillis;
            }
            mOldSelectedValue = mSelectedValue;
        }
        if (null != mListener) {
            mListener.onValueChange(mSelectedValue);
        }
    }

    //滑动后的回调
    public interface OnValueChangeListener {
        void onValueChange(int value);
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleVelocityTracker();
    }
}
