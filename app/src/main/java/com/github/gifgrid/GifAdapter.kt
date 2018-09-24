package com.github.gifgrid

import android.app.Activity
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.widget.CircularProgressDrawable
import android.support.v7.content.res.AppCompatResources
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.gifgrid.favorite.FavoriteManager
import com.github.gifgrid.trending.Gif

class GifAdapter(
        private val activity: Activity,
        private val favoriteManager: FavoriteManager
) : PagedListAdapter<Gif, GifLayoutViewHolder>(DiffCallback(favoriteManager)) {

    private val cellSize: Int = getCellSize()
    private val layoutInflater: LayoutInflater = activity.layoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifLayoutViewHolder {
        val view = layoutInflater.inflate(R.layout.gif_layout, parent, false)
        view.layoutParams = view.layoutParams.apply {
            height = cellSize
            width = cellSize
        }
        val holder = GifLayoutViewHolder(view)
        holder.favoriteButton.setOnClickListener {
            getItem(holder.adapterPosition)?.apply {
                holder.favoriteButton.setImageDrawable(createProgressDrawable(holder.favoriteButton.context, true))
                favoriteManager.setFavorite(this, favoriteManager.isFavorite(this).not())
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: GifLayoutViewHolder, position: Int) {
        val gif = getItem(position)
        Glide.with(activity).clear(holder.imageView)
        holder.favoriteButton.visibility = View.GONE
        gif?.apply {
            val isFavorite = favoriteManager.isFavorite(this)
            holder.favoriteButton.setImageDrawable(createFavoriteDrawable(holder.favoriteButton.context))
            holder.favoriteButton.isSelected = isFavorite
            holder.imageView.setImageDrawable(null)
            Glide.with(activity)
                    .asGif()
                    .load(if (isFavorite) favoriteManager.getLinkForFavorite(this) else this.getMediaLink())
                    .apply(RequestOptions.placeholderOf(createProgressDrawable(holder.imageView.context)))
                    .addListener(getRequestListener(holder.favoriteButton))
                    .into(holder.imageView)
        }
    }

    private fun getRequestListener(view: View): RequestListener<GifDrawable?> =
            object : RequestListener<GifDrawable?> {
                override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean): Boolean {
                    view.visibility = View.VISIBLE
                    return false
                }
            }

    private fun createFavoriteDrawable(context: Context): Drawable {
        return AppCompatResources.getDrawable(
                context,
                R.drawable.favorite
        )!!
    }

    private fun createProgressDrawable(
            context: Context,
            isSmall: Boolean = false
    ) = CircularProgressDrawable(context)
            .apply {
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                strokeWidth = context.resources.getDimensionPixelSize(R.dimen.stroke).toFloat()
                centerRadius = context.resources.getDimensionPixelSize(
                        if (isSmall) R.dimen.radius_small
                        else R.dimen.radius).toFloat()
                start()
            }

    private fun getCellSize(): Int {
        return activity.let {
            val metrics = DisplayMetrics()
            it.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels / 2
        }
    }
}

class DiffCallback(
        private val favoriteManager: FavoriteManager
) : DiffUtil.ItemCallback<Gif>() {
    override fun areItemsTheSame(oldItem: Gif?, newItem: Gif?): Boolean {
        return oldItem?.id == newItem?.id
    }

    override fun areContentsTheSame(oldItem: Gif?, newItem: Gif?): Boolean {
        return if (oldItem != null && newItem != null) {
            favoriteManager.isFavorite(oldItem) == favoriteManager.isFavorite(newItem)
        } else {
            oldItem?.id == newItem?.id
        }
    }
}

class GifLayoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.findViewById(R.id.image_view)
    val favoriteButton: ImageView = view.findViewById(R.id.favorite_button)
}