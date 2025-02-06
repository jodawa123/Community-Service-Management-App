package com.example.cs3700;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import android.animation.ValueAnimator;
import android.graphics.Color;

public class CurvedPathView extends View {
    private int currentWeek = 0;
    private ValueAnimator glowAnimator;
    private int glowColor = Color.parseColor("#F44336"); // Default red color

    public CurvedPathView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupGlowAnimation();
    }
    public void setWeekProgress(int weekProgress) {
        this.currentWeek = weekProgress;
        invalidate(); // Redraw the view when progress changes
    }

    private void setupGlowAnimation() {
        glowAnimator = ValueAnimator.ofFloat(0, 1);
        glowAnimator.setDuration(1000); // 1 second animation
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repeat indefinitely
        glowAnimator.setRepeatMode(ValueAnimator.REVERSE); // Reverse the animation
        glowAnimator.addUpdateListener(animation -> {
            // Update the glow color based on the animation progress
            float fraction = animation.getAnimatedFraction();
            int alpha = (int) (255 * (0.5f + 0.5f * fraction)); // Vary alpha between 128 and 255
            glowColor = Color.argb(alpha, 244, 67, 54); // Red with varying alpha
            invalidate(); // Redraw the view
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int totalWeeks = 10; // Total milestones
        int circleRadius = 65; // Size of milestone circles
        int trailWidth = 15; // Width of the trail

        // Paints for trail and milestones
        Paint trailPaint = new Paint();
        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeWidth(trailWidth);
        trailPaint.setAntiAlias(true);

        Paint circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);

        int startX = getWidth() / 2; // Center of the screen horizontally
        int startY = 150; // Starting vertical position
        int curveOffset = getWidth() / 2 - circleRadius; // Curve amplitude to fill the width

        Path trailPath = new Path();
        trailPath.moveTo(startX, startY);

        for (int i = 1; i <= totalWeeks; i++) {
            // Alternate curve direction for a wavy trail
            int controlX = (i % 2 == 0) ? startX + curveOffset : startX - curveOffset;
            int nextY = startY + 350; // Spacing between milestones

            // Create a continuous curved path
            trailPath.quadTo(controlX, (startY + nextY) / 2, startX, nextY);

            // Update colors based on progress
            if (i < currentWeek) {
                trailPaint.setColor(Color.parseColor("#FFD700")); // yellow for passed weeks
                circlePaint.setColor(Color.parseColor("#FFD700"));
            } else if (i == currentWeek) {
                trailPaint.setColor(glowColor); // Use the animated glow color
                circlePaint.setColor(glowColor);
                if (glowAnimator != null && !glowAnimator.isStarted()) {
                    glowAnimator.start(); // Start the glow animation
                }
            } else {
                trailPaint.setColor(Color.parseColor("#BDBDBD")); // Gray for upcoming weeks
                circlePaint.setColor(Color.parseColor("#BDBDBD"));
            }

            // Draw the segment of the trail
            canvas.drawPath(trailPath, trailPaint);

            // Draw milestone circle
            canvas.drawCircle(startX, nextY, circleRadius, circlePaint);

            // Draw milestone name
            String milestoneName = "Week " + i;
            canvas.drawText(milestoneName, startX - circleRadius, nextY - circleRadius - 20, textPaint);

            startY = nextY; // Move to the next milestone
            trailPath.reset(); // Reset the path for the next segment
            trailPath.moveTo(startX, startY); // Move to the new start position
        }
    }
}