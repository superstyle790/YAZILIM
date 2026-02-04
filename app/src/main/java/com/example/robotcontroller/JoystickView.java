package com.example.robotcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class JoystickView extends View {
    public interface JoystickListener {
        void onDirection(char command);
        void onStop();
    }

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerX;
    private float centerY;
    private float baseRadius;
    private float knobRadius;
    private float knobX;
    private float knobY;
    private JoystickListener listener;

    public JoystickView(Context context) {
        super(context);
        init();
    }

    public JoystickView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        basePaint.setColor(Color.parseColor("#E0E0E0"));
        knobPaint.setColor(Color.parseColor("#455A64"));
    }

    public void setListener(JoystickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) * 0.4f;
        knobRadius = baseRadius * 0.4f;
        resetKnob();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (distance > baseRadius) {
                    dx = dx / distance * baseRadius;
                    dy = dy / distance * baseRadius;
                }
                knobX = centerX + dx;
                knobY = centerY + dy;
                invalidate();
                dispatchDirection(dx / baseRadius, dy / baseRadius);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetKnob();
                invalidate();
                if (listener != null) {
                    listener.onStop();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void dispatchDirection(float normX, float normY) {
        if (listener == null) {
            return;
        }
        float absX = Math.abs(normX);
        float absY = Math.abs(normY);
        float threshold = 0.35f;
        if (absX < threshold && absY < threshold) {
            return;
        }
        if (absX > absY) {
            listener.onDirection(normX > 0 ? 'R' : 'L');
        } else {
            listener.onDirection(normY > 0 ? 'B' : 'F');
        }
    }

    private void resetKnob() {
        knobX = centerX;
        knobY = centerY;
    }
}
