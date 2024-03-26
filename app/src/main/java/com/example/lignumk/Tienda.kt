package com.example.lignumk

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.lignumk.databinding.FragmentTiendaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject


val actividades = Actividades()

class Tienda : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTiendaBinding
    private lateinit var taskViewModel: TaskViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        taskViewModel = ViewModelProvider(activity)[TaskViewModel::class.java]

        //Boton
        cFirebase.LeerDatos("Tienda", "tipo", "potenciador", activity)
        cFirebase.LeerDatos("Tienda", "tipo", "estetica", activity)

        setDrawablesColor(activity)
        setItems(activity)
        setPotenciadores(activity)
    }


    fun setDrawablesColor(activity: FragmentActivity){
        val col = actividades.sharedPref(activity, "Skin", Int::class.java)!!
        val color = ContextCompat.getColor(activity, col)
        binding.textView1.backgroundTintList = ColorStateList.valueOf(color)
        binding.textView2.backgroundTintList = ColorStateList.valueOf(color)
        binding.textView3.backgroundTintList = ColorStateList.valueOf(color)
    }

    data class Item(
        val icono: String,
        val descripcion: String,
        val precio: String,
        val tipo: String,
        val nombre: String,
        val subtipo: String,
        val value: Int = 0
    )
    data class ItemPotenciador(
        val icono: String,
        val descripcion: String,
        val precio: String,
        val tipo: String,
        val nombre: String,
        val subtipo: String
    )

    fun setPotenciadores(activity: FragmentActivity){

        val jsonEstetica = actividades.leeArchivo(activity,"Tiendapotenciador")
        val jsonEsteticaString = jsonEstetica.toString()
        val listType = object : TypeToken<List<ItemPotenciador>>() {}.type
        val items: List<ItemPotenciador> = Gson().fromJson(jsonEsteticaString, listType)

        for (item in items) {
            val imageButton = ImageButton(activity)
            val fiftyDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                100f,
                activity.resources.displayMetrics
            ).toInt()

            // Establecer el tamaño del ImageButton a 50dp
            imageButton.layoutParams = LinearLayout.LayoutParams(fiftyDp, fiftyDp)

            // Cargar la imagen desde la URL en el JSON
            Glide.with(activity)
                .load(item.icono)
                .into(imageButton)

                val potenciador = actividades.sharedPref(activity, "potenciadorActivo", String::class.java)

                if (potenciador.equals("no")) {
                    imageButton.setOnClickListener {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle(item.nombre)
                            .setMessage(item.descripcion + "\nPrecio: ${item.precio}")
                            .setPositiveButton("Aceptar") { dialog, which ->

                                if (actualizaMonedas(activity, item.precio)) {
                                    actividades.saveSharedPref(
                                        activity,
                                        "tipoPotenciadorActivo",
                                        item.subtipo
                                    )
                                    actividades.saveSharedPref(
                                        activity,
                                        "potenciadorActivo",
                                        item.nombre
                                    )
                                    Toast.makeText(activity,"Potenciador activo!", Toast.LENGTH_SHORT).show()
                                    dismiss()
                                }

                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }else{
                    imageButton.alpha = 0.6F
                    imageButton.setOnClickListener {
                        Toast.makeText(activity,"Ya hay un potenciador activo!",Toast.LENGTH_SHORT).show()
                    }
                }



            binding.miPotenciadorLayout.addView(imageButton)
        }
    }

    fun setItems(activity: FragmentActivity){
        val jsonEstetica = actividades.leeArchivo(activity,"Tiendaestetica")
        val jsonEsteticaString = jsonEstetica.toString()
        val listType = object : TypeToken<List<Item>>() {}.type
        val items: List<Item> = Gson().fromJson(jsonEsteticaString, listType)

        // Crear ImageButtons para cada elemento en el JSON
        for (item in items) {
            val imageButton = ImageButton(activity)
            val fiftyDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, activity.resources.displayMetrics).toInt()

            // Establecer el tamaño del ImageButton a 50dp
            imageButton.layoutParams = LinearLayout.LayoutParams(fiftyDp, fiftyDp)

            // Cargar la imagen desde la URL en el JSON
            Glide.with(activity)
                .load(item.icono)
                .into(imageButton)

            // Verificar si el ítem ya ha sido comprado
            val avatarsComprados = actividades.sharedPref(activity, "AvatarComprados", String::class.java)
            val skinsCompradas = actividades.sharedPref(activity, "SkinCompradas", String::class.java)
            Log.d("Values ","Value extraido: ${item.value}")
            if ((item.subtipo == "avatar" && avatarsComprados?.contains(item.icono) == true) ||
                (item.subtipo == "skin" && skinsCompradas?.contains(item.value.toString()) == true)) {
                // Si el ítem ya ha sido comprado, deshabilitar el botón
                imageButton.alpha = 0.6F
                imageButton.setOnClickListener {
                    Toast.makeText(activity,"Item ya comprado!",Toast.LENGTH_SHORT).show()
                }
            } else {
                // Si el ítem no ha sido comprado, configurar el OnClickListener
                imageButton.setOnClickListener {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(item.nombre)
                        .setMessage(item.descripcion+"\n"+"Precio: ${item.precio}")
                        .setPositiveButton("Comprar") { dialog, which ->
                            if(actualizaMonedas(activity, item.precio)) {
                                when (item.subtipo) {
                                    "avatar" -> {
                                        actividades.saveSharedPref(activity,
                                            "AvatarComprados",
                                            item.icono + "," + actividades.sharedPref(activity,
                                                "AvatarComprados", String::class.java))
                                    }
                                    "skin" -> {
                                        actividades.saveSharedPref(activity,
                                            "SkinCompradas",
                                            item.value.toString() + "," + skinsCompradas)
                                    }
                                }
                                Toast.makeText(activity,"Item comprado! \nRevisa la configuración",Toast.LENGTH_LONG).show()
                            }
                        }
                        .setNegativeButton("Cancelar") { dialog, which ->
                            // Acción para el botón negativo
                        }
                        .show()
                }
            }

            // Agregar el ImageButton al layout de tu BottomSheetDialogFragment
            binding.miLinearLayout.addView(imageButton)
        }

    }

    fun actualizaMonedas(activity: FragmentActivity, precio: String): Boolean{
        val uid = actividades.sharedPref(activity,"UID",String::class.java)

        val json = actividades.leeArchivo(activity,"Usuarios")
        val monedas : Int =  actividades.objetoBuscado(json,uid!!,"monedas",Int::class)?.minus(precio.toInt()) ?: 0

        return if (monedas < 0){
            Toast.makeText(activity,"No tienes suficientes monedas!", Toast.LENGTH_SHORT).show()
            false
        }else{
            val jsonObject = JSONObject()
            jsonObject.put("coleccion", "Usuarios")
            jsonObject.put("documento", uid)
            jsonObject.put("monedas",monedas)
            val jsonDatos = jsonObject.toString()

            cFirebaseA.UpdateData(jsonDatos)

            cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",activity)

            true
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTiendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Recargar la actividad
        (activity as MenuPrincipal).cardUsuario(activity as Context)
    }

}