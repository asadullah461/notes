package com.asadullah.notes.listeners;

import com.asadullah.notes.room.Note;

public interface NotesListeners {

    void onNoteClicked(Note note, int position);
}
