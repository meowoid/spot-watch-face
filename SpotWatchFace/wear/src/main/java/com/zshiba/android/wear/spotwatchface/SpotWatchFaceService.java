package com.zshiba.android.wear.spotwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class SpotWatchFaceService extends CanvasWatchFaceService{

  private static final String TAG = "SpotWatchFaceService";

  @Override
  public Engine onCreateEngine(){
    return new Engine();
  }


  private class Engine extends CanvasWatchFaceService.Engine{

    private static final int MESSAGE_UPDATE_TIME = 0;

    private boolean isInLowBitAmbientMode;
    private boolean isInBurnInProtectionMode;

    private Paint hourSpotPaint;
    private Paint minuteSpotPaint;
    private Paint secondSpotPaint;
    private Paint dayPaint;
    private Paint dayFramePaint;
    private Paint hourPaint;
    private Paint minutePaint;
    private Bitmap backgroundBitmap;
    private Bitmap backgroundScaledBitmap;

    private Time time;
    private long updateTimeRateInMillisec;
    private Handler updateTimeHandler;
    private BroadcastReceiver timeZoneBroadcastReceiver;
    private boolean isTimeZoneBroadcastReceiverRegistered;

    public Engine(){
      this.isInLowBitAmbientMode = false;
      this.isInBurnInProtectionMode = false;
      this.hourSpotPaint = null;
      this.minuteSpotPaint = null;
      this.secondSpotPaint = null;
      this.dayPaint = null;
      this.dayFramePaint = null;
      this.hourPaint = null;
      this.minutePaint = null;
      this.backgroundBitmap = null;
      this.backgroundScaledBitmap = null;
      this.time = new Time();

      this.updateTimeRateInMillisec = TimeUnit.SECONDS.toMillis(1);
      this.updateTimeHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
          switch(message.what){
            case MESSAGE_UPDATE_TIME:
              if(Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "updating time");
              invalidate();
              if(shouldTick()){
                long delay = updateTimeRateInMillisec - (System.currentTimeMillis() % updateTimeRateInMillisec);
                updateTimeHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_TIME, delay);
              }
              break;
            default:
              break;
          }
        }
      };
      this.timeZoneBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
          time.clear(intent.getStringExtra("time-zone"));
          time.setToNow();
        }
      };
      this.isTimeZoneBroadcastReceiverRegistered = false;
    }

    private boolean shouldTick(){
      if(this.isVisible() && !this.isInAmbientMode())
        return true;
      else
        return false;
    }

    @Override
    public void onCreate(SurfaceHolder holder){
      if(Log.isLoggable(TAG, Log.DEBUG))
        Log.d(TAG, "onCreate");
      super.onCreate(holder);

      this.setWatchFaceStyle(new WatchFaceStyle.Builder(SpotWatchFaceService.this)
          .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
          .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
          .setShowSystemUiTime(false)
          .build());

      this.hourSpotPaint = new Paint();
      this.hourSpotPaint.setARGB(200, 0, 0, 255);
      this.hourSpotPaint.setAntiAlias(true);

      this.minuteSpotPaint = new Paint();
      this.minuteSpotPaint.setARGB(200, 0, 255, 0);
      this.minuteSpotPaint.setAntiAlias(true);

      this.secondSpotPaint = new Paint();
      this.secondSpotPaint.setARGB(200, 255, 0, 0);
      this.secondSpotPaint.setAntiAlias(true);

      this.dayPaint = new Paint();
      this.dayPaint.setARGB(100, 255, 255, 255);
      this.dayPaint.setTextSize(18.0f);
      this.dayPaint.setTypeface(Typeface.create(this.dayPaint.getTypeface(), Typeface.BOLD));
      this.dayPaint.setAntiAlias(true);
      this.dayPaint.setTextAlign(Paint.Align.CENTER);

      this.dayFramePaint = new Paint();
      this.dayFramePaint.setARGB(100, 255, 255, 255);
      this.dayFramePaint.setStrokeWidth(2.0f);
      this.dayFramePaint.setAntiAlias(true);
      this.dayFramePaint.setStyle(Paint.Style.STROKE);

      this.hourPaint = new Paint();
      this.hourPaint.setARGB(255, 255, 255, 255);
      this.hourPaint.setTextSize(30.0f);
      this.hourPaint.setAntiAlias(true);
      this.hourPaint.setTextAlign(Paint.Align.CENTER);

      this.minutePaint = new Paint();
      this.minutePaint.setARGB(255, 255, 255, 255);
      this.minutePaint.setTextSize(24.0f);
      this.minutePaint.setAntiAlias(true);
      this.minutePaint.setTextAlign(Paint.Align.CENTER);

      Resources resources = SpotWatchFaceService.this.getResources();
      Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
      this.backgroundBitmap = ((BitmapDrawable)backgroundDrawable).getBitmap();

      this.time.setToNow();
    }

    @Override
    public void onDestroy(){
      this.stopTicking();
      this.unregisterTimeZoneBroadcastReceiver();
      super.onDestroy();
    }

    @Override
    public void onPropertiesChanged(Bundle properties){
      super.onPropertiesChanged(properties);
      this.isInLowBitAmbientMode = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
      this.isInBurnInProtectionMode = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
      if(Log.isLoggable(TAG, Log.DEBUG)){
        Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + this.isInLowBitAmbientMode);
        Log.d(TAG, "onPropertiesChanged: burn-in protection = " + this.isInBurnInProtectionMode);
      }
      boolean b = !this.isInLowBitAmbientMode;
      this.hourSpotPaint.setAntiAlias(b);
      this.minuteSpotPaint.setAntiAlias(b);
      this.secondSpotPaint.setAntiAlias(b);
      this.dayPaint.setAntiAlias(b);
      this.dayFramePaint.setAntiAlias(b);
      this.hourPaint.setAntiAlias(b);
      this.minutePaint.setAntiAlias(b);
    }

    @Override
    public void onTimeTick(){
      super.onTimeTick();
      if(Log.isLoggable(TAG, Log.DEBUG))
        Log.d(TAG, "onTimeTick: ambient = " + this.isInAmbientMode());

      this.invalidate();
    }

    @Override
    public void onAmbientModeChanged(boolean inAmbientMode){
      super.onAmbientModeChanged(inAmbientMode);
      if (Log.isLoggable(TAG, Log.DEBUG))
        Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

      this.invalidate();
      if(this.shouldTick())
        this.startTicking();
      else
        this.stopTicking();
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds){
      super.onDraw(canvas, bounds);
      this.time.setToNow();

      int width = bounds.width();
      int height = bounds.height();

      if(this.backgroundScaledBitmap == null || this.backgroundScaledBitmap.getWidth() != width || this.backgroundScaledBitmap.getHeight() != height)
        this.backgroundScaledBitmap = Bitmap.createScaledBitmap(this.backgroundBitmap, width, height, true);

      canvas.drawBitmap(this.backgroundScaledBitmap, 0, 0, null);

      float centerX = width / 2.0f;
      float centerY = height / 2.0f;

      float secRadian = this.time.second / 30.0f * (float)Math.PI - (float)Math.PI / 2.0f;
      float minRadian = this.time.minute / 30.0f * (float)Math.PI - (float)Math.PI / 2.0f;
      float hourRadian = ((this.time.hour + (this.time.minute / 60.0f)) / 6.0f ) * (float)Math.PI - (float)Math.PI / 2.0f;

      float r = Math.min(width, height) / 2.0f;
      float secLength = r - 20.0f;
      float minLength = r - 46.0f;
      float hourLength = r - 80.0f;

      float hourX = (float)Math.cos(hourRadian) * hourLength;
      float hourY = (float)Math.sin(hourRadian) * hourLength;
      float hourTextHeight = (this.hourPaint.descent() + this.hourPaint.ascent()) / 2.0f;
      canvas.drawCircle(centerX + hourX, centerY + hourY, 30.0f, this.hourSpotPaint);
      canvas.drawText(String.valueOf(this.time.hour), centerX + hourX, centerY + hourY - hourTextHeight, this.hourPaint);

      float minX = (float)Math.cos(minRadian) * minLength;
      float minY = (float)Math.sin(minRadian) * minLength;
      float minTextHeight = (this.minutePaint.descent() + this.minutePaint.ascent()) / 2.0f;
      canvas.drawCircle(centerX + minX, centerY + minY, 20.0f, this.minuteSpotPaint);
      canvas.drawText(String.valueOf(this.time.minute), centerX + minX, centerY + minY - minTextHeight, this.minutePaint);

      if(!this.isInAmbientMode() && !this.isInBurnInProtectionMode){
        float secX = (float)Math.cos(secRadian) * secLength;
        float secY = (float)Math.sin(secRadian) * secLength;
        canvas.drawCircle(centerX + secX, centerY + secY, 10.0f, this.secondSpotPaint);
      }

      String dayAndWeekDay = this.time.monthDay + "," + this.getWeekDayOf(this.time.weekDay);
      float dayTextHeight = (this.dayPaint.descent() + this.dayPaint.ascent()) / 2.0f;
      canvas.drawText(dayAndWeekDay, centerX, centerY - dayTextHeight, this.dayPaint);
      Rect b = new Rect();
      this.dayPaint.getTextBounds(dayAndWeekDay, 0, dayAndWeekDay.length(), b);
      b.offset((int)(centerX - b.width() / 2.0f), (int)(centerY + b.height() / 2.0f));
      canvas.drawRoundRect(new RectF(b.left - 4.0f, b.top - 6.0f, b.right + 4.0f, b.bottom + 2.0f), 3.0f, 3.0f, this.dayFramePaint);
    }

    private String getWeekDayOf(int weekDay){
      String weekDayString;
      switch(weekDay){
        case Time.SUNDAY:
          weekDayString = "Sun";
          break;
        case Time.MONDAY:
          weekDayString = "Mon";
          break;
        case Time.TUESDAY:
          weekDayString = "Tue";
          break;
        case Time.WEDNESDAY:
          weekDayString = "Wed";
          break;
        case Time.THURSDAY:
          weekDayString = "Thu";
          break;
        case Time.FRIDAY:
          weekDayString = "Fri";
          break;
        case Time.SATURDAY:
          weekDayString = "Sat";
          break;
        default:
          weekDayString = null;
          break;
      }
      return weekDayString;
    }

    @Override
    public void onVisibilityChanged(boolean visible){
      super.onVisibilityChanged(visible);
      if(Log.isLoggable(TAG, Log.DEBUG))
        Log.d(TAG, "onVisibilityChanged: " + visible);

      if(visible){
        this.registerTimeZoneBroadcastReceiver();
        this.time.clear(TimeZone.getDefault().getID());
        this.time.setToNow();
      }else{
        this.unregisterTimeZoneBroadcastReceiver();
      }
      if(this.shouldTick())
        this.startTicking();
      else
        this.stopTicking();
    }

    private void registerTimeZoneBroadcastReceiver(){
      if(!this.isTimeZoneBroadcastReceiverRegistered){
        this.isTimeZoneBroadcastReceiverRegistered = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
        SpotWatchFaceService.this.registerReceiver(this.timeZoneBroadcastReceiver, filter);
      }
    }
    private void unregisterTimeZoneBroadcastReceiver(){
      if(this.isTimeZoneBroadcastReceiverRegistered){
        this.isTimeZoneBroadcastReceiverRegistered = false;
        SpotWatchFaceService.this.unregisterReceiver(this.timeZoneBroadcastReceiver);
      }
    }

    private void startTicking(){
      if(Log.isLoggable(TAG, Log.DEBUG))
        Log.d(TAG, "startTicking");

      this.updateTimeHandler.removeMessages(MESSAGE_UPDATE_TIME);
      this.updateTimeHandler.sendEmptyMessage(MESSAGE_UPDATE_TIME);
    }
    private void stopTicking(){
      if(Log.isLoggable(TAG, Log.DEBUG))
        Log.d(TAG, "stopTicking");

      this.updateTimeHandler.removeMessages(MESSAGE_UPDATE_TIME);
    }

  }

}
