package com.smart.hgnu;

import static com.smart.hgnu.UiComponents.GREEN;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

/** Decorative drawing with a fixed design coordinate system. */
final class CampusIllustrationView extends View {
    Paint paint = new Paint(3);

    CampusIllustrationView(android.content.Context context) {
        super(context);
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(0xffe9eee2);
        background.setCornerRadius(24 * getResources().getDisplayMetrics().density);
        setBackground(background);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth() / 340f, getHeight() / 170f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffd6dfcb);
        canvas.drawCircle(285, 42, 28, paint);
        paint.setColor(0xffc9d5be);
        canvas.drawRoundRect(23, 115, 317, 141, 12, 12, paint);
        paint.setColor(0xffa3b99b);
        canvas.drawRect(62, 74, 278, 128, paint);
        paint.setColor(0xfff7f5e9);
        canvas.drawRect(119, 58, 221, 132, paint);
        Path roof = new Path();
        roof.moveTo(105, 58);
        roof.lineTo(170, 24);
        roof.lineTo(235, 58);
        roof.close();
        paint.setColor(GREEN);
        canvas.drawPath(roof, paint);
        paint.setColor(0xff1f7157);
        for (int index = 0; index < 4; index++)
            canvas.drawRect(131 + index * 22, 72, 141 + index * 22, 127, paint);
        paint.setColor(0xff6b906b);
        for (int index = 0; index < 3; index++) {
            canvas.drawRect(73 + index * 13, 87, 79 + index * 13, 101, paint);
            canvas.drawRect(236 + index * 13, 87, 242 + index * 13, 101, paint);
        }
        paint.setColor(0xffedf1e7);
        canvas.drawRect(112, 128, 228, 133, paint);
        canvas.drawRect(102, 135, 238, 140, paint);
        paint.setColor(0xff376d50);
        canvas.drawOval(24, 61, 61, 118, paint);
        canvas.drawOval(287, 71, 318, 122, paint);
        paint.setStrokeWidth(3);
        canvas.drawLine(43, 105, 43, 135, paint);
        canvas.drawLine(302, 111, 302, 137, paint);
        paint.setColor(0xff7c9572);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        canvas.drawText("HUANGGANG NORMAL UNIVERSITY", 90, 158, paint);
        canvas.restore();
    }
}
