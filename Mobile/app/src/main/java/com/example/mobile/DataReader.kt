package com.example.mobile

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.BufferedReader
import java.io.File
import java.util.*

class DataReader   {

    private val file_path: String = "/data/data/polar.project/files/database1.txt"

    val firebaseHandler = FirebaseHandler()
    //check fireAuthentication
    //    val mFirebaseAuth = FirebaseAuth.getInstance();
    //
    //    val mFirebaseUser = mFirebaseAuth.currentUser;
    //data title


   // private val db = Firebase.firestore
    private val TRAINING = "training"
    private val ROUND = "round"
    private val PUNCH = "punch"
    private val ROUND_INFO = "round_info"
    var count = 1
    //Read and handle data structure same as data design
    fun DataHandler()
    {

        val currentUser = firebaseHandler.getCurrentUser()
        Log.e("USER",currentUser?.email.toString())
        var stringArray: MutableList<String> = mutableListOf()

       // var round:RoundInforTest
        var punch = Punch(0,false,0.0)
        var punches: MutableList<Punch> =mutableListOf()
        var roundSessions: MutableList<RoundInfor> =mutableListOf()

        //Get training_id vs date in files
        var training_id:String =""
        var date:Date=Date()
        //Get data by lines from file
        stringArray =  ReadDataFromFile()


        stringArray.map { it ->
            var stringHandler = it.replace("[", "").replace("]", "").split(",").toList();

//            stringHandler.map{
//                Log.e("TAG",  it.trimStart())
//            }
            when(stringHandler[0]){

                TRAINING -> {
//                     training1 = Training(stringHandler[1].trimStart(),"","", Date())
//                    training1 =Training({})
//                    Log.e("TAG",  training1.toString())
                    training_id=stringHandler[1].trimStart()
                    date = Date(stringHandler[2].trimStart())
                }
                ROUND -> {
                   // var round = RoundSession( stringHandler[1].trimStart(),stringHandler[2].trimStart(),"","")
                   // Log.e("TAG",  round.toString())
                }
                PUNCH -> {
                     punch = Punch( stringHandler[1].trimStart().toInt(), stringHandler[2].trimStart().toBoolean(), stringHandler[3].trimStart().toDouble())

                    punches.add(punch)
                }
                ROUND_INFO -> {
                    var roundInfo= RoundInfor(
                        stringHandler[1].trimStart(),
                        stringHandler[2].trimStart().toDouble(),
                        stringHandler[3].trimStart().toInt(),
                        stringHandler[4].trimStart().toInt(),
                        stringHandler[5].trimStart().toInt(),
                        stringHandler[6].trimStart().toDouble(),
                        stringHandler[7].trimStart().toDouble(),
                        punches
                    )
                    roundSessions.add(roundInfo)
                    punches =mutableListOf()
                }

                else -> Log.e("ERROR", "SOMETHING IS WRONG !!!!")
            }

        }

        var training = Training(training_id,currentUser?.uid.toString(),currentUser?.email.toString(), date,roundSessions)
        //var trainingTest= TrainingTest(training)

        Log.e("TAG",  training.toString())

        if(currentUser?.email !== null)
        {
          firebaseHandler.addDocument("training_test",training)

        }
        else
        {
            CheckInternalFiles()
        }

    }


    fun ReadDataFromFile(): MutableList<String> {
        val stringArray: MutableList<String> = mutableListOf()

        try {
            val bufferedReader: BufferedReader = File(file_path).bufferedReader()
            bufferedReader.use { br->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val parts = line?.split(",")

                    stringArray.add(parts.toString())

                }


            }
            // Log.e("TAG",inputString)
        } catch (ex: Exception) {
            //Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
            Log.e("hihihaha", "Error: ${ex.message}")
        }
        return stringArray
    }


    fun CheckInternalFiles(){
        val file = File("/data/data/polar.project/files/")
        val list = file.listFiles()
        var count = 0
        for (f in list) {
            val name = f.name
            if (name.startsWith("database_without_account") ) count++

        }
        println("How many files: $count")
    }

    }

