package com.example.actividad5.repository

import android.content.Context
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
                    appContext.contentResolver.delete(uri, null, null)
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
}