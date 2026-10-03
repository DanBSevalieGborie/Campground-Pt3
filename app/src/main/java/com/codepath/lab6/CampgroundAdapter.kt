package com.codepath.lab6

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codepath.lab6.databinding.ItemCampgroundBinding

/**
 * Connects the campground cards (item_campground.xml) to the list of campgrounds.
 *
 * Same card style as the Parks tab: a big photo with the nightly price on it,
 * the campground type and number of sites, the name, a short description and
 * the town it's in.
 */
class CampgroundAdapter(
    private val onCampgroundClicked: (Campground) -> Unit
) : ListAdapter<Campground, CampgroundAdapter.ViewHolder>(CampgroundDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCampgroundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCampgroundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(campground: Campground) {
            val context = itemView.context

            binding.campgroundName.text = campground.name
            binding.campgroundDescription.text = campground.description

            // "DEVELOPED CAMPGROUND · 34 SITES"
            val type = campground.classification.ifEmpty { context.getString(R.string.campground) }
            binding.campgroundOverline.text = if (campground.totalSites > 0) {
                context.resources.getQuantityString(
                    R.plurals.campground_type_and_sites, campground.totalSites, type, campground.totalSites
                )
            } else {
                type
            }

            binding.campgroundLocation.text = campground.shortLocation
            binding.campgroundLocation.isVisible = campground.shortLocation.isNotEmpty()

            // "$8 / night" or "Free" on top of the photo
            val fee = campground.fee
            binding.campgroundPrice.isVisible = fee != null
            if (fee != null) {
                binding.campgroundPrice.text = if (fee.isFree) {
                    fee.compactCost
                } else {
                    context.getString(R.string.price_per_night, fee.compactCost)
                }
            }

            binding.campgroundImage.contentDescription = campground.images.firstOrNull()?.altText
                ?.takeIf { it.isNotEmpty() }
                ?: context.getString(R.string.campground_image_description)

            Glide.with(itemView)
                .load(campground.imageUrl)
                .placeholder(R.drawable.image_placeholder)
                .centerCrop()
                .into(binding.campgroundImage)

            binding.root.setOnClickListener { onCampgroundClicked(campground) }
        }
    }

    private object CampgroundDiffCallback : DiffUtil.ItemCallback<Campground>() {
        override fun areItemsTheSame(oldItem: Campground, newItem: Campground): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Campground, newItem: Campground): Boolean =
            oldItem == newItem
    }
}
