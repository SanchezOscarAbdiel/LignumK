package com.example.lignumk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation

class CarouselAdapter(private val images: List<String>, private val text: String):
    RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        return CarouselViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.image_item, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val imageUrl = images[position]
        val imageText = text // Reemplaza esto con el texto que quieras mostrar
        holder.bind(imageUrl, imageText)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class CarouselViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val carouselImageView: AppCompatImageView = view.findViewById(R.id.carouselImageView)
        private val imageTextView: AppCompatTextView = view.findViewById(R.id.imageText)

        fun bind(imageUrl:String, imageText:String){
            carouselImageView.load(imageUrl){
                transformations(RoundedCornersTransformation(8f))
            }
            imageTextView.text = imageText
        }
    }

}