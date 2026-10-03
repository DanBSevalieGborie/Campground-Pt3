package com.codepath.lab6

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codepath.lab6.databinding.ItemParkBinding

/**
 * Connects the park cards (item_park.xml) to the list of parks.
 *
 * It's a ListAdapter so that when the search filter changes the list,
 * DiffUtil works out which cards were added or removed and animates them.
 */
class ParksAdapter(
    private val onParkClicked: (NationalPark) -> Unit
) : ListAdapter<NationalPark, ParksAdapter.ViewHolder>(ParkDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemParkBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(park: NationalPark) {
            binding.parkName.text = park.fullName
            binding.parkDescription.text = park.description

            binding.parkState.text = park.formattedStateCodes
            binding.parkState.isVisible = park.stateCodes.isNotEmpty()

            // "National Park", "National Monument"...
            binding.parkDesignation.text = park.designation
            binding.parkDesignation.isVisible = park.designation.isNotEmpty()

            // Use the photo's own alt text for screen readers when the API provides it
            binding.parkImage.contentDescription = park.images.firstOrNull()?.altText
                ?.takeIf { it.isNotEmpty() }
                ?: itemView.context.getString(R.string.park_image_description)

            // A null URL shows the placeholder instead of a recycled photo from another card
            Glide.with(itemView)
                .load(park.imageUrl)
                .placeholder(R.drawable.image_placeholder)
                .centerCrop()
                .into(binding.parkImage)

            binding.root.setOnClickListener { onParkClicked(park) }
        }
    }

    private object ParkDiffCallback : DiffUtil.ItemCallback<NationalPark>() {
        override fun areItemsTheSame(oldItem: NationalPark, newItem: NationalPark): Boolean =
            oldItem.parkCode == newItem.parkCode

        override fun areContentsTheSame(oldItem: NationalPark, newItem: NationalPark): Boolean =
            oldItem == newItem
    }
}
