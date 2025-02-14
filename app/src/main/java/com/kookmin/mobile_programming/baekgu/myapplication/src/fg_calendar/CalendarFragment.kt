package com.kookmin.mobile_programming.baekgu.myapplication.src.fg_calendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CalendarView.OnDateChangeListener
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.kookmin.mobile_programming.baekgu.myapplication.R
import com.kookmin.mobile_programming.baekgu.myapplication.config.BaseFragment
import com.kookmin.mobile_programming.baekgu.myapplication.databinding.FragmentCalendarBinding
import com.kookmin.mobile_programming.baekgu.myapplication.src.diet_details.DietDetailsActivity
import com.kookmin.mobile_programming.baekgu.myapplication.src.diet_details.rv.DietDetailsDataClass
import com.kookmin.mobile_programming.baekgu.myapplication.src.dto.DietInfo
import com.kookmin.mobile_programming.baekgu.myapplication.src.dto.LocalDB
import com.kookmin.mobile_programming.baekgu.myapplication.src.fg_calendar.data_class.ProteinAmountDataClass
import java.text.SimpleDateFormat
import java.util.*


class CalendarFragment: BaseFragment<FragmentCalendarBinding>(FragmentCalendarBinding::bind, R.layout.fragment_calendar) {
    private val TAG="CalendarFragmentTAG"

    private var month:String=""
    private var day:String=""

    private var proteinAmountList=ArrayList<ProteinAmountDataClass>()
    private var dietDetailsList=ArrayList<DietDetailsDataClass>()

    private val DIET_DETAILS_CODE=101

    private var firebaseDatabase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null
    private lateinit var auth: FirebaseAuth


    // Pre contract
    private val preDietDetailsActivityContractStartActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { a_result ->
            if (a_result.resultCode == DIET_DETAILS_CODE) {
                a_result.data?.let {
                    var amountList=it.getSerializableExtra("proteinAmountList") as ArrayList<Int>
                    for(i in 0 until amountList.size){
                        proteinAmountList[i].currentAmount=proteinAmountList[i].currentAmount+amountList[i]
                    }

                    binding.fgCalendarTvCurrentProtein.text="${proteinAmountList[day.toInt()-1].currentAmount.toString()}g"
                    Log.d("wegwegaes",amountList.toString())
                }
            }
        }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val now = System.currentTimeMillis()
        val date = Date(now)

        var sdf = SimpleDateFormat("MM.dd")

        val getTime: String = sdf.format(date)
        month = getTime.split(".")[0]
        day = getTime.split(".")[1]


        Log.d("weagawegawegwaeg", getTime)

        binding.fgCalendarTvDate.text = getTime
        setListener()


        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase!!.getReference().child("Survey")
        auth = Firebase.auth
        val email: String = (auth.currentUser?.email) as String

