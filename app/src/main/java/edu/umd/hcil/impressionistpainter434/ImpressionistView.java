package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private VelocityTracker velocityTracker = VelocityTracker.obtain();

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     *
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle) {

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if (bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     *
     * @param imageView
     */
    public void setImageView(ImageView imageView) {
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     *
     * @param brushType
     */
    public void setBrushType(BrushType brushType) {
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting() {
        //if the canvas isn't null, paints a white rectangle over the existing bitmap
        if (_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    /**
     * Randomly adds 1000 points to the bitmap using randHelper method
     */
    public void randomAuto() {

        Bitmap imageViewBitmap = _imageView.getDrawingCache();
        //if the bitmap is null, don't do anything
        if (imageViewBitmap == null) {
            return;
        }
        for(int i = 0; i < 1000; i++){
            randHelper();
        }
        invalidate();
    }

    /**
     * Private helper method that randomly adds 1 point to the canvas
     */
    private void randHelper(){
        Bitmap imageViewBitmap = _imageView.getDrawingCache();
        Random rand = new Random();
        float touchX = rand.nextInt(imageViewBitmap.getWidth());
        float touchY = rand.nextInt(imageViewBitmap.getHeight());
        int x = (int) touchX;
        int y = (int) touchY;

        int[] viewCoor = new int[2];
        getLocationOnScreen(viewCoor);
        //shouldn't be out of bounds, but just in case make sure it is
        if (getBitmapPositionInsideImageView(_imageView).contains(x, y)) {
            //randomly generate points and brush size
            float dx = rand.nextFloat()*(2);
            //randomly decide if dx is negative
            if(rand.nextBoolean()){
                dx *= -1;
            }
            float dy = rand.nextFloat()*2;
            //randomly decide if dy is negative
            if(rand.nextBoolean()){
                dy *= -1;
            }

            //the range the brush can be is 10 to 50px to prevent it from being obnoxiously big or miniscule
            float width = (_paint.getStrokeWidth() + 10) * (float) Math.hypot(dx, dy) < 50 ? (_paint.getStrokeWidth() + 10) * (float) Math.hypot(dx, dy) : 50;
            width = width < 10 ? 10 : width;
            //get color of corresponding pixel in image
            int color = imageViewBitmap.getPixel(x, y);
            _paint.setColor(color);

            //logic of drawing based on brush type
            if (_brushType == BrushType.Square) {
                _offScreenCanvas.drawRect(touchX - width, touchY - width, touchX + width, touchY + width, _paint);
            } else if (_brushType == BrushType.Circle) {
                _offScreenCanvas.drawCircle(touchX, touchY, width, _paint);
            } else if (_brushType == BrushType.Line) {
                if (dx < 0) {
                    if (dy < 0) {
                        _offScreenCanvas.drawLine(touchX - width, touchY + width, touchX + width, touchY - width, _paint);
                    } else {
                        _offScreenCanvas.drawLine(touchX - width, touchY - width, touchX + width, touchY + width, _paint);
                    }
                } else {
                    if (dy < 0) {
                        _offScreenCanvas.drawLine(touchX + width, touchY + width, touchX - width, touchY - width, _paint);
                    } else {
                        _offScreenCanvas.drawLine(touchX + width, touchY - width, touchX - width, touchY + width, _paint);
                    }
                }
            }
        }
    }

    /**
     * Based on brush type, draws shapes to screen
     * @param motionEvent
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();
        int x = (int) touchX;
        int y = (int) touchY;

        Bitmap imageViewBitmap = _imageView.getDrawingCache();

        //if the bit map is null, don't do anything
        if (imageViewBitmap == null) {
            return true;
        }

        int[] viewCoor = new int[2];
        getLocationOnScreen(viewCoor);

        //if the touch coordinates are inside of the ImpressionistView then do stuff
        if (getBitmapPositionInsideImageView(_imageView).contains(x, y)) {

            //add the point to the velocity tracker
            velocityTracker.addMovement(motionEvent);
            //compute speed
            velocityTracker.computeCurrentVelocity(1, Float.MAX_VALUE);
            float dx = velocityTracker.getXVelocity();
            float dy = velocityTracker.getYVelocity();

            //range of brush size is 10-50px
            float width = (_paint.getStrokeWidth() + 10) * (float) Math.hypot(dx, dy) <= 50 ? (_paint.getStrokeWidth() + 10) * (float) Math.hypot(dx, dy) : 50;
            width = width < 10 ? 10 : width;
            int w = (int) width;

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //ACTION_DOWN and ACTION_MOVE are the exact same so just drop to the next one
                case MotionEvent.ACTION_MOVE:
                    //get color of corresponding pixel in image
                    int color = imageViewBitmap.getPixel(x, y);
                    _paint.setColor(color);

                    //logic of painting based on brush type
                    if (_brushType == BrushType.Square) {
                        _offScreenCanvas.drawRect(touchX - width, touchY - width, touchX + width, touchY + width, _paint);
                    } else if (_brushType == BrushType.Circle) {
                        _offScreenCanvas.drawCircle(touchX, touchY, width, _paint);
                    } else if (_brushType == BrushType.Line) {
                        if (dx < 0) {
                            if (dy < 0) {
                                _offScreenCanvas.drawLine(touchX - width, touchY + width, touchX + width, touchY - width, _paint);
                            } else {
                                _offScreenCanvas.drawLine(touchX - width, touchY - width, touchX + width, touchY + width, _paint);
                            }
                        } else {
                            if (dy < 0) {
                                _offScreenCanvas.drawLine(touchX + width, touchY + width, touchX - width, touchY - width, _paint);
                            } else {
                                _offScreenCanvas.drawLine(touchX + width, touchY - width, touchX - width, touchY + width, _paint);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //reset the velocity tracker
                    velocityTracker.clear();
                    break;
            }
            //invalidate the rectangle around where the touch event happen. I imagine it speeds things up
            invalidate(x - w, y - w, x + w, y + w);
        }

        return true;
    }

    /**
     * Saves image to file that is in the directory of parameter dir
     * @param dir
     * @param c
     */
    public void saveImage(File dir, Context c) {

        try {
            //finds file name that isn't used up
            File f;
            int i = 0;
            do {
                i++;
                f = new File(dir, "Impressionist_" + i + ".PNG");
            } while (f.exists());
            //save the file
            FileOutputStream fos = new FileOutputStream(f);
            _offScreenBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            //add it to the gallery
            FileUtils.addImageToGallery(f.getAbsolutePath(), c);
            //let the user know where it is saved
            Toast.makeText(c, "Saved to " + f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     * - http://stackoverflow.com/a/15538856
     * - http://stackoverflow.com/a/26930938
     *
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView) {
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual) / 2;
        int left = (int) (imgViewW - widthActual) / 2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

/*
//Rect r = getBitmapPositionInsideImageView(_imageView);
                _imageView.buildDrawingCache(true);
                Bitmap bmap = _imageView.getDrawingCache (true);
                //_imageView.setImageBitmap(bmap);
                _offScreenBitmap = Bitmap.createBitmap(_imageView.getDrawingCache(true));
                //Log.i("is it null?",((BitmapDrawable) _imageView.getDrawable()).toString());
                //_offScreenBitmap = ((BitmapDrawable) _imageView.getDrawable()).getBitmap();
                //_offScreenBitmap
                //int[] pixels = new int[(int) (_offScreenBitmap.getWidth()*_offScreenBitmap.getWidth())];
                //pixels[-1] = 0;
                //Log.i("touch down", "getting pixels");
                //Log.i("touch down", pixels.length + " " +0 +" "+ _offScreenBitmap.getWidth()+" "+  0+" "+  0+" "+  10+" "+  10);
                //_offScreenBitmap.getPixels(pixels,0,_offScreenBitmap.getWidth(),(int) touchX, (int) touchY,(int) _paint.getStrokeWidth(),(int) _paint.getStrokeWidth());
                //_offScreenBitmap.getPixels(pixels, 0, _offScreenBitmap.getWidth(), 0, 0, 10, 10);
                //_offScreenBitmap.getPixels(pixels,0,1,(int) touchX, (int) touchY,(int) _offScreenBitmap.getWidth(),(int) _offScreenBitmap.getWidth());
                //for(int i : pixels){
                // Log.i("touch",new Integer(i).toString());
                // }

                for(int i = 0; i < _paint.getStrokeWidth(); i++) {
                    for (int j = 0; j < _paint.getStrokeWidth(); j++) {
                        int x = (int) touchX + i;
                        int y = (int) touchY + j;
                        if (!(x < 0 || y < 0 || x > imageViewBitmap.getWidth() || y > imageViewBitmap.getHeight())) {
                            float[] hsv = new float[3];
                            Color.colorToHSV(imageViewBitmap.getPixel(x, y), hsv);
                            int c = Color.HSVToColor(210, hsv);
                            _offScreenBitmap.setPixel(x, y, c);
                        }
                    }
                }
 */

/*
                //int c = 0;

                int reds = 0;
                int greens = 0;
                int blues = 0;
                //int[] pixels = new int[imageViewBitmap.getWidth() * imageViewBitmap.getHeight()];
                for(int i = 0; i < _paint.getStrokeWidth(); i++){
                    for(int j = 0; j < _paint.getStrokeWidth(); j++){
                        int x = (int) touchX+i;
                        int y = (int) touchY+j;
                        if(!(x > imageViewBitmap.getWidth() || y > imageViewBitmap.getHeight())) {
                            //float[] hsv = new float[3];
                            int color =imageViewBitmap.getPixel(x,y); // x + y * width
                            reds += (color >> 16) & 0xFF; // Color.red
                            greens += (color >> 8) & 0xFF; // Color.greed
                            blues += (color & 0xFF); // Color.blue
                            //c += imageViewBitmap.getPixel(x,y);

                        }
                    }
                }
                int red = reds / (int) (_paint.getStrokeWidth() * _paint.getStrokeWidth());
                int green = greens / (int) (_paint.getStrokeWidth() * _paint.getStrokeWidth());
                int blue = blues/ (int) (_paint.getStrokeWidth() * _paint.getStrokeWidth());
                _offScreenCanvas.drawRect((touchX - _paint.getStrokeWidth())/2, (touchY - _paint.getStrokeWidth())/2, (touchX + _paint.getStrokeWidth())/2, (touchY+ _paint.getStrokeWidth())/2, _paint);
                */

//Log.i("colors",red+ " "+green + " "+ blue);
//for(int i = 0; i < _paint.getStrokeWidth()*10; i++){
//    for(int j = 0; j < _paint.getStrokeWidth()*10; j++){
//        int x = (int) touchX+i;
//        int y = (int) touchY+j;
//        if(!(x > imageViewBitmap.getWidth() || y > imageViewBitmap.getHeight())) {
//            _offScreenBitmap.setPixel(x,y,Color.rgb(red,green,blue));
//        }
//    }
//}
//_offScreenBitmap.setPixels(pixels,0,);
//_offScreenBitmap.setPixel();
//break;
