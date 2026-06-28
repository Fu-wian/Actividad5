package com.example.actividad5.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import com.example.actividad5.data.DiarioDatabase
import com.example.actividad5.data.Entrada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EntradaRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = DiarioDatabase.getInstance(appContext).entradaDao()

    suspend fun getAll(): List<Entrada> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun getById(id: Long): Entrada? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun insertar(entrada: Entrada): Long = withContext(Dispatchers.IO) {
        dao.insertar(entrada)
    }

    suspend fun eliminar(entrada: Entrada) = withContext(Dispatchers.IO) {
        borrarArchivoSiExiste(entrada.rutaAudio)
        borrarArchivoSiExiste(entrada.rutaFoto)
        borrarArchivoSiExiste(entrada.rutaVideo)

        dao.eliminar(entrada)
    }

    private fun borrarArchivoSiExiste(ruta: String?) {
        if (ruta.isNullOrBlank()) return

        try {
            val uri = ruta.toUri()

            when (uri.scheme) {
                "content" -> {
                    val archivo = resolverArchivoDesdeContentUri(uri)
                    if (archivo != null && archivo.exists()) {
                        archivo.delete()
                    }
                }
                "file" -> {
                    val archivo = File(uri.path ?: return)
                    if (archivo.exists()) {
                        archivo.delete()
                    }
                }
                else -> {
                    val archivo = File(ruta)
                    if (archivo.exists()) {
                        archivo.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resolverArchivoDesdeContentUri(uri: Uri): File? {
        return try {
            val ultimoSegmento = uri.lastPathSegment ?: return null
            val nombreArchivo = File(ultimoSegmento).name

            val carpeta = when {
                uri.pathSegments.contains("pictures") ->
                    appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                uri.pathSegments.contains("movies") ->
                    appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                else -> null
            } ?: return null

            val candidatos = listOf(
                File(carpeta, nombreArchivo),
                File(carpeta, "importadas/$nombreArchivo")
            )

            candidatos.firstOrNull { it.exists() }
        } catch (e: Exception) {
            null
        }
    }
}