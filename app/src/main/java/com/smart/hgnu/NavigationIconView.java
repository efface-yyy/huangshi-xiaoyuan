package com.smart.hgnu;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Decorative drawing with a fixed design coordinate system. */
final class NavigationIconView extends View {
    private final int iconIndex, color;
    private final Paint paint = new Paint(3);

    NavigationIconView(android.content.Context context, int iconIndex, int color) {
        super(context);
        this.iconIndex = iconIndex;
        this.color = color;
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth() / 24f, getHeight() / 24f);
        paint.setColor(color);
        paint.setStrokeWidth(1.7f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (iconIndex == 0) {
            Path path = new Path();
            path.moveTo(3, 10);
            path.lineTo(12, 3);
            path.lineTo(21, 10);
            canvas.drawPath(path, paint);
            canvas.drawRect(5, 10, 19, 21, paint);
            canvas.drawRect(10, 14, 14, 21, paint);
        } else if (iconIndex == 1) {
            canvas.drawRoundRect(4, 5, 20, 21, 3, 3, paint);
            canvas.drawLine(4, 10, 20, 10, paint);
            canvas.drawLine(8, 3, 8, 7, paint);
            canvas.drawLine(16, 3, 16, 7, paint);
            canvas.drawLine(8, 14, 11, 14, paint);
            canvas.drawLine(14, 14, 17, 14, paint);
            canvas.drawLine(8, 18, 11, 18, paint);
        } else if (iconIndex == 2) {
            for (int row = 0; row < 2; row++)
                for (int column = 0; column < 2; column++)
                    canvas.drawRoundRect(
                            4 + column * 10,
                            4 + row * 10,
                            10 + column * 10,
                            10 + row * 10,
                            1.5f,
                            1.5f,
                            paint);
        } else {
            canvas.drawCircle(12, 7, 4, paint);
            canvas.drawArc(4, 12, 20, 26, 190, 160, false, paint);
        }
        canvas.restore();
    }
}
