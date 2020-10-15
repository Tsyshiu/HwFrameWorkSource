package com.huawei.hwtransition.anim;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;

public class HintErrorEffect {
    private static final String OUTILNE_PATH_FIELD_NAME = "mPath";
    private static final int OUTLINE_COLOR = -322791;
    private static final String OUTLINE_RADIUS_FIELD_NAME = "mRadius";
    private static final String OUTLINE_RECT_FIELD_NAME = "mRect";
    private static final int OUTLINE_SHADOW_RADIUS = 16;
    private static final int OUTLINE_SHADOW_STROKE_WIDTH = 1;
    private static final int OUTLINE_START_ALPHA = 127;
    private static final String TAG = "HintErrorEffect";
    private boolean isShowOutline;
    private ArrayList<Float> mAlphaListFactors;
    private Path mEffectPath;
    private RectF mEffectPathBounds;
    private Matrix mEffectPathMatrix;
    private Outline mOutline;
    private Paint mPaint;
    private Path mPath;
    private RectF mPathBounds;
    private float mRadius;
    private Rect mRect;
    private View mView;
    private ViewOutlineProvider mVop;

    public HintErrorEffect(View view) {
        if (view == null) {
            Log.w(TAG, "TargetView is null");
        }
        this.mPathBounds = new RectF();
        this.mPaint = new Paint(1);
        this.mAlphaListFactors = new ArrayList<>(0);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(1.0f);
        this.mPaint.setColor(OUTLINE_COLOR);
        this.mPaint.setAlpha(OUTLINE_START_ALPHA);
        this.mPaint.setDither(true);
        this.mPaint.setStrokeJoin(Paint.Join.ROUND);
        this.mEffectPath = new Path();
        this.mEffectPathBounds = new RectF();
        this.mEffectPathMatrix = new Matrix();
        computeAlphaList();
        this.mView = view;
        this.mOutline = new Outline();
    }

    public void getOutlinePath(Outline outline) {
        try {
            final Field pathField = outline.getClass().getField(OUTILNE_PATH_FIELD_NAME);
            final Field rectField = outline.getClass().getField(OUTLINE_RECT_FIELD_NAME);
            final Field radiusField = outline.getClass().getField(OUTLINE_RADIUS_FIELD_NAME);
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                /* class com.huawei.hwtransition.anim.HintErrorEffect.AnonymousClass1 */

                @Override // java.security.PrivilegedAction
                public Object run() {
                    pathField.setAccessible(true);
                    rectField.setAccessible(true);
                    radiusField.setAccessible(true);
                    return null;
                }
            });
            this.mPath = (Path) pathField.get(outline);
            this.mRect = (Rect) rectField.get(outline);
            this.mRadius = ((Float) radiusField.get(outline)).floatValue();
        } catch (NoSuchFieldException e) {
            Log.w(TAG, "Wrong reflection. no such filed in Outline class");
        } catch (IllegalAccessException e2) {
            Log.w(TAG, "Wrong reflection. Illegal access the Outline class");
        }
    }

    public void drawErrorEffect(Canvas canvas) {
        if (this.isShowOutline) {
            if (getPath() == null && getRect() == null) {
                Log.w(TAG, "drawErrorEffect outline is null " + getPath());
            } else if (this.mRect != null && !this.mRect.isEmpty()) {
                int delta = 0;
                Iterator<Float> it = this.mAlphaListFactors.iterator();
                while (it.hasNext()) {
                    this.mPaint.setAlpha((int) (it.next().floatValue() * 127.0f));
                    canvas.drawRoundRect((float) (this.mRect.left + delta), (float) (this.mRect.top + delta), (float) (this.mRect.right - delta), (float) (this.mRect.bottom - delta), this.mRadius, this.mRadius, this.mPaint);
                    delta++;
                }
            } else if (this.mPath != null) {
                int delta2 = 0;
                this.mPath.computeBounds(this.mPathBounds, true);
                float oldWidth = this.mPathBounds.width();
                float oldHeight = this.mPathBounds.height();
                Iterator<Float> it2 = this.mAlphaListFactors.iterator();
                while (it2.hasNext()) {
                    this.mPaint.setAlpha((int) (it2.next().floatValue() * 127.0f));
                    this.mEffectPathMatrix.setScale(1.0f - (((float) delta2) / oldWidth), 1.0f - (((float) delta2) / oldHeight));
                    this.mEffectPath.set(this.mPath);
                    this.mEffectPath.transform(this.mEffectPathMatrix);
                    this.mEffectPath.computeBounds(this.mEffectPathBounds, true);
                    float currentWidth = this.mEffectPathBounds.width();
                    float currentHeight = this.mEffectPathBounds.height();
                    canvas.save();
                    canvas.translate((oldWidth / 2.0f) - (currentWidth / 2.0f), (oldHeight / 2.0f) - (currentHeight / 2.0f));
                    canvas.drawPath(this.mEffectPath, this.mPaint);
                    canvas.restore();
                    delta2 = (int) (((float) delta2) + 2.0f);
                }
            }
        }
    }

    private void computeAlphaList() {
        float alpha;
        this.mAlphaListFactors.clear();
        int index = 0;
        int currentAlpha = OUTLINE_START_ALPHA;
        while (currentAlpha > 0) {
            float currentAlphaFactor = 0.0f;
            for (int j = -8; j < 8; j++) {
                if (index + j < 0) {
                    alpha = 1.0f;
                } else if (index + j >= this.mAlphaListFactors.size()) {
                    alpha = 0.0f;
                } else {
                    alpha = this.mAlphaListFactors.get(index + j).floatValue();
                }
                currentAlphaFactor += alpha;
            }
            float currentAlphaFactor2 = currentAlphaFactor / 16.0f;
            this.mAlphaListFactors.add(Float.valueOf(currentAlphaFactor2));
            currentAlpha = (int) (127.0f * currentAlphaFactor2);
            index++;
        }
    }

    public Path getPath() {
        return this.mPath;
    }

    public Rect getRect() {
        return this.mRect;
    }

    public void showErrEffect(boolean isShow) {
        if (this.mView != null && isShow) {
            this.mVop = this.mView.getOutlineProvider();
            this.mVop.getOutline(this.mView, this.mOutline);
            getOutlinePath(this.mOutline);
        }
        this.isShowOutline = isShow;
    }
}
