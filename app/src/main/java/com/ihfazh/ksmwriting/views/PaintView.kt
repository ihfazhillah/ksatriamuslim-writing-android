package com.ihfazh.ksmwriting.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PaintView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        // length in pixel of each dimension for the bitmap displayed on the screen
        const val BITMAP_DIMEN = 128

        // length in pixels of each dimension for the bitmap to be fed into the model
        const val FEED_DIMEN = 28

        const val TAG = "PAINTVIEW"
    }

    private var setup = false
    private var drawHere = true

    private var paint: Paint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
    }

    private var bitmap: Bitmap? = null
    private var path: Path = Path()
    private  var canvas: Canvas? = null


    private val transformMat = Matrix()
    private val inverseTransformMat = Matrix()

    private val pointF = PointF()

    private val paintPathList = arrayListOf<PaintPath>()
    private lateinit var drawTextView: View

    data class PaintPath(
        val paint: Paint,
        val path: Path
    )


    fun reset(){
        path.reset()
        bitmap?.eraseColor(Color.BLACK)
    }

    fun setDrawText(view: View){
        drawTextView = view
        drawHere = true
    }

    /*
    This function will create the transform matrix for scaling up and centering the bitmap
    that will represent our character image inside the view.
    The inverse matrix is also created
    for mapping touch coordinates to coordinates within the bitmap
     */
    fun setupScaleMatrices(){

        // view size
        val theWidth: Float = width.toFloat()
        val theHeight: Float = height.toFloat()
        val scaleW: Float = theWidth / BITMAP_DIMEN
        val scaleH: Float = theHeight / BITMAP_DIMEN

        var scale: Float = scaleW
        if (scale > scaleH){
            scale = scaleH
        }

        // translation to center bitmap in view after it is scaled up
        val centerX = BITMAP_DIMEN * scale / 2
        val centerY = BITMAP_DIMEN * scale / 2
        val dx = theWidth / 2 - centerX
        val dy = height / 2 - centerY

        transformMat.setScale(scale, scale)
        transformMat.postTranslate(dx, dy)
        transformMat.invert(inverseTransformMat)
        setup = true

    }

    /*
    This gets the coordinates in the bitmap based on the coordinates of where the user touched
     */
    fun getBitmapCoords(x: Float, y: Float, out: PointF){
        val points = floatArrayOf(x, y)
        inverseTransformMat.mapPoints(points)
        out.x = points[0]
        out.y = points[1]
    }

    fun createBitmap(){
        bitmap?.recycle()
        bitmap = Bitmap.createBitmap(BITMAP_DIMEN, BITMAP_DIMEN, Bitmap.Config.ARGB_8888).also{
            canvas = Canvas(it)
        }
        reset()
    }

    fun releaseBitmap(){
        bitmap?.recycle()
        bitmap = null
        canvas = null
        reset()
    }

    fun onResume(){
        createBitmap()
    }

    fun onPause(){
        releaseBitmap()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (drawHere){
            drawTextView.visibility = INVISIBLE
            drawHere = false
        }

        canvas?.drawPath(path, paint)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                getBitmapCoords(event.x, event.y, pointF)
                path.moveTo(pointF.x, pointF.y)
                path.lineTo(pointF.x, pointF.y)
            }
            MotionEvent.ACTION_MOVE -> {
                getBitmapCoords(event.x, event.y, pointF)
                path.lineTo(pointF.x, pointF.y)
                paint.color = Color.WHITE
                paintPathList.add(PaintPath(paint, path))
            }
        }

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        if (!setup){
            setupScaleMatrices()
        }

        bitmap?.let {
            canvas?.drawBitmap(it, transformMat, paint)

//            val paintPath = paintPathList.lastOrNull()
//            paintPath?.let{ pp ->
//                canvas?.drawPath(pp.path, pp.paint)
//            }
        }

    }

    fun getPixelData(): FloatArray? {
        return bitmap?.let {
            val resizedBitmap = Bitmap.createScaledBitmap(it, FEED_DIMEN, FEED_DIMEN, false)

            val width = FEED_DIMEN
            val height = FEED_DIMEN

            val pixels = IntArray(width * height)
            resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)


            val returnPixels = FloatArray(pixels.size)

            pixels.forEachIndexed { index, i ->
                val b = i and 0xff
                returnPixels[index] = (b/255.0).toFloat()
            }

            return returnPixels

        }
    }

    fun getByteBuffer(): ByteBuffer? {

        return bitmap?.let {
            val resizedBitmap = Bitmap.createScaledBitmap(it, FEED_DIMEN, FEED_DIMEN, false)

            val width = FEED_DIMEN
            val height = FEED_DIMEN

            val pixels = IntArray(width * height)
            resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
//
//            val returnPixels = FloatArray(pixels.size)
//
//            pixels.forEachIndexed { index, i ->
//                val b = i and 0xff
//                returnPixels[index] = (b/255.0).toFloat()
//            }
//
            val modelInputSize = 4 * 28 * 28 * 1
            val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
            byteBuffer.order(ByteOrder.nativeOrder())

            for (pixelValue in pixels) {
                val r = (pixelValue shr 16 and 0xFF)
                val g = (pixelValue shr 8 and 0xFF)
                val b = (pixelValue and 0xFF)

                val normalizedPixelvalue = (r + g + b) / 3.0f / 255.0f
                byteBuffer.putFloat(normalizedPixelvalue)
            }



            return byteBuffer

        }


    }


}