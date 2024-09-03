package com.asadullah.notes.room;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

@androidx.room.Database(entities = Note.class, version = 1 , exportSchema = false)
public abstract class Database extends RoomDatabase {

    private static Database database;

    public static synchronized Database getDatabase(Context context){
        if (database == null){
            database = Room.databaseBuilder(
                    context,
                    Database.class,
                    "notes_db"
            ).build();
        }
        return database;
    }

    public abstract Dao dao();
}
