package com.example.lignumk

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.lignumk.databinding.FragmentConfiguracionBinding
import com.example.lignumk.databinding.FragmentTiendaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

class Configuracion : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentConfiguracionBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        taskViewModel = ViewModelProvider(activity)[TaskViewModel::class.java]
        fotoPerfil(activity)
        avatares(activity)
        notificaciones(activity)
        setDrawablesColor(activity)
        skins(activity)
    }
    fun setDrawablesColor(activity: FragmentActivity){
        val col = actividades.sharedPref(activity, "Skin", Int::class.java)!!
        val color = ContextCompat.getColor(activity, col)
        binding.textView.backgroundTintList = ColorStateList.valueOf(color)
        binding.textView1.backgroundTintList = ColorStateList.valueOf(color)
        binding.textView2.backgroundTintList = ColorStateList.valueOf(color)
        binding.textView3.backgroundTintList = ColorStateList.valueOf(color)
    }

    data class Item(
        val UID: String,
        val Notificaciones: Boolean
    )
    data class ItemSkin(
        val icono: String,
        val value: Int
    )
    private fun notificaciones(activity: FragmentActivity) {
        val uid = actividades.sharedPref(activity,"UID",String::class.java)
        val json = actividades.leeArchivo(activity,"Usuarios")
        val jsonString = json.toString()
        val listType = object : TypeToken<List<Item>>() {}.type
        val items: List<Item> = Gson().fromJson(jsonString, listType)

        for (item in items){
            if(item.UID == uid){
                binding.SwitchNotificacion.isChecked = item.Notificaciones
                break
            }
        }

        binding.SwitchNotificacion.setOnCheckedChangeListener{_, isChecked ->
            val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireActivity(), AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE)

            if (isChecked) {
                Toast.makeText(activity, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                // Programar la alarma para que se dispare cada 24 horas
                val interval: Long = 24 * 60 * 60 * 1000 // 24 horas en milisegundos
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent)
            } else {
                // Cancelar la alarma
                Toast.makeText(activity, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                alarmManager.cancel(pendingIntent)
            }

            val jsonObject = JSONObject()
            jsonObject.put("coleccion", "Usuarios")
            jsonObject.put("documento", uid)
            jsonObject.put("Notificaciones",binding.SwitchNotificacion.isChecked)
            val jsonDatos = jsonObject.toString()

            cFirebaseA.UpdateData(jsonDatos)

            cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",activity)

            Toast.makeText(activity,"Preferencias actualizadas",Toast.LENGTH_SHORT).show()
        }
    }

    private fun skins(activity: FragmentActivity){
        val skinCompradas = actividades.sharedPref(activity,"SkinCompradas", String::class.java)!!
        val skins = skinCompradas.split(",")

        val json = actividades.leeArchivo(activity,"Tiendaestetica")
        val jsonString = json.toString()
        val listType = object : TypeToken<List<ItemSkin>>() {}.type
        val items: List<ItemSkin> = Gson().fromJson(jsonString, listType)

        // Crear un mapa con las 'value' como claves y los 'icono' como valores
        val itemMap = items.associateBy({ it.value }, { it.icono })

        for(skin in skins){
            val imageButton = ImageButton(activity)
            val fiftyDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, activity.resources.displayMetrics).toInt()
            imageButton.layoutParams = LinearLayout.LayoutParams(fiftyDp, fiftyDp)

            // Verificar si la 'value' existe en el mapa
            val icono = itemMap[skin.toInt()]
            Log.d("A","skin: $skin, icono $icono")
            if (icono != null) {
                // Si existe, cargar la imagen correspondiente en el ImageButton
                Glide.with(activity)
                    .load(icono)
                    .into(imageButton)
            } else {
                imageButton.setBackgroundColor(getResources().getColor(R.color.seed))
            }

            imageButton.setOnClickListener{
                actividades.saveSharedPref(activity,"Skin",skin.toInt())
                Toast.makeText(activity,"Apariencia cambiada! \n reinicia la aplicación", Toast.LENGTH_SHORT).show()
            }

            // Agregar el ImageButton al layout
            binding.linearSkin.addView(imageButton)
        }
    }


    private fun avatares(activity: FragmentActivity) {
        val avatarsComprados = actividades.sharedPref(activity,"AvatarComprados", String::class.java)!!

        // Divide la cadena en URLs individuales
        val urls = avatarsComprados.split(",")

        // Crea un ImageButton para cada URL
        for (url in urls) {
            val imageButton = ImageButton(activity)
            val fiftyDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, activity.resources.displayMetrics).toInt()

            // Establece el tamaño del ImageButton a 50dp
            Log.d("Log","URLS avatares $url")
            imageButton.layoutParams = LinearLayout.LayoutParams(fiftyDp, fiftyDp)

            // Carga la imagen desde la URL
            Glide.with(activity)
                .load(url)
                .into(imageButton)

            imageButton.setOnClickListener(){
                actividades.saveSharedPref(activity,"AvatarActivo",url)
                Toast.makeText(activity,"Avatar cambiado! \nReinicia la aplicacion", Toast.LENGTH_SHORT).show()
            }

            // Añade el ImageButton al layout de tu elección
            binding.linearAvatar.addView(imageButton)


        }
    }

    fun fotoPerfil(activity: FragmentActivity){
        val foto = actividadesMP.sharedPref(activity,"fotoPerfil",String::class.java)
        val tipo = actividadesMP.sharedPref(activity,"tipoFoto",String::class.java)

        if (tipo == "uri"){
            val decodedString = Base64.decode(foto, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            binding.IvFotoConfiguracion.setImageBitmap(decodedByte)

        }else{
            val user = Firebase.auth.currentUser
            user?.let {
                val photoURL = it.photoUrl
                Glide.with(this)
                    .load(photoURL)
                    .into(binding.IvFotoConfiguracion)
            }
        }

        //Setea selector de galeria:
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            binding.IvFotoConfiguracion.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.IvFotoConfiguracion.setImageURI(uri)
            val encodedImage = actividades.uriToBase64(activity,uri!!,"fotoPerfil")
            actividades.saveSharedPref(activity,"fotoPerfil",encodedImage)
            actividades.saveSharedPref(activity,"tipoFoto","uri")

            val jsonObject = JSONObject()
            jsonObject.put("coleccion", "Usuarios")
            jsonObject.put("documento", actividades.sharedPref(activity,"UID",String::class.java))
            jsonObject.put("tipoFoto", actividades.sharedPref(activity,"tipoFoto",String::class.java))
            jsonObject.put("fotoPerfil", actividades.sharedPref(activity,"fotoPerfil",String::class.java))
            val jsonDatos = jsonObject.toString()

            cFirebaseA.UpdateData(jsonDatos)
        }

        binding.IvFotoConfiguracion.setOnClickListener(){
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))


        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as MenuPrincipal).cardUsuario(activity as Context)
    }
}