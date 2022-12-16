package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : AppCompatActivity() {

    private lateinit var myModelList: ArrayList<MyModel>
    private lateinit var myAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        loadCards()

        //add page change listener
//        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
//            override fun onPageScrolled(
//                position: Int,
//                positionOffset: Float,
//                positionOffsetPixels: Int
//            ) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onPageSelected(position: Int) {
//
//            }
//
//            override fun onPageScrollStateChanged(state: Int) {
//
//            }
//        })

    }

    private fun loadCards() {
        //init list
        myModelList = ArrayList()

        //add items to list
        myModelList.add(
            MyModel(
            1,
            "Before training, you need to set the connection between your sensor and the phone. " +
                    "Go to SETTINGS, have your devices's bluetooth on and hit the SCAN SENSORS button"
        ))
        myModelList.add(MyModel(
            2,
             "Then choose one ID sensor which matches your sensor in the menu list on the right sight," +
                     " and let's START TRAINING"
        ))
        myModelList.add(MyModel(
            3,
            "Pressing the CONNECT button to get your devices connected to each other, you will see the battery power on the right sight. " +
                    "Okay now you can start your first round in the training. " +
                    "You can have many rounds in one training session controlled by START or STOP round button "
        ))
        myModelList.add(MyModel(
            4,
            "When your training session is done, press the END TRAINING button to exit. " +
                    "Then you will be leaded to the STATS screen to review all of your results"
        ))
//        myModelList.add(MyModel(
//            5,
//            "Praesent hendrerit varius justo, ac ultricies turpis posuere a. " +
//                    "Donec auctor dapibus sollicitudin. " +
//                    "Vivamus nulla elit, pulvinar id venenatis quis, egestas ut diam."
//        ))

        //setup Adapter
        myAdapter = ViewPagerAdapter(this, myModelList)

        //set Adapter to View
        viewPager.adapter = myAdapter
        viewPager.setPadding(100, 0, 100, 0)

        //navigation
        help_nav_bar.setOnClickListener {
            val homePage = Intent(this, HomeActivity::class.java)
            startActivity(homePage)
            finish()
        }


    }
}