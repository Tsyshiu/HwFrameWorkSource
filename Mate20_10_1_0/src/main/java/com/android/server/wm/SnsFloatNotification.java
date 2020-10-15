package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.huawei.bd.Reporter;

public class SnsFloatNotification extends FrameLayout {
    private static final boolean DEBUG = false;
    private static final float FLYING_MIN_DISTANCE = 20.0f;
    private static final float FLYING_SPEED = 4.0f;
    private static final int GESTRUE_SCROLLING = 1;
    private static final int GESTRUE_SWIPING = 2;
    private static final float MIN_DISMISS_DISTANCE = 270.0f;
    private static final String TAG = "SnsFloatNotification";
    private static final boolean sHasNotch = (!TextUtils.isEmpty(SystemProperties.get("ro.config.hw_notch_size", "")));
    /* access modifiers changed from: private */
    public boolean mAnimating;
    private ValueAnimator mAnimator;
    private boolean mAttached;
    /* access modifiers changed from: private */
    public Context mContext;
    private float mInitRawX;
    private boolean mIsBeingDragged;
    private boolean mIsScrolling;
    private boolean mIsSwiping;
    private float mLastMotionRawX;
    private float mLastMotionRawY;
    private WindowManager.LayoutParams mLp;
    private WindowManager.LayoutParams mLpChanged;
    private int mMarginTop = 54;
    private int mScreenWidth = 1080;
    private float mStartRawX;
    private float mStartRawY;
    private int mTouchSlop;
    private WindowManager mWindowManager;

    public SnsFloatNotification(Context context) {
        super(context);
        this.mContext = context;
    }

