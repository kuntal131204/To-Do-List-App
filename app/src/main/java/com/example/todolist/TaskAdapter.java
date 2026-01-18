package com.example.todolist;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;

public class TaskAdapter extends ArrayAdapter<Task> {

    Runnable saveCallback;

    public TaskAdapter(Context context, ArrayList<Task> tasks, Runnable saveCallback) {
        super(context, 0, tasks);
        this.saveCallback = saveCallback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Task task = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.task_item, parent, false);
        }

        CheckBox checkBox = convertView.findViewById(R.id.taskCheckBox);
        TextView taskText = convertView.findViewById(R.id.taskText);
        ImageButton deleteBtn = convertView.findViewById(R.id.deleteBtn);

        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(task.isCompleted);
        taskText.setText(task.title);

        taskText.setPaintFlags(
                task.isCompleted ?
                        taskText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG :
                        taskText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
        );

        checkBox.setOnCheckedChangeListener((btn, checked) -> {
            task.isCompleted = checked;
            saveCallback.run();
            notifyDataSetChanged();
        });

        deleteBtn.setOnClickListener(v -> {
            remove(task);
            saveCallback.run();
            notifyDataSetChanged();
        });

        return convertView;
    }
}
