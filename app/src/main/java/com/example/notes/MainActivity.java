package com.example.notes;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Uri> fileUris = new ArrayList<>();
    private ArrayList<String> notesList = new ArrayList<>();
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    private NotesAdapter adapter;
    private ActivityResultLauncher<Intent> noteEditorLauncher;
    private RecyclerView recyclerView;
    private Button newNotesButton;
    private Button importNotesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewNotes);
        newNotesButton = findViewById(R.id.buttonio);
        importNotesButton = findViewById(R.id.import_button);

        adapter = new NotesAdapter(notesList, this::onNoteClick, this::onDeleteClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadNotesFromStorage();

        noteEditorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String noteText = data.getStringExtra("NOTE_TEXT");
                            int position = data.getIntExtra("NOTE_POSITION", -1);
                            saveNote(noteText, position);
                        }
                    }
                });

        openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        String content = readTextFromUri(uri);
                        if (content != null) {
                            saveNoteAndOpen(content);
                        }
                    }
                });

        newNotesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            noteEditorLauncher.launch(intent);
        });

        importNotesButton.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"text/plain"}));
    }

    public void onNoteClick(int position) {
        String existingNotes = notesList.get(position);
        Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
        intent.putExtra("NOTE_TEXT", existingNotes);
        intent.putExtra("NOTE_POSITION", position);
        noteEditorLauncher.launch(intent);
    }

    public void onDeleteClick(int position) {
        try {
            Uri uriToDelete = fileUris.get(position);
            ContentResolver contentResolver = getContentResolver();
            int rowsDeleted = contentResolver.delete(uriToDelete, null, null);

            if (rowsDeleted > 0) {
                notesList.remove(position);
                fileUris.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, notesList.size());
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error deleting note", e);
            Toast.makeText(this, "Error deleting note", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotesFromStorage() {
        notesList.clear();
        fileUris.clear();

        Uri collection = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME};
        String selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ? AND " + MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
        String[] selectionArgs = new String[]{"%Documents%", "text/plain"};

        try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), String.valueOf(id));
                    String content = readTextFromUri(contentUri);
                    if (content != null) {
                        notesList.add(content);
                        fileUris.add(contentUri);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveNote(String text, int position) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "note_" + System.currentTimeMillis() + ".txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/");

        Uri uri;
        if (position == -1) {
            uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        } else {
            uri = fileUris.get(position);
        }

        if (uri != null) {
            try (OutputStream os = getContentResolver().openOutputStream(uri, "w")) { // Use "w" for write mode
                if (os != null) {
                    os.write(text.getBytes());
                    Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();

                    if (position == -1) {
                        notesList.add(text);
                        fileUris.add(uri);
                        adapter.notifyItemInserted(notesList.size() - 1);
                    } else {
                        notesList.set(position, text);
                        adapter.notifyItemChanged(position);
                    }
                }
            } catch (IOException e) {
                Log.e("MainActivity", "Error saving note", e);
                Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveNoteAndOpen(String text) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "note_" + System.currentTimeMillis() + ".txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/");

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        if (uri != null) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    os.write(text.getBytes());
                    Toast.makeText(this, "Note imported!", Toast.LENGTH_SHORT).show();

                    notesList.add(text);
                    fileUris.add(uri);
                    int newPosition = notesList.size() - 1;
                    adapter.notifyItemInserted(newPosition);
                    
                    onNoteClick(newPosition);
                }
            } catch (IOException e) {
                Log.e("MainActivity", "Error saving note", e);
                Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String readTextFromUri(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Error reading from URI", e);
            return null;
        }
        return stringBuilder.toString().trim();
    }
}
