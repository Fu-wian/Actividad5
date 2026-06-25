package com.example.actividad5.ui

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.actividad5.R
import com.example.actividad5.data.Entrada
import com.example.actividad5.viemodels.DiarioViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "com.utp.diariomultimedia.EXTRA_ID"
    }

    private val vm: DiarioViewModel by viewModels()

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progresoRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id == -1L) {
            finish()
            return
        }

        vm.cargarPorId(id)

        lifecycleScope.launch {
            vm.entradaSeleccionada.collect { entrada ->
                entrada ?: return@collect
                mostrar(entrada)
            }
        }
    }

    private fun mostrar(entrada: Entrada) {
        val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
        supportActionBar?.title = entrada.titulo

        findViewById<TextView>(R.id.tvTituloDetalle).text = entrada.titulo
        findViewById<TextView>(R.id.tvFechaDetalle).text = sdf.format(Date(entrada.fecha))

        // ── Foto ──────────────────────────────────────────────────────────
        val imgFoto = findViewById<ImageView>(R.id.imgFotoDetalle)
        val sectionFoto = findViewById<View>(R.id.sectionFoto)

        if (entrada.rutaFoto != null) {
            sectionFoto.visibility = View.VISIBLE

            Glide.with(this)
                .load(Uri.parse(entrada.rutaFoto))
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imgFoto)
        } else {
            sectionFoto.visibility = View.GONE
        }

        // ── Audio ─────────────────────────────────────────────────────────
        val sectionAudio = findViewById<View>(R.id.sectionAudio)
        val btnPlay = findViewById<Button>(R.id.btnReproducirAudio)
        val seekBarAudio = findViewById<SeekBar>(R.id.seekBarAudio)
        val tvTiempoAudio = findViewById<TextView>(R.id.tvTiempoAudio)

        if (entrada.rutaAudio != null) {
            sectionAudio.visibility = View.VISIBLE

            val rutaAudio = entrada.rutaAudio

            btnPlay.setOnClickListener {
                if (player?.isPlaying == true) {
                    player?.pause()
                    detenerActualizacionProgreso()
                    btnPlay.text = "▶ Reproducir audio"
                } else {
                    if (player != null) {
                        player?.start()
                        iniciarActualizacionProgreso(seekBarAudio, tvTiempoAudio)
                        btnPlay.text = "⏸ Pausar audio"
                    } else {
                        reproducirAudio(
                            rutaAudio,
                            btnPlay,
                            seekBarAudio,
                            tvTiempoAudio
                        )
                    }
                }
            }

            seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        player?.seekTo(progress)

                        val duracion = player?.duration ?: 0
                        tvTiempoAudio.text =
                            "${formatearTiempo(progress)} / ${formatearTiempo(duracion)}"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

        } else {
            sectionAudio.visibility = View.GONE
        }

        // ── Video ─────────────────────────────────────────────────────────
        val sectionVideo = findViewById<View>(R.id.sectionVideo)
        val videoView = findViewById<VideoView>(R.id.videoViewDetalle)

        if (entrada.rutaVideo != null) {
            sectionVideo.visibility = View.VISIBLE

            val mc = MediaController(this)
            mc.setAnchorView(videoView)

            videoView.setMediaController(mc)
            videoView.setVideoURI(Uri.parse(entrada.rutaVideo))

            videoView.setOnCompletionListener {
                Toast.makeText(this, "Video terminado", Toast.LENGTH_SHORT).show()
            }
        } else {
            sectionVideo.visibility = View.GONE
        }
    }


    // ── Funciones ─────────────────────────────────────────────────────────

    private fun reproducirAudio(
        ruta: String,
        btnPlay: Button,
        seekBarAudio: SeekBar,
        tvTiempoAudio: TextView
    ) {
        player?.release()

        player = MediaPlayer().apply {
            setDataSource(ruta)
            prepare()
            start()
        }

        val duracion = player?.duration ?: 0

        seekBarAudio.max = duracion
        seekBarAudio.progress = 0

        tvTiempoAudio.text =
            "${formatearTiempo(0)} / ${formatearTiempo(duracion)}"

        btnPlay.text = "⏸ Pausar audio"

        iniciarActualizacionProgreso(seekBarAudio, tvTiempoAudio)

        player?.setOnCompletionListener {
            val duracionFinal = it.duration

            detenerActualizacionProgreso()

            seekBarAudio.progress = 0
            tvTiempoAudio.text =
                "${formatearTiempo(0)} / ${formatearTiempo(duracionFinal)}"

            it.release()
            player = null

            btnPlay.text = "▶ Reproducir audio"
        }
    }

    private fun iniciarActualizacionProgreso(
        seekBarAudio: SeekBar,
        tvTiempoAudio: TextView
    ) {
        detenerActualizacionProgreso()

        progresoRunnable = object : Runnable {
            override fun run() {
                val reproductor = player ?: return

                val posicionActual = reproductor.currentPosition
                val duracionTotal = reproductor.duration

                seekBarAudio.max = duracionTotal
                seekBarAudio.progress = posicionActual

                tvTiempoAudio.text =
                    "${formatearTiempo(posicionActual)} / ${formatearTiempo(duracionTotal)}"

                if (reproductor.isPlaying) {
                    handler.postDelayed(this, 500)
                }
            }
        }

        handler.post(progresoRunnable!!)
    }

    private fun detenerActualizacionProgreso() {
        progresoRunnable?.let {
            handler.removeCallbacks(it)
        }

        progresoRunnable = null
    }

    private fun formatearTiempo(milisegundos: Int): String {
        val segundosTotales = milisegundos / 1000
        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60

        return String.format(
            Locale.getDefault(),
            "%d:%02d",
            minutos,
            segundos
        )
    }

    override fun onStop() {
        super.onStop()

        detenerActualizacionProgreso()

        player?.apply {
            stop()
            release()
        }

        player = null

        findViewById<VideoView>(R.id.videoViewDetalle).stopPlayback()
    }
}