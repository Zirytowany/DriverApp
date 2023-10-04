package eu.mobileApp.DriverApp.login

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    @ColumnInfo(name="name")val name: String? = null,
    @ColumnInfo(name="password")val password: String? = null,
    @ColumnInfo(name="jwt")val jwt:String?=null,
    @ColumnInfo(name="expirationDate")val expirationDate:Long?=null,
    @ColumnInfo(name="loginDate")val loginDate:Long?=null,
    @ColumnInfo(name="logged")val logged:Boolean=false,
    //@ColumnInfo(name="HRCode")val hrCode:String?=null,
    //@ColumnInfo(name="driverID")val driverID:String?=null,
    //@ColumnInfo(name="crewID")val crewID:String?=null
)