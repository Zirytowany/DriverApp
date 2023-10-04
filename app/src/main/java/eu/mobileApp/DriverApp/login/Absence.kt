package eu.mobileApp.DriverApp.login

data class Absence(
    var ID:String ,
    var DateFrom: String,
    var DateTo: String,
    var AbsenceDays: Int,
    var Info: String,
    var StatusID: Int,
    var Status: String
)
