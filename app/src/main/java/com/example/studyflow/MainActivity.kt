package com.example.studyflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Proyecto(val id: Int, val nombre: String)
data class Tarea(val proyecto: Int, val titulo: String, val responsable: String)
data class Panel(val proyectos: List<Proyecto>, val tareas: List<Tarea>)
interface StudyApi {
    @GET("api/libros") suspend fun libros(@retrofit2.http.Query("consulta") consulta: String): List<String>
    @GET("api/resumen") suspend fun resumen(): List<String>
    @GET("api/panel") suspend fun panel(): Panel
}
class StudyModel : ViewModel() {
    var panel by mutableStateOf(Panel(listOf(Proyecto(1,"Exposición de arquitectura")), listOf(Tarea(1,"Preparar diapositivas","Ángel"), Tarea(1,"Ensayar exposición","Equipo"))))
        private set
    var estado by mutableStateOf("Datos de ejemplo locales")
        private set
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create()).build().create(StudyApi::class.java)
    var resultados by mutableStateOf(listOf<String>())
        private set
    fun consultar(consulta: String?) = viewModelScope.launch {
        estado = "Consultando…"
        resultados = emptyList()
        try {
            resultados = if (consulta == null) api.resumen() else api.libros(consulta)
            estado = "${resultados.size} resultados"
        } catch (e: java.io.IOException) {
            estado = "Revisa la conexión con la API"
        } catch (e: retrofit2.HttpException) {
            estado = "Servicio no disponible (HTTP ${e.code()})"
        }
    }
    fun actualizar() = viewModelScope.launch {
        estado = "Consultando API…"
        try {
            panel = api.panel()
            estado = "Datos recibidos de la API"
        } catch (e: java.io.IOException) {
            estado = "API sin conexión. Se conservan los datos visibles."
        } catch (e: retrofit2.HttpException) {
            estado = "La API respondió con error ${e.code()}"
        }
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xff413887), secondary = Color(0xff087e80))) { StudyFlow() } }
    }
}
@Composable
fun StudyFlow(model: StudyModel = viewModel()) {
    var vista by rememberSaveable { mutableStateOf("Inicio") }
    var elegido by rememberSaveable { mutableIntStateOf(1) }
    val proyecto = model.panel.proyectos.find { it.id == elegido }
    androidx.activity.compose.BackHandler(vista != "Inicio") { vista = "Inicio" }
    Scaffold(bottomBar = {
        NavigationBar { listOf("Inicio","Proyecto","Herramientas").forEachIndexed { i, nombre ->
            NavigationBarItem(selected = vista == nombre, onClick = { vista = nombre }, icon = { Text(listOf("⌂","▤","⚙")[i]) }, label = { Text(nombre) })
        } }
    }) { inset ->
        Column(Modifier.padding(inset).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("StudyFlow", color = MaterialTheme.colorScheme.primary)
            Text(when(vista) { "Inicio" -> "Mis proyectos"; "Proyecto" -> proyecto?.nombre ?: "Detalle"; else -> "Herramientas" }, style = MaterialTheme.typography.headlineMedium)
            when(vista) {
                "Inicio" -> {
                    Text("${model.panel.tareas.size} tareas pendientes")
                    model.panel.proyectos.forEach { p ->
                        ElevatedCard(onClick = { elegido = p.id; vista = "Proyecto" }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp)) { Text(p.nombre); Text("Ver tareas y responsables →") }
                        }
                    }
                }
                "Proyecto" -> {
                    Text("Tareas del equipo")
                    model.panel.tareas.filter { it.proyecto == elegido }.forEach { tarea ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(tarea.titulo); Text(tarea.responsable, style = MaterialTheme.typography.bodySmall) } }
                    }
                }
                else -> {
                    Text(model.estado)
                    Button(onClick = { model.actualizar() }) { Text("Actualizar desde la API") }
                    OutlinedButton(onClick = { model.consultar(null) }) { Text("Leer resumen de Sheets") }
                    Text("Apps Script · resumen de pendientes")
                    var consulta by rememberSaveable { mutableStateOf("") }
                    OutlinedTextField(value = consulta, onValueChange = { consulta = it }, label = { Text("Tema de bibliografía") })
                    Button(enabled = consulta.trim().length in 2..100, onClick = { model.consultar(consulta.trim()) }) { Text("Buscar en Open Library") }
                    model.resultados.forEach { Text(it) }
                }
            }
        }
    }
}