    public SnsFloatNotification(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnsFloatNotification(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initLayout() {
        if (!sHasNotch || getResources().getConfiguration().orientation != 1) {
            this.mMarginTop = getContext().getResources().getDimensionPixelSize(34472503);
        } else {
            this.mMarginTop = getContext().getResources().getDimensionPixelSize(34472504);
        }
        this.mWindowManager = (WindowManager) getContext().getSystemService("window");
        this.mScreenWidth = this.mWindowManager.getDefaultDisplay().getWidth();
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public void addNotificationView(View view) {
        removeNotificationViewIfExists();
        addView(view);
        this.mLp = new WindowManager.LayoutParams(-2, -2, 2003, 40, -3);
        WindowManager.LayoutParams layoutParams = this.mLp;
        layoutParams.layoutInDisplayCutoutMode = 1;
        layoutParams.gravity = 49;
        layoutParams.y = this.mMarginTop;
        layoutParams.packageName = getContext().getPackageName();
        this.mLpChanged = new WindowManager.LayoutParams();
        this.mLpChanged.copyFrom(this.mLp);
        if (!this.mAttached) {
            addNotificationViewInternel();
        } else {
            updateNotificationViewInternel();
        }
    }

    public void removeNotificationView() {
        ValueAnimator valueAnimator;
        if (this.mAnimating && (valueAnimator = this.mAnimator) != null) {
            valueAnimator.cancel();
        }
        removeNotificationViewInternel();
    }

    private void addNotificationViewInternel() {
        if (!this.mAttached && this.mWindowManager != null) {
            HwSnsVideoManager.getInstance(this.mContext).setAttached(true);
            this.mWindowManager.addView(this, this.mLp);
            this.mAttached = true;
        }
    }

    private void updateNotificationViewInternel() {
        WindowManager windowManager;
        if (this.mAttached && (windowManager = this.mWindowManager) != null) {
            windowManager.updateViewLayout(this, this.mLp);
        }
    }

    private void removeNotificationViewInternel() {
        removeAllViews();
        if (this.mAttached && this.mWindowManager != null) {
            HwSnsVideoManager.getInstance(this.mContext).setAttached(false);
            this.mAttached = false;
            this.mWindowManager.removeView(this);
        }
    }

    private void setOffsetY(float diffY) {
        WindowManager.LayoutParams layoutParams = this.mLpChanged;
        layoutParams.y = (int) (((float) layoutParams.y) + diffY);
        int maxY = getMaxScrollY();
        WindowManager.LayoutParams layoutParams2 = this.mLpChanged;
        layoutParams2.y = layoutParams2.y > maxY ? maxY : this.mLpChanged.y;
        apply();
    }

    /* access modifiers changed from: private */
    public void setTargetY(float targetY) {
        this.mLpChanged.y = (int) targetY;
        apply();
    }

    private void setOffsetX(float diffX) {
        WindowManager.LayoutParams layoutParams = this.mLpChanged;
        layoutParams.x = (int) (((float) layoutParams.x) + diffX);
        apply();
    }

    /* access modifiers changed from: private */
    public void setTargetX(float targetX) {
        this.mLpChanged.x = (int) targetX;
        apply();
    }

    private void apply() {
        if (this.mLp.copyFrom(this.mLpChanged) != 0) {
            updateNotificationViewInternel();
        }
    }

    private void removeNotificationViewIfExists() {
        removeAllViews();
    }

    private int getMaxScrollY() {
        return this.mMarginTop;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x001c, code lost:
        if (r3 != 3) goto L_0x0044;
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        boolean z = true;
        if ((action == 2 && this.mIsBeingDragged) || this.mAnimating) {
            return true;
        }
        int i = action & 255;
        if (i != 0) {
            if (i != 1) {
                if (i == 2) {
                    int gesture = recongnizeGesture(ev.getRawX(), ev.getRawY());
                    if (gesture != 0) {
                        this.mIsBeingDragged = true;
                        this.mIsScrolling = gesture == 1;
                        if (gesture != 2) {
                            z = false;
                        }
                        this.mIsSwiping = z;
                    }
                }
            }
            this.mIsBeingDragged = false;
        } else {
            initDownEvent(ev);
        }
        return this.mIsBeingDragged;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0013, code lost:
        if (r0 != 3) goto L_0x006c;
     */
    public boolean onTouchEvent(MotionEvent event) {
        int gesture;
        if (this.mAnimating) {
            return true;
        }
        int action = event.getAction();
        if (action != 0) {
            boolean z = false;
            if (action != 1) {
                if (action == 2) {
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    if (!this.mIsScrolling && !this.mIsSwiping && (gesture = recongnizeGesture(rawX, rawY)) != 0) {
                        this.mIsBeingDragged = true;
                        if (gesture == 1) {
                            z = true;
                        }
                        this.mIsScrolling = z;
                        this.mIsSwiping = true ^ this.mIsScrolling;
                    }
                    if (this.mIsScrolling) {
                        handleScrolling(rawY);
                    }
                    if (this.mIsSwiping) {
                        handleSwiping(rawX);
                    }
                    if (this.mIsBeingDragged) {
                        this.mLastMotionRawX = rawX;
                        this.mLastMotionRawY = rawY;
                    }
                }
            }
            if (this.mIsBeingDragged) {
                this.mIsBeingDragged = false;
                if (this.mIsScrolling) {
                    this.mIsScrolling = false;
                    flying(true);
                }
                if (this.mIsSwiping) {
                    this.mIsSwiping = false;
                    flying(false);
                }
            }
        } else {
            initDownEvent(event);
        }
        return super.onTouchEvent(event);
    }

    private void initDownEvent(MotionEvent event) {
        this.mStartRawX = event.getRawX();
        this.mStartRawY = event.getRawY();
        this.mLastMotionRawX = event.getRawX();
        this.mLastMotionRawY = event.getRawY();
        this.mIsBeingDragged = false;
        this.mIsScrolling = false;
        this.mIsSwiping = false;
        this.mInitRawX = (float) this.mLp.x;
    }

    private int recongnizeGesture(float rawX, float rawY) {
        int whichGesture = 0;
        float durationY = rawY - this.mStartRawY;
        float distanceX = Math.abs(rawX - this.mStartRawX);
        float distanceY = Math.abs(durationY);
        if (distanceY > ((float) this.mTouchSlop) && distanceY > distanceX) {
            if (!(durationY > 0.0f) || canScrollDown()) {
                whichGesture = 1;
            }
        }
        if (whichGesture != 0 || distanceX <= ((float) this.mTouchSlop)) {
            return whichGesture;
        }
        return 2;
    }

    private boolean canScrollDown() {
        WindowManager.LayoutParams layoutParams = this.mLp;
        return layoutParams != null && layoutParams.y < this.mMarginTop;
    }

    private void handleScrolling(float rawY) {
        setOffsetY(rawY - this.mLastMotionRawY);
    }

    private void handleSwiping(float rawX) {
        setOffsetX(rawX - this.mLastMotionRawX);
    }

    private void flying(boolean isScroll) {
        float target;
        boolean goLeft = true;
        boolean dismiss = false;
        if (isScroll) {
            if (((float) this.mLp.y) < ((float) getMaxScrollY()) / 2.0f) {
                dismiss = true;
            }
            flying(true, (float) this.mLp.y, (float) (dismiss ? getHeight() * -1 : getMaxScrollY()), dismiss);
            return;
        }
        boolean dismiss2 = Math.abs(((float) this.mLp.x) - this.mInitRawX) > MIN_DISMISS_DISTANCE;
        if (((float) this.mLp.x) >= this.mInitRawX) {
            goLeft = false;
        }
        float current = (float) this.mLp.x;
        if (dismiss2) {
            target = (float) (goLeft ? -getWidth() : this.mScreenWidth);
        } else {
            target = this.mInitRawX;
        }
        flying(false, current, target, dismiss2);
    }

    private void flying(final boolean scroll, float current, float target, final boolean dismiss) {
        ValueAnimator valueAnimator;
        if (this.mAnimating && (valueAnimator = this.mAnimator) != null) {
            valueAnimator.cancel();
        }
        this.mAnimator = ValueAnimator.ofFloat(current, target);
        long duration = getFlyingDuration(current, target);
        if (duration > 0) {
            this.mAnimating = true;
            this.mAnimator.setDuration(duration);
            this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.server.wm.SnsFloatNotification.AnonymousClass1 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    ((Float) animation.getAnimatedValue()).floatValue();
                    if (scroll) {
                        SnsFloatNotification.this.setTargetY(((Float) animation.getAnimatedValue()).floatValue());
                    } else {
                        SnsFloatNotification.this.setTargetX(((Float) animation.getAnimatedValue()).floatValue());
                    }
                }
            });
            this.mAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.server.wm.SnsFloatNotification.AnonymousClass2 */

                public void onAnimationEnd(Animator animation) {
                    if (dismiss) {
                        HwSnsVideoManager manager = HwSnsVideoManager.getInstance(SnsFloatNotification.this.mContext);
                        Context access$200 = SnsFloatNotification.this.mContext;
                        Reporter.e(access$200, 802, "{pkg:" + manager.getPkgName() + "}");
                        manager.cancelRemoveRunnable();
                        manager.setReadyToShowActivity(false);
                        SnsFloatNotification.this.removeNotificationView();
                    }
                    boolean unused = SnsFloatNotification.this.mAnimating = false;
                }

                public void onAnimationCancel(Animator animation) {
                    boolean unused = SnsFloatNotification.this.mAnimating = false;
                }
            });
            this.mAnimator.start();
            return;
        }
        if (scroll) {
            setTargetY(target);
        } else {
            setTargetX(target);
        }
        if (dismiss) {
            HwSnsVideoManager manager = HwSnsVideoManager.getInstance(this.mContext);
            Context context = this.mContext;
            Reporter.e(context, 802, "{pkg:" + manager.getPkgName() + "}");
            manager.cancelRemoveRunnable();
            manager.setReadyToShowActivity(false);
            removeNotificationView();
        }
    }

    private long getFlyingDuration(float current, float target) {
        float distance = Math.abs(current - target);
        if (FLYING_MIN_DISTANCE < distance) {
            return (long) (distance / FLYING_SPEED);
        }
        return 0;
    }

    public boolean isAttached() {
        return this.mAttached;
    }
}
