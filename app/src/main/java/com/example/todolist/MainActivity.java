package com.example.todolist;

import android.app.*;
import android.content.*;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    EditText taskInput;
    Button addTaskBtn, timeBtn, soundBtn, summaryBtn, exportBtn;
    ListView taskList;
    TextView summaryText, progressText;
    ProgressBar progressBar;

    ArrayList<Task> allTasks = new ArrayList<>();
    ArrayList<Task> todayTasks = new ArrayList<>();
    TaskAdapter adapter;

    long selectedTimeMillis = 0;
    String selectedSoundUri = null;

    SharedPreferences prefs;
    static final String PREF_NAME = "todo_prefs";
    static final String TASK_KEY = "tasks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        createNotificationChannel();

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        allTasks = loadTasks();
        filterTodayTasks();

        adapter = new TaskAdapter(this, todayTasks, this::saveAndUpdate);
        taskList.setAdapter(adapter);

        addTaskBtn.setOnClickListener(v -> addTask());
        timeBtn.setOnClickListener(v -> pickTime());
        soundBtn.setOnClickListener(v -> pickSound());
        summaryBtn.setOnClickListener(v -> showSummary());
        exportBtn.setOnClickListener(v -> exportSummary());

        updateSummary();
    }

    void bindViews() {
        taskInput = findViewById(R.id.taskInput);
        addTaskBtn = findViewById(R.id.addTaskBtn);
        timeBtn = findViewById(R.id.timeBtn);
        soundBtn = findViewById(R.id.soundBtn);
        summaryBtn = findViewById(R.id.summaryBtn);
        exportBtn = findViewById(R.id.exportBtn);
        taskList = findViewById(R.id.taskList);
        summaryText = findViewById(R.id.summaryText);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
    }

    void addTask() {
        String text = taskInput.getText().toString().trim();
        if (text.isEmpty() || selectedTimeMillis == 0) {
            Toast.makeText(this, "Set task & time", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(text, false, today(), selectedTimeMillis, selectedSoundUri);
        allTasks.add(task);
        todayTasks.add(task);
        scheduleAlarm(task);
        addToCalendar(task);

        taskInput.setText("");
        selectedTimeMillis = 0;
        selectedSoundUri = null;

        saveAndUpdate();
    }

    void pickTime() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (v, h, m) -> {
            c.set(Calendar.HOUR_OF_DAY, h);
            c.set(Calendar.MINUTE, m);
            c.set(Calendar.SECOND, 0);
            selectedTimeMillis = c.getTimeInMillis();
            Toast.makeText(this, "Time set", Toast.LENGTH_SHORT).show();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    void pickSound() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        startActivityForResult(i, 101);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 101 && res == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) selectedSoundUri = uri.toString();
        }
    }

    void scheduleAlarm(Task task) {
        Intent i = new Intent(this, AlarmReceiver.class);
        i.putExtra("task", task.title);
        i.putExtra("sound", task.soundUri);

        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, task.timeMillis, pi);
    }

    void addToCalendar(Task task) {
        Intent i = new Intent(Intent.ACTION_INSERT);
        i.setData(android.provider.CalendarContract.Events.CONTENT_URI);
        i.putExtra(android.provider.CalendarContract.Events.TITLE, task.title);
        i.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.timeMillis);
        i.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, task.timeMillis + 1800000);
        startActivity(i);
    }

    void saveAndUpdate() {
        saveTasks();
        updateSummary();
        adapter.notifyDataSetChanged();
    }

    void updateSummary() {
        int total = todayTasks.size();
        int done = 0;
        for (Task t : todayTasks) if (t.isCompleted) done++;

        int percent = total == 0 ? 0 : (done * 100 / total);
        summaryText.setText("Total: " + total + " | Done: " + done + " | Pending: " + (total - done));
        progressBar.setProgress(percent);
        progressText.setText("Completion: " + percent + "%");
    }

    void showSummary() {
        new AlertDialog.Builder(this)
                .setTitle("Today's Summary")
                .setMessage(summaryText.getText())
                .setPositiveButton("OK", null)
                .show();
    }

    void exportSummary() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, summaryText.getText());
        startActivity(Intent.createChooser(i, "Share"));
    }

    String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    void filterTodayTasks() {
        for (Task t : allTasks)
            if (t.date.equals(today())) todayTasks.add(t);
    }

    void saveTasks() {
        JSONArray arr = new JSONArray();
        try {
            for (Task t : allTasks) {
                JSONObject o = new JSONObject();
                o.put("title", t.title);
                o.put("completed", t.isCompleted);
                o.put("date", t.date);
                o.put("time", t.timeMillis);
                o.put("sound", t.soundUri);
                arr.put(o);
            }
            prefs.edit().putString(TASK_KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    ArrayList<Task> loadTasks() {
        ArrayList<Task> list = new ArrayList<>();
        try {
            String json = prefs.getString(TASK_KEY, null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new Task(
                            o.getString("title"),
                            o.getBoolean("completed"),
                            o.getString("date"),
                            o.getLong("time"),
                            o.optString("sound", null)
                    ));
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "task_channel",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
