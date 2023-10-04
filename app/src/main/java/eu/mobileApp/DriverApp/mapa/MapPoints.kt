package eu.mobileApp.DriverApp.mapa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MapPoints(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    @ColumnInfo(name="user")val name: String? = null,
    @ColumnInfo(name="user")val order: String? = null,
    @ColumnInfo(name="points")val points:String
)