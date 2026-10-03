package com.codepath.lab6

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.codepath.lab6.databinding.ViewPhotoGalleryBinding

/**
 * Fills the swipeable photo gallery (view_photo_gallery.xml) that both detail
 * screens include: the photos, a "3 / 12" counter, and each photo's caption and credit.
 */
fun ViewPhotoGalleryBinding.bind(images: List<NpsImage>) {
    if (images.isEmpty()) {
        root.isVisible = false
        return
    }

    val context = root.context
    photoPager.adapter = PhotoAdapter(images)
    photoCounter.isVisible = images.size > 1

    fun showPhotoInfo(position: Int) {
        val image = images.getOrNull(position) ?: return
        photoCounter.text = context.getString(R.string.photo_counter, position + 1, images.size)

        photoCaption.text = image.caption.ifEmpty { image.title }
        photoCaption.isVisible = photoCaption.text.isNotEmpty()

        photoCredit.text = context.getString(R.string.photo_credit, image.credit)
        photoCredit.isVisible = image.credit.isNotEmpty()
    }

    showPhotoInfo(0)
    photoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = showPhotoInfo(position)
    })
    // After a rotation the pager comes back on the photo it was showing
    photoPager.post { showPhotoInfo(photoPager.currentItem) }
}

/**
 * Feeds the gallery's ViewPager2. ViewPager2 is built on RecyclerView,
 * so this is a regular RecyclerView adapter.
 */
class PhotoAdapter(
    private val images: List<NpsImage>
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val imageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false) as ImageView
        return PhotoViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val image = images[position]

        holder.imageView.contentDescription = image.altText.ifEmpty {
            holder.imageView.context.getString(R.string.photo_description)
        }

        Glide.with(holder.imageView)
            .load(image.url)
            .placeholder(R.drawable.image_placeholder)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size
}
