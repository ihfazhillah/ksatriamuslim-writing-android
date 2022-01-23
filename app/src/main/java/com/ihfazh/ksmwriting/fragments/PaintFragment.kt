package com.ihfazh.ksmwriting.fragments

import android.animation.Animator
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.ihfazh.ksmwriting.PaintViewModel
import com.ihfazh.ksmwriting.R
import com.ihfazh.ksmwriting.core.AlphabetChecker
import com.ihfazh.ksmwriting.ml.AlphabetAllCategory10
import com.ihfazh.ksmwriting.views.PaintView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PaintFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PaintFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var paintView: PaintView
    private lateinit var animation: LottieAnimationView
    private val viewModel: PaintViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_paint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paintView = view.findViewById(R.id.paintView)
        val drawHereView = view.findViewById<TextView>(R.id.tulis_disini)
        paintView.setDrawText(drawHereView)

        animation = view.findViewById(R.id.MyAnim)
        animation.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                animation.visibility = View.INVISIBLE
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationRepeat(p0: Animator?) {
            }

        })

        viewModel.setText("SAKINAH")

        view.findViewById<ImageButton>(R.id.btn_clear).apply {
            setOnClickListener {
                clear()
            }
        }

        view.findViewById<ImageButton>(R.id.btn_yes).apply{
            setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO){
                    val checker = AlphabetChecker(context)
                    val result = checker.predict(paintView.getByteBuffer()!!)

                    if (result != null){
                        viewModel.setPredicted(result)
                    }

                    Log.d("RESULT", "${result.toString()}")
                    checker.close()
                }
            }
        }

        val text = view.findViewById<TextView>(R.id.text)
        viewModel.splittedText.observe(viewLifecycleOwner){
            val string = buildSpannedString {
                it.forEach {  writtenText ->
                    var color = ForegroundColorSpan(Color.BLACK)

                    if (writtenText.written){
                        color = ForegroundColorSpan(Color.GREEN)
                    } else if (writtenText.active){
                        color = ForegroundColorSpan(Color.RED)
                    }
                    append(writtenText.char, color, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }
            }

            text.text = string
        }

        val currentActive = view.findViewById<TextView>(R.id.CharToWrite)
        viewModel.currentActive.observe(viewLifecycleOwner){
            it?.let { currentChar -> currentActive.text = currentChar }
        }

        viewModel.getMatch.observe(viewLifecycleOwner){
            if (it){
                animation.setAnimation(R.raw.thumbs_up)
            } else {
                animation.setAnimation(R.raw.no_records_found)
            }

            animation.visibility = View.VISIBLE
            animation.playAnimation()
            clear()
        }
    }

    private fun clear(){
        paintView.reset()
        paintView.invalidate()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PaintFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PaintFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onResume() {
        paintView.onResume()
        super.onResume()
    }

    override fun onPause() {
        paintView.onPause()
        super.onPause()
    }
}