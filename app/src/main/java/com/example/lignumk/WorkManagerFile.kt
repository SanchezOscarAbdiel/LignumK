// Importar la biblioteca de work manager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.lignumk.Actividades
import org.json.JSONArray
import java.io.File
import java.io.FileReader
import org.json.JSONObject
import kotlin.random.Random

// Extender la clase Worker
class WorkManagerFile(context: Context, params: WorkerParameters) : Worker(context, params) {

    val act = Actividades()
    // Sobrescribir el método doWork
    override fun doWork(): Result {

        val parametro = inputData.getString("parametro")
        // Usar un ciclo when para seleccionar la función
        when (parametro) {
            "AsignarTareas" -> act.AsignarTareas(applicationContext)
            "EstablecerCiclo" -> act.periodicRTareas(applicationContext)
            /*"EscribirTareas" -> EscribirTareas()
            "BorrarTareas" -> BorrarTareas()*/
            else -> Log.d("MiWorker", "Parámetro inválido")
        }
        return Result.success()
    }


}
