package com.example.pieviewtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pie_vew.apply {
            val list = arrayListOf<PieData>()
            list.add(PieData(20, 0f, 0, 0f))
            list.add(PieData(20, 0f, 0, 0f))
            list.add(PieData(20, 0f, 0, 0f))
            list.add(PieData(20, 0f, 0, 0f))
            list.add(PieData(20, 0f, 0, 0f))
            setPieData(list)
        }
    }
}