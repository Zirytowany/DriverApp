package eu.mobileApp.DriverApp.login

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class], version=1)
abstract class LoginDatabase: RoomDatabase() {
    abstract fun LoginDAO(): LoginDAO

    companion object{
        @Volatile
        private var INSTANCE: LoginDatabase?=null

        fun getDatabase(context: Context): LoginDatabase {
            var tempInstance = INSTANCE
            if(tempInstance!=null){
                return tempInstance
            }
            synchronized(this){
                val instance= Room.databaseBuilder(
                    context.applicationContext,
                    LoginDatabase::class.java,
                    DB_NAME
                ).build()
                INSTANCE =instance
                return instance
            }
        }
    }
}