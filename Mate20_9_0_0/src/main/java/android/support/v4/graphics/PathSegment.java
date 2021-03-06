package android.support.v4.graphics;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.util.Preconditions;

public final class PathSegment {
    private final PointF mEnd;
    private final float mEndFraction;
    private final PointF mStart;
    private final float mStartFraction;

    public PathSegment(@NonNull PointF start, float startFraction, @NonNull PointF end, float endFraction) {
        this.mStart = (PointF) Preconditions.checkNotNull(start, "start == null");
        this.mStartFraction = startFraction;
        this.mEnd = (PointF) Preconditions.checkNotNull(end, "end == null");
        this.mEndFraction = endFraction;
    }

    @NonNull
    public PointF getStart() {
        return this.mStart;
    }

    public float getStartFraction() {
        return this.mStartFraction;
    }

    @NonNull
    public PointF getEnd() {
        return this.mEnd;
    }

    public float getEndFraction() {
        return this.mEndFraction;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (!(o instanceof PathSegment)) {
            return false;
        }
        PathSegment that = (PathSegment) o;
        if (!(Float.compare(this.mStartFraction, that.mStartFraction) == 0 && Float.compare(this.mEndFraction, that.mEndFraction) == 0 && this.mStart.equals(that.mStart) && this.mEnd.equals(that.mEnd))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * ((31 * this.mStart.hashCode()) + (this.mStartFraction != 0.0f ? Float.floatToIntBits(this.mStartFraction) : 0))) + this.mEnd.hashCode());
        if (this.mEndFraction != 0.0f) {
            i = Float.floatToIntBits(this.mEndFraction);
        }
        return result + i;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PathSegment{start=");
        stringBuilder.append(this.mStart);
        stringBuilder.append(", startFraction=");
        stringBuilder.append(this.mStartFraction);
        stringBuilder.append(", end=");
        stringBuilder.append(this.mEnd);
        stringBuilder.append(", endFraction=");
        stringBuilder.append(this.mEndFraction);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
