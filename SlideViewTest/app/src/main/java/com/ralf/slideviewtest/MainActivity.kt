package com.ralf.slideviewtest

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        single_btn.setOnClickListener {
            startActivity(Intent(this, SingleSlideActivity::class.java))
        }
        multi_btn.setOnClickListener {
            startActivity(Intent(this, MultiViewSlideActivity::class.java))
        }

    }
}