        databaseReference!!.get().addOnSuccessListener {

            val data = it.children.iterator().next().getValue() as HashMap<String, Any>
            Log.d("전부다", data.toString())
            var fflavour: Array<Int>? = null
            var fproduct: Array<Int>? = null
            var fproteinAmount: Int? = null
            if (data.get("user_id").toString() == email) {
                var sFFlavour = data.get("user_flaPre") as ArrayList<Int>?
                var sFProduct = data.get("user_proPre") as ArrayList<Int>?
                var sFProtein = data.get("user_proteinAmount") as Int
                fflavour = strToArray(sFFlavour!!)
                fproduct = strToArray(sFProduct!!).copyOf()
                fproteinAmount = sFProtein!!
            }


            // DB에서 받아오는 부분 ---------------------------------------
            // 제품 순서 : 소시지, 볼, 소스, 소고기, 생선, 스테이크, 프로틴, 간식
            // allergy : product 배열 idx

            val proteinAmount =fproteinAmount ?: 85
            val flavour = fflavour ?: arrayOf(4, 2, 3, 5, 4, 3, 2, 1)
            val product = fproduct ?: arrayOf(5, 2, 3, 4, 1, 4)

            val allergy = 4 // 생선
            val month = 12 //캘린더 정보에서 받아오기

            val result = makeDietCalendar(proteinAmount, flavour, product, allergy, month)

            binding.fgCalendarTvTargetProtein.text = "${proteinAmount.toString()}g"
            binding.fgCalendarTvCurrentProtein.text = "0g"

            for (i: Int in 0..30) {
                proteinAmountList.add(ProteinAmountDataClass(proteinAmount, 0))
                dietDetailsList.add(
                    DietDetailsDataClass(
                        month.toString(),
                        (i + 1).toString(),
                        result.calendar[i][0],
                        result.calendar[i][1],
                        result.calendar[i][2],
                        result.calendar[i][3],
                        result.calendar[i][4]
                    )
                )
                Log.d(TAG, "2022/12/${i + 1} 아침 : ${result.calendar[i][0]}")
                Log.d(TAG, "2022/12/${i + 1} 점심 : ${result.calendar[i][1]}")
                Log.d(TAG, "2022/12/${i + 1} 저녁 : ${result.calendar[i][2]}")
                Log.d(TAG, "2022/12/${i + 1} 간식1 :${result.calendar[i][3]}")
                Log.d(TAG, "2022/12/${i + 1} 간식2 :${result.calendar[i][4]}")
                Log.d(TAG, "----------------------------------------------")
            }


        }
    }

    private fun strToArray(arraylist: ArrayList<Int>) : Array<Int> {
        val result = Array<Int>(10){i->0}
        for(i: Int in 0..arraylist.size-1){
            result[i] = Integer.parseInt(arraylist[i].toString())
            Log.d("result", result.toString())
        }
        return result
    }

    private fun setListener(){


        binding.fgCalendarTvMore.setOnClickListener {

            var intent=Intent(requireContext(),DietDetailsActivity::class.java)

            intent.putExtra("date","${month}.${day}")
            intent.putExtra("dietList",dietDetailsList)

            preDietDetailsActivityContractStartActivityResult.launch(intent)

        }


        binding.fgCalendarMain.setOnDateChangeListener(OnDateChangeListener { calendarView, i, i1, i2 ->
            day=i2.toString()

            proteinAmountList[i2-1]

            if((i2-1)>0 && (i2-1)<31){
                binding.fgCalendarTvTargetProtein.text= "${proteinAmountList[i2-1].targetAmount.toString()}g"
                binding.fgCalendarTvCurrentProtein.text="${proteinAmountList[i2-1].currentAmount.toString()}g"
            }
            binding.fgCalendarTvDate.text="${i1+1}.${i2}"

        })

    }



    fun makeDietCalendar(proteinAmount: Int, flavour: Array<Int>, product: Array<Int>, allergy: Int, month: Int) : DietInfo {

        // 1. 받아온 달에 맞춰 필요한 총 상품 갯수 산출
        val case1 = arrayOf(1,3,4,7,8,10,12)
        val case2 = arrayOf(4,6,9,11)
        var totalAmount = 28 * 3
        if(case1.contains(month)) totalAmount = 31 * 3
        else if(case2.contains(month)) totalAmount = 30 * 3

        // 2. 알러지를 제외한 상품 리스트 생성
        val finalProduct = arrayListOf<Int>()
        var sum = 0         // 제품 선호 비율에 따라 식단을 구성하기 위한 변수
        var sumCheck = 0    // 비율별로 구성하고 모자란 제품을 채워주기 위한 변수

        for(i : Int in 0 until product.size){
            if(i != allergy) {
                sum += product[i]
                finalProduct.add(product[i])
            }
            else if(i == allergy){
                finalProduct.add(0)
            }
        }
        Log.d("FINAL PRODUCT CHECK :: ", finalProduct.toString())

        // 3. 제품 선호 비율에 맞게 한달치 제품을 배정하고 모자란 부분은 제일 선호가 높았던 제품으로 채워줌
        var max = finalProduct[0]
        var idx = 0
        for(i : Int in 0 until finalProduct.size){
            if(finalProduct[i] > max) {
                max = finalProduct[i]
                idx = i
            }
            val value = ( totalAmount / sum * finalProduct[i]) / 1
            sumCheck += value
            finalProduct[i] = value // finalProduct 배열을 선호 수치에 따라 한달치 상품 갯수 비율에 맞게 수정
        }
        // ex) finalProduct = [10, 15, 20, 20, 0, 25+3]
        if(totalAmount - sumCheck > 0) finalProduct[idx] += (totalAmount - sumCheck) // 한달치 식단을 짜고 모자란 갯수는 제일 선호도가 높은 제품으로 채워줌

        // 4. 하루 필요 단백질양을 고려하여 식단 산출
        // dietInfo에 30일치 식단 넣어서 보내줄거임.
        var dietInfo = DietInfo()
        var localDb = LocalDB()
        var dietList = arrayListOf<String>()

        for(i : Int in 0 until finalProduct.size) {

            if (finalProduct[i] == 0) continue  // 알러지 상품은 제외
            val random = Random()

            for(j : Int in 0..finalProduct[i]){
                dietList.add(localDb.product[i][random.nextInt(localDb.product[i].size)].toString())
            }
        }
        dietList.shuffle() // 구성된 제품을 랜점하게 셔플

        for(i : Int in 0 until (totalAmount/3)){
            dietInfo.calendar[i][0] = dietList.get(3*i)
            dietInfo.calendar[i][1] = dietList.get(3*i+1)
            dietInfo.calendar[i][2] = dietList.get(3*i+2)
        }

        // 단백질 양이 부족하면 채워줌
        if (70 < proteinAmount - 20){
            for(i : Int in 0 until (totalAmount/3)){
                dietInfo.calendar[i][3] = localDb.protein[i%10]
            }
            if (91 < proteinAmount - 20){
                for(i : Int in 0 until (totalAmount/3)){
                    dietInfo.calendar[i][4] = localDb.appetizer[i%5]
                }
            }
        }

        return dietInfo
    }

}