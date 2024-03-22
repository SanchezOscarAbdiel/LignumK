package com.example.lignumk

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
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
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
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
        setItems(activity)
    }
    data class Item(
        val icono: String,
        val descripcion: String,
        val precio: String,
        val tipo: String,
        val nombre: String,
        val subtipo: String
    )

    fun setItems(activity: FragmentActivity){
        val jsonEstetica = actividades.leeArchivo(activity,"Tiendaestetica")
        val jsonEsteticaString = jsonEstetica.toString()
        val listType = object : TypeToken<List<Item>>() {}.type
        val items: List<Item> = Gson().fromJson(jsonEsteticaString, listType)

        // Crear ImageButtons para cada elemento en el JSON
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
            Log.d("AvatarsComprados", "Avatars comprados = $avatarsComprados, \n item.icon: ${item.icono}")
            if (avatarsComprados?.contains(item.icono) == true) {
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

                                        Toast.makeText(activity,"Item comprado! \nRevisa la configuración",Toast.LENGTH_LONG).show()
                                    }
                                    "estetica" -> {

                                    }
                                }
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