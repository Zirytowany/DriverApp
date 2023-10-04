package eu.mobileApp.DriverApp.orders

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ExpandableListAdapter
import android.widget.Toast
import eu.mobileApp.DriverApp.MainScreen
import eu.mobileApp.DriverApp.R
import eu.mobileApp.DriverApp.databinding.ActivityOrderBinding

class OrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderBinding

    private var adapter: ExpandableListAdapter?=null
    private var titleList: List<String>?=null

    val data: HashMap<String,List<String>>
        get(){
            val listData = HashMap<String, List<String>>()

            var przyklZlecenie=ArrayList<String>()
            przyklZlecenie.add("Miejsca docelowe")
            przyklZlecenie.add("Garaż")
            przyklZlecenie.add("Postoje")

            listData["Przykładowe zlecenie a\nzzzzzzzzzzza\nazzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"] = przyklZlecenie

            return listData
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityOrderBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)


        setupExpandableListView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    fun onGroupItemClick(item: MenuItem) {
        when (item.toString()) {
            "home"->{
                val intent = Intent(this, MainScreen::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


    private fun setupExpandableListView(){
        val expandableListView=binding.expandableListView
        val listData = data
        titleList=ArrayList(listData.keys)
        adapter= CLAdapter(this,titleList as ArrayList<String>, listData)
        expandableListView.setAdapter(adapter)

        expandableListView.setOnGroupExpandListener { groupPosition->
            Toast.makeText(applicationContext, (titleList as ArrayList<String>)[groupPosition]+" List Expanded.",
                Toast.LENGTH_SHORT).show()
        }

        expandableListView.setOnChildClickListener{parent, v, groupPosition, childPosition, id->
            Toast.makeText(applicationContext,
                "Clicked: "+(titleList as ArrayList<String>)[groupPosition] +" -> "+listData[(titleList as ArrayList<String>)[groupPosition]]!!.get(childPosition),
                Toast.LENGTH_SHORT).show()
            false
        }
    }
}