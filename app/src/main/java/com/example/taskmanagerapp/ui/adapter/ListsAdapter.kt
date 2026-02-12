package com.example.taskmanagerapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagerapp.data.local.ListEntity
import com.example.taskmanagerapp.databinding.ItemListBinding

class ListsAdapter(
    private val onClick: (ListEntity) -> Unit
) : ListAdapter<ListWithCount, ListsAdapter.ListViewHolder>(ListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ListViewHolder(
        private val binding: ItemListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListWithCount) {
            binding.listName.text = item.list.name
            binding.taskCount.text = "${item.taskCount} tasks"
            binding.root.setOnClickListener { onClick(item.list) }
        }
    }
}

data class ListWithCount(
    val list: ListEntity,
    val taskCount: Int
)

class ListDiffCallback : DiffUtil.ItemCallback<ListWithCount>() {
    override fun areItemsTheSame(oldItem: ListWithCount, newItem: ListWithCount) =
        oldItem.list.id == newItem.list.id

    override fun areContentsTheSame(oldItem: ListWithCount, newItem: ListWithCount) =
        oldItem == newItem
}
