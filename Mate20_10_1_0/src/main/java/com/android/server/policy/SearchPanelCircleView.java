package com.android.server.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.ArrayList;

public class SearchPanelCircleView extends FrameLayout {
    public static final Interpolator ALPHA_OUT = new PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f);
    private static final String TAG = "SearchPanelCircleView";
    private Boolean isLandScapeProduct;
    private boolean mAnimatingOut;
    /* access modifiers changed from: private */
    public final Interpolator mAppearInterpolator;
    /* access modifiers changed from: private */
    public final Paint mBackgroundPaint;
    private final int mBaseMargin;
    private float mCircleAnimationEndValue;
    /* access modifiers changed from: private */
    public ValueAnimator mCircleAnimator;
    private boolean mCircleHidden;
    private final int mCircleMinSize;
    /* access modifiers changed from: private */
    public final Rect mCircleRect;
    /* access modifiers changed from: private */
    public float mCircleSize;
    private ValueAnimator.AnimatorUpdateListener mCircleUpdateListener;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mClipToOutline;
    /* access modifiers changed from: private */
    public final Interpolator mDisappearInterpolator;
    private boolean mDraggedFarEnough;
    /* access modifiers changed from: private */
    public ValueAnimator mFadeOutAnimator;
    private final Interpolator mFastOutSlowInInterpolator;
    /* access modifiers changed from: private */
    public ImageView mLogo;
    private final int mMaxElevation;
    private float mOffset;
    private boolean mOffsetAnimatingIn;
    /* access modifiers changed from: private */
    public ValueAnimator mOffsetAnimator;
    private ValueAnimator.AnimatorUpdateListener mOffsetUpdateListener;
    /* access modifiers changed from: private */
    public float mOutlineAlpha;
    /* access modifiers changed from: private */
    public final Paint mRipplePaint;
    /* access modifiers changed from: private */
    public ArrayList<Ripple> mRipples;
    private final int mStaticOffset;
    private final Rect mStaticRect;

    public SearchPanelCircleView(Context context) {
        this(context, null);
    }

    public SearchPanelCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchPanelCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SearchPanelCircleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mBackgroundPaint = new Paint();
        this.mRipplePaint = new Paint();
        this.mCircleRect = new Rect();
        this.mStaticRect = new Rect();
        this.mRipples = new ArrayList<>();
        this.isLandScapeProduct = Boolean.valueOf(SystemProperties.getInt("ro.panel.hw_orientation", 0) == 90);
        this.mCircleUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass1 */

            public void onAnimationUpdate(ValueAnimator animation) {
                SearchPanelCircleView.this.applyCircleSize(((Float) animation.getAnimatedValue()).floatValue());
                SearchPanelCircleView.this.updateElevation();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass2 */

            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = SearchPanelCircleView.this.mCircleAnimator = null;
            }
        };
        this.mOffsetUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass3 */

            public void onAnimationUpdate(ValueAnimator animation) {
                SearchPanelCircleView.this.setOffset(((Float) animation.getAnimatedValue()).floatValue());
            }
        };
        setOutlineProvider(new ViewOutlineProvider() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass4 */

            public void getOutline(View view, Outline outline) {
                if (SearchPanelCircleView.this.mCircleSize > 0.0f) {
                    outline.setOval(SearchPanelCircleView.this.mCircleRect);
                } else {
                    outline.setEmpty();
                }
                outline.setAlpha(SearchPanelCircleView.this.mOutlineAlpha);
            }
        });
        setWillNotDraw(false);
        this.mCircleMinSize = context.getResources().getDimensionPixelSize(34472143);
        this.mBaseMargin = context.getResources().getDimensionPixelSize(34472144);
        this.mStaticOffset = context.getResources().getDimensionPixelSize(34472145);
        this.mMaxElevation = context.getResources().getDimensionPixelSize(34472146);
        this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
        this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
        this.mBackgroundPaint.setAntiAlias(true);
        this.mBackgroundPaint.setColor(getResources().getColor(33882315));
        this.mRipplePaint.setColor(getResources().getColor(33882316));
        this.mRipplePaint.setAntiAlias(true);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawRipples(canvas);
    }

    private void drawRipples(Canvas canvas) {
        int size = this.mRipples.size();
        for (int i = 0; i < size; i++) {
            this.mRipples.get(i).draw(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawCircle((float) this.mCircleRect.centerX(), (float) this.mCircleRect.centerY(), this.mCircleSize / 2.0f, this.mBackgroundPaint);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLogo = (ImageView) findViewById(34603140);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        ImageView imageView = this.mLogo;
        imageView.layout(0, 0, imageView.getMeasuredWidth(), this.mLogo.getMeasuredHeight());
        if (changed) {
            updateCircleRect(this.mStaticRect, (float) this.mStaticOffset, true);
        }
    }

    public void setCircleSize(float circleSize) {
        setCircleSize(circleSize, false, null, 0, null);
    }

    private static class MyAnimatorListenerAdapter extends AnimatorListenerAdapter {
        Runnable mRunnable;

        public MyAnimatorListenerAdapter(Runnable runnable) {
            this.mRunnable = runnable;
        }

        public void onAnimationEnd(Animator animation) {
            Runnable runnable = this.mRunnable;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public void setCircleSize(float circleSize, boolean animated, Runnable endRunnable, int startDelay, Interpolator interpolator) {
        Interpolator desiredInterpolator;
        boolean isAnimating = this.mCircleAnimator != null;
        boolean animationPending = isAnimating && !this.mCircleAnimator.isRunning();
        boolean animatingOut = isAnimating && this.mCircleAnimationEndValue == 0.0f;
        if (animated || animationPending || animatingOut) {
            if (isAnimating) {
                this.mCircleAnimator.cancel();
            }
            this.mCircleAnimator = ValueAnimator.ofFloat(this.mCircleSize, circleSize);
            this.mCircleAnimator.addUpdateListener(this.mCircleUpdateListener);
            this.mCircleAnimator.addListener(this.mClearAnimatorListener);
            this.mCircleAnimator.addListener(new MyAnimatorListenerAdapter(endRunnable));
            if (interpolator != null) {
                desiredInterpolator = interpolator;
            } else {
                desiredInterpolator = this.mDisappearInterpolator;
            }
            this.mCircleAnimator.setInterpolator(desiredInterpolator);
            this.mCircleAnimator.setDuration(300L);
            this.mCircleAnimator.setStartDelay((long) startDelay);
            this.mCircleAnimator.start();
            this.mCircleAnimationEndValue = circleSize;
        } else if (isAnimating) {
            this.mCircleAnimator.getValues()[0].setFloatValues(circleSize - this.mCircleAnimationEndValue, circleSize);
            ValueAnimator valueAnimator = this.mCircleAnimator;
            valueAnimator.setCurrentPlayTime(valueAnimator.getCurrentPlayTime());
            this.mCircleAnimationEndValue = circleSize;
        } else {
            applyCircleSize(circleSize);
            updateElevation();
        }
    }

    /* access modifiers changed from: private */
    public void applyCircleSize(float circleSize) {
        this.mCircleSize = circleSize;
        updateLayout();
    }

    /* access modifiers changed from: private */
    public void updateElevation() {
        int i = this.mStaticOffset;
        float t = (((float) i) - this.mOffset) / ((float) i);
        float t2 = 0.0f;
        if (1.0f - t > 0.0f) {
            t2 = t;
        }
        setElevation(((float) this.mMaxElevation) * t2);
    }

    public void setOffset(float offset) {
        setOffset(offset, false, 0, null, null);
    }

    private void setOffset(float offset, boolean animate, int startDelay, Interpolator interpolator, final Runnable endRunnable) {
        if (!animate) {
            this.mOffset = offset;
            updateLayout();
            if (endRunnable != null) {
                endRunnable.run();
                return;
            }
            return;
        }
        ValueAnimator valueAnimator = this.mOffsetAnimator;
        if (valueAnimator != null) {
            valueAnimator.removeAllListeners();
            this.mOffsetAnimator.cancel();
        }
        boolean z = true;
        this.mOffsetAnimator = ValueAnimator.ofFloat(this.mOffset, offset);
        this.mOffsetAnimator.addUpdateListener(this.mOffsetUpdateListener);
        this.mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass5 */

            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = SearchPanelCircleView.this.mOffsetAnimator = null;
                Runnable runnable = endRunnable;
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        this.mOffsetAnimator.setInterpolator(interpolator != null ? interpolator : this.mDisappearInterpolator);
        this.mOffsetAnimator.setStartDelay((long) startDelay);
        this.mOffsetAnimator.setDuration(300L);
        this.mOffsetAnimator.start();
        if (offset == 0.0f) {
            z = false;
        }
        this.mOffsetAnimatingIn = z;
    }

    private void updateLayout() {
        updateCircleRect();
        updateLogo();
        invalidateOutline();
        invalidate();
        updateClipping();
    }

    /* access modifiers changed from: private */
    public void updateClipping() {
        boolean clip = this.mCircleSize < ((float) this.mCircleMinSize) || !this.mRipples.isEmpty();
        if (clip != this.mClipToOutline) {
            setClipToOutline(clip);
            this.mClipToOutline = clip;
        }
    }

    private void updateLogo() {
        boolean exitAnimationRunning = this.mFadeOutAnimator != null;
        Rect rect = exitAnimationRunning ? this.mCircleRect : this.mStaticRect;
        float translationX = (((float) (rect.left + rect.right)) / 2.0f) - (((float) this.mLogo.getWidth()) / 2.0f);
        float translationY = (((float) (rect.top + rect.bottom)) / 2.0f) - (((float) this.mLogo.getHeight()) / 2.0f);
        int i = this.mStaticOffset;
        float f = this.mOffset;
        float t = (((float) i) - f) / ((float) i);
        if (!exitAnimationRunning) {
            if (isLand()) {
                translationX += ((float) this.mStaticOffset) * t * 0.3f;
            } else {
                translationY += ((float) this.mStaticOffset) * t * 0.3f;
            }
            float alphaTmp = ((1.0f - t) - 0.5f) * 2.0f;
            float alpha = 0.0f;
            if (alphaTmp > 0.0f) {
                alpha = alphaTmp;
            }
            this.mLogo.setAlpha(alpha);
        } else {
            translationY += (f - ((float) i)) / 2.0f;
        }
        this.mLogo.setTranslationX(translationX);
        this.mLogo.setTranslationY(translationY);
    }

    private void updateCircleRect() {
        updateCircleRect(this.mCircleRect, this.mOffset, false);
    }

    private void updateCircleRect(Rect rect, float offset, boolean useStaticSize) {
        int top;
        int left;
        float circleSize = useStaticSize ? (float) this.mCircleMinSize : this.mCircleSize;
        if (!isLand()) {
            left = ((int) (((float) getWidth()) - circleSize)) / 2;
            top = (int) (((((float) getHeight()) - (circleSize / 2.0f)) - ((float) this.mBaseMargin)) - offset);
        } else if (this.isLandScapeProduct.booleanValue()) {
            left = ((int) (((float) getWidth()) - circleSize)) / 2;
            top = (int) (((((float) getHeight()) - (circleSize / 2.0f)) - ((float) this.mBaseMargin)) - offset);
        } else {
            left = (int) (((((float) getWidth()) - (circleSize / 2.0f)) - ((float) this.mBaseMargin)) - offset);
            top = (int) ((((float) getHeight()) - circleSize) / 2.0f);
        }
        rect.set(left, top, (int) (((float) left) + circleSize), (int) (((float) top) + circleSize));
    }

    public void setHorizontal(boolean horizontal) {
        updateCircleRect(this.mStaticRect, (float) this.mStaticOffset, true);
        updateLayout();
    }

    public void setDragDistance(float distance) {
        if (this.mAnimatingOut) {
            return;
        }
        if (!this.mCircleHidden || this.mDraggedFarEnough) {
            setCircleSize(((float) this.mCircleMinSize) + rubberband(distance));
        }
    }

    private float rubberband(float diff) {
        return (float) Math.pow((double) Math.abs(diff), 0.6000000238418579d);
    }

    public void startAbortAnimation(Runnable endRunnable) {
        if (!this.mAnimatingOut) {
            setCircleSize(0.0f, true, null, 0, null);
            setOffset(0.0f, true, 0, null, endRunnable);
            animate().alpha(0.0f).setDuration(300).setStartDelay(0).setInterpolator(ALPHA_OUT).start();
            this.mCircleHidden = true;
        } else if (endRunnable != null) {
            endRunnable.run();
        }
    }

    public void startEnterAnimation() {
        if (!this.mAnimatingOut) {
            setAlpha(1.0f);
            applyCircleSize(0.0f);
            setOffset(0.0f);
            setCircleSize((float) this.mCircleMinSize, true, null, 50, null);
            setOffset((float) this.mStaticOffset, true, 50, null, null);
            this.mCircleHidden = false;
        }
    }

    public void startExitAnimation(Runnable endRunnable) {
        if (!isLand()) {
            setOffset((((float) getHeight()) / 2.0f) - ((float) this.mBaseMargin), true, 50, this.mFastOutSlowInInterpolator, null);
            setCircleSize((float) Math.ceil(Math.hypot((double) (((float) getWidth()) / 2.0f), (double) (((float) getHeight()) / 2.0f)) * 2.0d), true, null, 50, this.mFastOutSlowInInterpolator);
            performExitFadeOutAnimation(50, 300, endRunnable);
            return;
        }
        endRunnable.run();
    }

    private void performExitFadeOutAnimation(int startDelay, int duration, final Runnable endRunnable) {
        this.mFadeOutAnimator = ValueAnimator.ofFloat(((float) this.mBackgroundPaint.getAlpha()) / 255.0f, 0.0f);
        this.mFadeOutAnimator.setInterpolator(new LinearInterpolator());
        this.mFadeOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass6 */

            public void onAnimationUpdate(ValueAnimator animation) {
                float backgroundValue;
                float animatedFraction = animation.getAnimatedFraction();
                float logoValue = SearchPanelCircleView.ALPHA_OUT.getInterpolation(1.0f - (animatedFraction > 0.5f ? 1.0f : animatedFraction / 0.5f));
                if (animatedFraction < 0.2f) {
                    backgroundValue = 0.0f;
                } else {
                    backgroundValue = SearchPanelCircleView.ALPHA_OUT.getInterpolation((animatedFraction - 0.2f) / 0.8f);
                }
                float backgroundValue2 = 1.0f - backgroundValue;
                SearchPanelCircleView.this.mBackgroundPaint.setAlpha((int) (255.0f * backgroundValue2));
                float unused = SearchPanelCircleView.this.mOutlineAlpha = backgroundValue2;
                SearchPanelCircleView.this.mLogo.setAlpha(logoValue);
                SearchPanelCircleView.this.invalidateOutline();
                SearchPanelCircleView.this.invalidate();
            }
        });
        this.mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass7 */

            public void onAnimationEnd(Animator animation) {
                Runnable runnable = endRunnable;
                if (runnable != null) {
                    runnable.run();
                }
                SearchPanelCircleView.this.mLogo.setAlpha(1.0f);
                SearchPanelCircleView.this.mBackgroundPaint.setAlpha(255);
                float unused = SearchPanelCircleView.this.mOutlineAlpha = 1.0f;
                ValueAnimator unused2 = SearchPanelCircleView.this.mFadeOutAnimator = null;
            }
        });
        this.mFadeOutAnimator.setStartDelay((long) startDelay);
        this.mFadeOutAnimator.setDuration((long) duration);
        this.mFadeOutAnimator.start();
    }

    public void setDraggedFarEnough(boolean farEnough) {
        if (farEnough != this.mDraggedFarEnough) {
            if (farEnough) {
                if (this.mCircleHidden) {
                    startEnterAnimation();
                }
                if (this.mOffsetAnimator == null) {
                    addRipple();
                } else {
                    postDelayed(new Runnable() {
                        /* class com.android.server.policy.SearchPanelCircleView.AnonymousClass8 */

                        public void run() {
                            SearchPanelCircleView.this.addRipple();
                        }
                    }, 100);
                }
            } else {
                startAbortAnimation(null);
            }
            this.mDraggedFarEnough = farEnough;
        }
    }

    /* access modifiers changed from: private */
    public void addRipple() {
        float yInterpolation;
        float xInterpolation;
        if (this.mRipples.size() <= 1) {
            if (isLand()) {
                xInterpolation = 0.75f;
                yInterpolation = 0.5f;
            } else {
                xInterpolation = 0.5f;
                yInterpolation = 0.75f;
            }
            float circleCenterX = (((float) this.mStaticRect.left) * (1.0f - xInterpolation)) + (((float) this.mStaticRect.right) * xInterpolation);
            float circleCenterY = (((float) this.mStaticRect.top) * (1.0f - yInterpolation)) + (((float) this.mStaticRect.bottom) * yInterpolation);
            float circleSize = this.mCircleSize;
            int i = this.mCircleMinSize;
            if (circleSize <= ((float) i) * 1.25f) {
                circleSize = ((float) i) * 1.25f;
            }
            new Ripple(circleCenterX, circleCenterY, 0.75f * circleSize).start();
        }
    }

    public void reset() {
        this.mDraggedFarEnough = false;
        this.mAnimatingOut = false;
        this.mCircleHidden = true;
        this.mClipToOutline = false;
        ValueAnimator valueAnimator = this.mFadeOutAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        this.mBackgroundPaint.setAlpha(255);
        this.mOutlineAlpha = 1.0f;
    }

    public boolean isAnimationRunning(boolean enterAnimation) {
        return this.mOffsetAnimator != null && enterAnimation == this.mOffsetAnimatingIn;
    }

    public void performOnAnimationFinished(Runnable runnable) {
        ValueAnimator valueAnimator = this.mOffsetAnimator;
        if (valueAnimator != null) {
            valueAnimator.addListener(new MyAnimatorListenerAdapter(runnable));
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void setAnimatingOut(boolean animatingOut) {
        this.mAnimatingOut = animatingOut;
    }

    public boolean isAnimatingOut() {
        return this.mAnimatingOut;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: private */
    public class Ripple {
        float alpha;
        float endRadius;
        float radius;
        float x;
        float y;

        Ripple(float x2, float y2, float endRadius2) {
            this.x = x2;
            this.y = y2;
            this.endRadius = endRadius2;
        }

        /* access modifiers changed from: package-private */
        public void start() {
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.server.policy.SearchPanelCircleView.Ripple.AnonymousClass1 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    Ripple.this.alpha = 1.0f - animation.getAnimatedFraction();
                    Ripple ripple = Ripple.this;
                    ripple.alpha = SearchPanelCircleView.this.mDisappearInterpolator.getInterpolation(Ripple.this.alpha);
                    Ripple ripple2 = Ripple.this;
                    ripple2.radius = SearchPanelCircleView.this.mAppearInterpolator.getInterpolation(animation.getAnimatedFraction());
                    Ripple.this.radius *= Ripple.this.endRadius;
                    SearchPanelCircleView.this.invalidate();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.server.policy.SearchPanelCircleView.Ripple.AnonymousClass2 */

                public void onAnimationEnd(Animator animation) {
                    SearchPanelCircleView.this.mRipples.remove(Ripple.this);
                    SearchPanelCircleView.this.updateClipping();
                }

                public void onAnimationStart(Animator animation) {
                    SearchPanelCircleView.this.mRipples.add(Ripple.this);
                    SearchPanelCircleView.this.updateClipping();
                }
            });
            animator.setDuration(400L);
            animator.start();
        }

        public void draw(Canvas canvas) {
            SearchPanelCircleView.this.mRipplePaint.setAlpha((int) (this.alpha * 255.0f));
            canvas.drawCircle(this.x, this.y, this.radius, SearchPanelCircleView.this.mRipplePaint);
        }
    }

    public boolean isLand() {
        return 2 == getResources().getConfiguration().orientation;
    }
}
