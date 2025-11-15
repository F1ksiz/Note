package com.example.notes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String NOTE_DIR = "notes";
    private ArrayList<String> filePaths = new ArrayList<>();
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

        loadNotesFromStorage();

        adapter = new NotesAdapter(notesList, this::onNoteClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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
            if (uri != null){
                String content = readTextFromUri(uri);
                if (content != null){
                    saveNote(content, -1);
                }
            }
                });


        newNotesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            noteEditorLauncher.launch(intent);
        });

        importNotesButton.setOnClickListener(v -> openDocumentLauncher.launch(new String [] {"text/plain"}));
    }

    public void onNoteClick(int position) {
        String existingNotes = notesList.get(position);

        Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);

        intent.putExtra("NOTE_TEXT",existingNotes);
        intent.putExtra("NOTE_POSITION",position);

        noteEditorLauncher.launch(intent);
    }
    private void loadNotesFromStorage() {
        File notesDir = new File(getFilesDir(), NOTE_DIR);
        if (!notesDir.exists()) {
            notesDir.mkdirs();
        }
        File[] noteFiles = notesDir.listFiles();
        if (noteFiles != null) {
            for (File file : noteFiles) {
                StringBuilder text = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    notesList.add(text.toString().trim());
                    filePaths.add(file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e("MainActivity", "Error loading note", e);
                }
            }
        }
    }
    private void saveNote(String text, int position) {
        File notesDir = new File(getFilesDir(), NOTE_DIR);
        if (!notesDir.exists()) {
            notesDir.mkdirs();
        }
        File noteFile;

        if (position == - 1) {
            noteFile = new File(notesDir, "note_" + System.currentTimeMillis() + ".txt");
        } else {
            noteFile = new File(filePaths.get(position));
        }
        Log.d("MainActivity", "Saving note to: " + noteFile.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(noteFile)) {
            fos.write(text.getBytes());
            Log.d("MainActivity", "Note saved successfully.");

            if (position == -1) {
                notesList.add(text);
                filePaths.add(noteFile.getAbsolutePath());
                adapter.notifyItemInserted(notesList.size() - 1);
            } else  {
                notesList.set(position, text);
                adapter.notifyItemChanged(position);
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Error saving note", e);
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
