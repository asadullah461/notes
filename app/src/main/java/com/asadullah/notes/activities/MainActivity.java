package com.asadullah.notes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.asadullah.notes.R;
import com.asadullah.notes.listeners.NotesListeners;
import com.asadullah.notes.recyclerview.NotesAdapter;
import com.asadullah.notes.room.Database;
import com.asadullah.notes.room.Note;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListeners {
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private RecyclerView notesRecyclerview;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;

    private AlertDialog dialogAddUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ImageView imgAddNoteMain = findViewById(R.id.imgAddNoteMain);
        imgAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        notesRecyclerview = findViewById(R.id.notesRecyclerview);
        notesRecyclerview.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerview.setAdapter(notesAdapter);

        // Optional: Load notes on initial launch if any exist
        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        findViewById(R.id.imgAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        findViewById(R.id.imgAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            MainActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE} ,
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                } else {
                    selectImage();
                }
            }
        });

        findViewById(R.id.imgAddWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddUrlDialog();
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage();
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index  = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            // Refresh the note list by querying the database again
            new GetNotesTask().execute();
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            // Refresh the note list by querying the database again
            new GetNotesTask().execute();
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType","image");
                        intent.putExtra("imagePath",selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    } catch (Exception exception){
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
//            // a new note is added, so reload the notes
//            getNotes(REQUEST_CODE_ADD_NOTE);
//        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
//            if (data != null) {
//                getNotes(REQUEST_CODE_UPDATE_NOTE);
//            }
//        }
//    }

    public void getNotes(final int requestCode, final boolean isNoteDeleted) {

        new GetNotesTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
        @Override
        protected List<Note> doInBackground(Void... voids) {
            return Database.getDatabase(getApplicationContext()).dao().getAllNotes();
        }

        @Override
        protected void onPostExecute(List<Note> notes) {
            super.onPostExecute(notes);
            noteList.clear();
            noteList.addAll(notes);
            notesAdapter.notifyDataSetChanged();  // Refresh RecyclerView with the updated list

        }



//            @Override
//            protected void onPostExecute(List<Note> notes) {
//                super.onPostExecute(notes);
//                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
//                    noteList.addAll(notes);
//                    notesAdapter.notifyDataSetChanged();
//                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
//                    noteList.add(0, notes.get(0));
//                    notesAdapter.notifyItemInserted(0);
//                    notesRecyclerview.smoothScrollToPosition(0);
//                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
//                    noteList.remove(noteClickedPosition);
//                    noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
//                    notesAdapter.notifyItemInserted(noteClickedPosition);
//                }
//
//


    }

    private void showAddUrlDialog() {
        if (dialogAddUrl == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url, (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddUrl = builder.create();
            if (dialogAddUrl.getWindow() != null) {
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputUrl = view.findViewById(R.id.inputUrl);
            inputUrl.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (inputUrl.getText().toString().trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputUrl.getText().toString()).matches()) {
                        Toast.makeText(MainActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                    } else {
                        dialogAddUrl.dismiss();
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType","URL");
                        intent.putExtra("URL",inputUrl.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddUrl.dismiss();
                }
            });
        }
        dialogAddUrl.show();
    }
}
