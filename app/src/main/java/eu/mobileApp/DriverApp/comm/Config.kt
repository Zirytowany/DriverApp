package eu.mobileApp.DriverApp.comm

data class Config(
    var LocalizationNetInterval:Int,
    var LocalizationSMSInterval:Int,
    var SOSInactivityPeriod:Int,
    var SMSButtonSensitivity:Int,
    var SwipingCounter:Int,
    var ScreenCounter:Int,
    var ScreenTimer:Int,
    var SwipingTimer:Int,
    var SwipingLowerAcceler:Int,
    var SwipingUpperAcceler:Int,
    var SwipingLog:String,
    var SOSPhoneNumber:String
)
