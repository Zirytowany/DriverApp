package eu.mobileApp.DriverApp.login

import androidx.room.*

@Dao
interface LoginDAO {
    @Query("SELECT * FROM user")
    fun getAll():List<User>

    @Query("SELECT * FROM user where name=:name")
    fun getUser(name:String):User

    //@Query("SELECT loginDate, expirationDate FROM user where name=:name AND password=:password AND logged=:logged")
    //fun getDate(name:String, password:String, logged: Boolean):List<Long>

    @Insert
    fun insert(vararg user: User)

    @Delete
    fun delete(user: User)

    @Query("DELETE FROM user WHERE loginDate>=expirationDate")
    fun deleteExpired()

    @Update
    fun updateUser(user: User)

    @Query("UPDATE user SET loginDate=:date WHERE name=:name AND password=:password AND logged=:logged")
    fun updateLoginDate(date:Long, name:String, password:String, logged:Boolean)
}