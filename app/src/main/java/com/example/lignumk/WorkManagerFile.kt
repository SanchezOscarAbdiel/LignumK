// Importar la biblioteca de work manager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.io.File
import java.io.FileReader
import org.json.JSONObject
import kotlin.random.Random

// Extender la clase Worker
class WorkManagerFile(context: Context, params: WorkerParameters) : Worker(context, params) {

    val cFirebase = ConexionFirebase()
    // Sobrescribir el método doWork
    override fun doWork(): Result {

        val parametro = inputData.getString("parametro")
        // Usar un ciclo when para seleccionar la función
        when (parametro) {
            "LeerTareas" -> LeerTareas()
            "EscribirTareas" -> EscribirTareas()
            "BorrarTareas" -> BorrarTareas()
            else -> Log.d("MiWorker", "Parámetro inválido")
        }
        return Result.success()
    }

    fun LeerTareas() {
        cFirebase.LeerDatos("Tareas", "tipo", "diaria", applicationContext)
        val archivo = File(applicationContext.getExternalFilesDir(null), "Tareas.json")
        val archivoLector = FileReader(archivo)
        val contenido = archivoLector.readText()

        val json = JSONArray(contenido)
        Log.d("LeerTareas", "Contenido en LeerTareas${json}")

        // Generar un número aleatorio entre 0 y el tamaño del arreglo menos uno
        val indice = Random.nextInt(0, json.length())
        // Obtener el elemento del arreglo json usando el índice
        val elemento = json.getJSONObject(indice)
        // Hacer algo con el elemento, por ejemplo, imprimirlo
        Log.d("LeerTareas", "Elemento al azar en LeerTareas${elemento.get("descripcion")}")

    }

    fun EscribirTareas() {
        // Aquí puedes escribir el código para escribir las tareas
    }

    fun BorrarTareas() {
        // Aquí puedes escribir el código para borrar las tareas
    }
}
