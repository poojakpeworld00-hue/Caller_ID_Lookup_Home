package com.callerid.numberlookup.home.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.callerid.numberlookup.home.data.PersonItem
import com.callerid.numberlookup.home.databinding.CellContactBinding

class PersonAdapter(
    private val items: List<PersonItem>
) : RecyclerView.Adapter<PersonAdapter.VH>() {

    inner class VH(val binding: CellContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = CellContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAvatar.text = item.initials
            tvName.text = item.name
            tvNumber.text = item.detail
        }
    }

    override fun getItemCount(): Int = items.size
}
