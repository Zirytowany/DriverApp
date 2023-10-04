package eu.mobileApp.DriverApp.comm

data class PendingAbsence(
    var ID:String ,
    var DateFrom: String,
    var DateTo: String,
    var AbsenceDays: Int,
    var Info: String,
    var StatusID: String
)
