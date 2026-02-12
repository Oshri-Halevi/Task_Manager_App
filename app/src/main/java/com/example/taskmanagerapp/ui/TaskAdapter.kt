package com.example.taskmanagerapp.ui

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskmanagerapp.R
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.databinding.ItemTaskBinding
import java.util.Date

class TaskAdapter(
    private val onItemClick: (Task) -> Unit,
    private val onCheckChanged: (Task, Boolean) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback) {

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {

            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description

            if (task.dueDate != null) {
                val dateString = DateFormat.getDateFormat(binding.root.context).format(Date(task.dueDate))
                binding.chipDate.visibility = View.VISIBLE
                binding.chipDate.text = dateString
            } else {
                binding.chipDate.visibility = View.GONE
            }

            val colorRes = when (task.priority) {
                2 -> R.color.priority_high   // High
                1 -> R.color.priority_normal // Normal
                else -> R.color.priority_low // Low
            }
            binding.priorityIndicator.background.setTint(
                binding.root.context.getColor(colorRes)
            )

            binding.checkDone.setOnCheckedChangeListener(null)
            binding.checkDone.isChecked = task.isDone
            binding.checkDone.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != task.isDone) {
                    onCheckChanged(task, isChecked)
                }
            }

            if (!task.imageUri.isNullOrEmpty()) {
                binding.taskImage.visibility = View.VISIBLE
                Glide.with(binding.root)
                    .load(task.imageUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.taskImage)
            } else {
                binding.taskImage.visibility = View.GONE
            }

            if (task.isDone) {
                binding.taskTitle.alpha = 0.5f
                binding.taskDescription.alpha = 0.5f
                 binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.taskTitle.alpha = 1f
                binding.taskDescription.alpha = 1f
                 binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.root.setOnClickListener {
                onItemClick(task)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getTaskAt(position: Int): Task {
        return getItem(position)
    }

    private object TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem == newItem
    }
}
