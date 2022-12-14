package com.example.mobile

import java.util.*

//data class Training (
//    var sessionID: String = "",
//    var userId: String = "123",
//    var username: String = "123",
//    var date: Date = Date(),
//)
//data class RoundSession(
//    var sessionID: String = "",
//    var roundID: String = "",
//    var userId: String = "",
//    var username: String = "123",
////    var date: Date = Date(),
////    var roundInfo:RoundInfor
//)
//data class RoundInfor(
//    var roundID: String = "",
//    var round_length: Float = 0.0f,
//    var  total_punches: Int = 0,
//    var correct_punches: Int = 0,
//    var incorrect_punches: Int = 0,
//    var avg_heart_rate: Double = 0.0,
//    var avg_speed: Double = 0.0
//)

data class Punch(
    var punchID: Int = 0,
    var isCorrect: Boolean = true,
    var speed: Double = 0.0
)

data class Training(
    var sessionID: String = "",
    var userId: String = "",
    var username: String = "",
    var date: Date = Date(),
    var round_Sessions: MutableList<RoundInfor>
)

data class RoundInfor(
    var roundID: String = "",
    var round_length: Double = 0.0,
    var total_punches: Int = 0,
    var correct_punches: Int = 0,
    var incorrect_punches: Int = 0,
    var avg_heart_rate: Double = 0.0,
    var avg_speed: Double = 0.0,
    var punches: MutableList<Punch>
)