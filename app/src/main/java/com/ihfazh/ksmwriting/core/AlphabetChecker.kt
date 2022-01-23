package com.ihfazh.ksmwriting.core

import android.content.Context
import com.ihfazh.ksmwriting.ml.AlphabetAllCategory50
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AlphabetChecker(context: Context) {
    private var model: AlphabetAllCategory50? = AlphabetAllCategory50.newInstance(context)
    private val abjads = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    suspend fun predict(byteBuffer: ByteBuffer): Char? {
        return suspendCoroutine {
            val input = TensorBuffer.createFixedSize(intArrayOf(1, 28, 28, 1), DataType.FLOAT32)
            input.loadBuffer(byteBuffer)
            val outputs = model?.process(input)
            val char = outputs?.outputFeature0AsTensorBuffer?.floatArray?.let{ array ->
                getChar(array)
            }

            it.resume(char)
        }
    }

    private fun getMaxIndex(floatArray: FloatArray): Int? {
        return floatArray.indices.maxByOrNull {
            floatArray[it]
        }
    }

    private fun getChar(floatArray: FloatArray): Char?{
        val index = getMaxIndex(floatArray)
        return index?.let{
            abjads[it]
        }
    }

    fun close(){
        model?.close()
        model = null
    }
}