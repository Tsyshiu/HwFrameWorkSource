package com.huawei.hwtransition.algorithm;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import com.huawei.hwtransition.AlgorithmUtil;
import com.huawei.hwtransition.algorithm.BaseTransition;

public class RotateTransition extends BaseTransition {
    private static final float CAMERA_DISTANCE_FACTOR = 2.67f;
    private static final int PAGE_ANGLE = 180;

    public RotateTransition() {
        this.mAnimationType = "3D";
        if (this.mTransformationInfo.mCamera == null) {
            this.mTransformationInfo.mCamera = new Camera();
            this.mTransformationInfo.matrix3D = new Matrix();
        }
    }

    @Override // com.huawei.hwtransition.algorithm.BaseTransition
    public boolean transform(int part, boolean isOverScrollFirst, boolean isOverScrollLast, float scrollProgress, View child) {
        if (Math.abs(scrollProgress) > 0.5f) {
            return false;
        }
        int cw = child.getWidth();
        int ch = child.getHeight();
        this.mTransformationInfo.mCamera.setLocation(0.0f, 0.0f, ((((float) (-child.getMeasuredHeight())) * CAMERA_DISTANCE_FACTOR) * child.getResources().getDisplayMetrics().density) / ((float) child.getResources().getDisplayMetrics().densityDpi));
        TransformationInfo transformationInfo = this.mTransformationInfo;
        transformationInfo.mPivotX = AlgorithmUtil.transformPivotX(child, ((float) cw) / 2.0f);
        TransformationInfo transformationInfo2 = this.mTransformationInfo;
        transformationInfo2.mPivotY = AlgorithmUtil.transformPivotY(child, ((float) ch) / 2.0f);
        this.mTransformationInfo.mRotationY = 180.0f * scrollProgress * -1.0f;
        if (this.mLayoutType == 0 && !isOverScrollFirst && !isOverScrollLast) {
            this.mTransformationInfo.mTranslationX = ((float) cw) * scrollProgress * child.getScaleX();
        }
        this.mTransformationInfo.mIsMatrixDirty = true;
        return true;
    }
}
