package com.ihfazh.ksmwriting

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class WrittenText(
    val char: String,
    val active: Boolean,
    val written: Boolean = false
)

class PaintViewModel: ViewModel() {
    private val text = MutableLiveData("")
    private val activeIndex = MutableLiveData<Int>()
    val getMatch = MutableLiveData<Boolean>()


    val splittedText = MediatorLiveData<List<WrittenText>>().apply {
        addSource(text){ text ->
            value = text.split("").filter{
                it != ""
            }
                .map{
                WrittenText(it, active = false, written = false)
            }
        }

        addSource(activeIndex){
            val currentValue = value

            if (currentValue != null){
                value = currentValue.mapIndexed {index, writtenText ->
                    if (index == it){
                        writtenText.copy(active = true)
                    } else {
                        writtenText
                    }
                }
            }
        }
    }

    val currentActive = MediatorLiveData<String?>().apply {
        addSource(activeIndex){
            value = text.value?.getOrNull(it)?.toString()
        }
    }

    fun setText(text: String){
        this.text.value = text
        this.activeIndex.value = 0
    }

    fun setPredicted(predicted: Char){
        val index = this.activeIndex.value
        val textList = splittedText.value!!

        var isMatch = false

        val finalText = textList.mapIndexed { i, writtenText ->
            if (index == i){
                isMatch = writtenText.char == predicted.toString()
                writtenText.copy(written = isMatch)
            } else {
                writtenText
            }
        }

        Log.d("VIEWMODEL", "setPredicted: $finalText")

        splittedText.postValue(finalText)
        getMatch.postValue(isMatch)

        // next active char
        if (isMatch){
            activeIndex.postValue(activeIndex.value?.plus(1) ?: 0)
        }

    }

}