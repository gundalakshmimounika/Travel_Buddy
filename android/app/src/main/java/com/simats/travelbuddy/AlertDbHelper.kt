package com.simats.travelbuddy

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AlertItem(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long
)

class AlertDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "travelbuddy_alerts.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_NAME = "alerts"
        const val COLUMN_ID = "id"
        const val COLUMN_USER_EMAIL = "user_email"
        const val COLUMN_TITLE = "title"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_TYPE = "type"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_EMAIL + " TEXT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_MESSAGE + " TEXT, "
                + COLUMN_TYPE + " TEXT, "
                + COLUMN_TIMESTAMP + " INTEGER)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertAlert(userEmail: String, title: String, message: String, type: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_USER_EMAIL, userEmail)
        contentValues.put(COLUMN_TITLE, title)
        contentValues.put(COLUMN_MESSAGE, message)
        contentValues.put(COLUMN_TYPE, type)
        contentValues.put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        
        val result = db.insert(TABLE_NAME, null, contentValues)
        return result != -1L
    }

    fun getAllAlerts(userEmail: String): List<AlertItem> {
        val alertList = ArrayList<AlertItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_USER_EMAIL = ? ORDER BY $COLUMN_TIMESTAMP DESC",
            arrayOf(userEmail)
        )
        
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                
                alertList.add(AlertItem(id, title, message, type, timestamp))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return alertList
    }

    fun clearAllAlerts(userEmail: String) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_USER_EMAIL = ?", arrayOf(userEmail))
    }

    fun deleteAlert(id: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return result > 0
    }
}
