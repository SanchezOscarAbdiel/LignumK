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
import android.graphics.BitmapFactory
import android.util.Base64

class CarouselAdapterLeader(private val imagesWithRacha: List<Pair<String, Int>>):
    RecyclerView.Adapter<CarouselAdapterLeader.CarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        return CarouselViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.image_item, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val (imageData, racha) = imagesWithRacha[position]

        // Verificar si la imagen es una URL o una cadena base64
        if (imageData.startsWith("http")) {
            // La imagen es una URL, cargarla directamente
            holder.bindUrl(imageData, racha)
        } else {
            // La imagen es una cadena base64, decodificarla y luego cargarla
            holder.bindBase64(imageData, racha)
        }
    }

    override fun getItemCount(): Int {
        return imagesWithRacha.size
    }

    inner class CarouselViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val carouselImageView: AppCompatImageView = view.findViewById(R.id.carouselImageView)
        private val imageTextView: AppCompatTextView = view.findViewById(R.id.imageText)

        fun bindUrl(imageUrl:String, racha:Int){
            carouselImageView.load(imageUrl){
                transformations(RoundedCornersTransformation(8f))
            }
            imageTextView.text = "Racha: $racha dias!"
        }

        fun bindBase64(encodedImage:String, racha:Int){
            val decodedByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)

            carouselImageView.setImageBitmap(bitmap)
            imageTextView.text = "Racha: $racha dias!"
        }
    }

}