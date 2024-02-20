import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import android.content.Context
import android.os.Environment
import com.google.firebase.firestore.FieldValue
import org.json.JSONObject
import java.io.File
import java.io.FileWriter




class ConexionFirebase {

    val db = Firebase.firestore


    fun LeerDatos(coleccion: String, campo: String, valor: String, context: Context) {

        val docRef = db.collection(coleccion).whereEqualTo(campo, valor)
        docRef.get()
            .addOnSuccessListener { result ->
                val datosDocumentos = result.documents.map { it.data } // Obtener los datos de cada documento
                val datosFormateados = datosDocumentos.joinToString(separator = ",") { it.toString() } // Formatear los datos

                Log.d(TAG, "Datos del documento: $datosFormateados")

                val archivo = File(context.getExternalFilesDir(null), "$coleccion.json")
                val archivoEscritor = FileWriter(archivo)
                val rutaArchivo = archivo.path

                archivoEscritor.write(datosFormateados) // Escribir los datos formateados en el archivo
                archivoEscritor.close()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    fun PostData(jsonData: String) {
        try {
            // Parsear el JSON para obtener la colección, el documento y los campos
            val jsonObject = JSONObject(jsonData)
            val coleccion = jsonObject.getString("coleccion") ?: throw Exception("Colección no especificada")
            val documento = jsonObject.getString("documento") ?: throw Exception("Documento no especificado")
            val campos = HashMap<String, Any>()
            for (key in jsonObject.keys()) {
                if (key != "coleccion" && key != "documento") {
                    campos[key] = jsonObject.get(key)
                }
            }

            // Crear una referencia al documento
            val documentReference = db.collection(coleccion).document(documento)

            // Actualizar los campos del documento
            documentReference.set(campos)
                .addOnSuccessListener {
                    Log.d(TAG, "Documento actualizado con éxito")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al actualizar el documento", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar el JSON", e)
        }
    }


    fun DeleteData(coleccion: String, documento: String, campos: List<String>) {
        val docRef = db.collection(coleccion).document(documento)

        // Crea un mapa para almacenar los campos a eliminar
        val updates = HashMap<String, Any>()
        for (campo in campos) {
            updates[campo] = FieldValue.delete()
        }

        docRef.update(updates).addOnCompleteListener { }
    }



}