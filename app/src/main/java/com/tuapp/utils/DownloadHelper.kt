package com.tuapp.utils

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File

/**
 * =========================================================
 * DOWNLOAD HELPER - Gestión de Descargas para Android
 * =========================================================
 * 
 * Uso en tu Activity/Fragment:
 * 
 * 1. Inicializar:
 *    val downloadHelper = DownloadHelper(this)
 *    
 * 2. Configurar WebView:
 *    downloadHelper.setupWebViewDownloads(webView)
 *    
 * 3. Descargar archivo manualmente:
 *    downloadHelper.downloadFile(url, fileName)
 *    
 * 4. En onDestroy:
 *    downloadHelper.unregister()
 * =========================================================
 */
class DownloadHelper(private val context: Context) {

    private var downloadReceiver: BroadcastReceiver? = null
    private var currentDownloadId: Long = -1
    
    // Tu URL base del servidor
    private val baseUrl = "https://buscandoadios-espana.com"
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        
        // Tipos MIME comunes
        private val MIME_TYPES = mapOf(
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "txt" to "text/plain",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "mp3" to "audio/mpeg",
            "mp4" to "video/mp4",
            "zip" to "application/zip",
            "apk" to "application/vnd.android.package-archive"
        )
    }
    
    /**
     * Configura el WebView para manejar descargas automáticamente
     */
    fun setupWebViewDownloads(webView: WebView) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            handleDownload(url, userAgent, contentDisposition, mimeType, contentLength)
        }
    }
    
    /**
     * Maneja la descarga interceptada del WebView
     */
    private fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        // Verificar permisos
        if (!hasStoragePermission()) {
            Toast.makeText(context, "Se necesitan permisos de almacenamiento", Toast.LENGTH_LONG).show()
            return
        }
        
        // Obtener nombre del archivo
        val fileName = extractFileName(url, contentDisposition, mimeType)
        
        // Convertir URL relativa a absoluta si es necesario
        val downloadUrl = if (url.startsWith("http")) url else "$baseUrl$url"
        
        // Usar el endpoint de descarga de la API si no lo usa ya
        val finalUrl = if (downloadUrl.contains("/api/download.php")) {
            downloadUrl
        } else {
            // Convertir a URL de descarga de la API
            val filePath = Uri.parse(downloadUrl).path ?: ""
            "$baseUrl/api/download.php?file=${Uri.encode(filePath)}"
        }
        
        downloadFile(finalUrl, fileName, mimeType, userAgent)
    }
    
    /**
     * Descarga un archivo usando DownloadManager
     */
    fun downloadFile(
        url: String,
        fileName: String,
        mimeType: String? = null,
        userAgent: String? = null
    ): Long {
        if (!hasStoragePermission()) {
            Toast.makeText(context, "Se necesitan permisos de almacenamiento", Toast.LENGTH_LONG).show()
            return -1
        }
        
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // Título y descripción en notificaciones
                setTitle(fileName)
                setDescription("Descargando archivo...")
                
                // Mostrar en notificaciones
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                // Destino del archivo
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                
                // Permitir conexiones medidas (datos móviles)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                
                // Headers
                userAgent?.let { addRequestHeader("User-Agent", it) }
                
                // Cookies del WebView
                val cookies = CookieManager.getInstance().getCookie(url)
                cookies?.let { addRequestHeader("Cookie", it) }
                
                // MIME type
                val finalMimeType = mimeType ?: getMimeType(fileName) ?: "application/octet-stream"
                setMimeType(finalMimeType)
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            currentDownloadId = downloadManager.enqueue(request)
            
            Toast.makeText(context, "Descargando: $fileName", Toast.LENGTH_SHORT).show()
            
            // Registrar receiver para notificar cuando termine
            registerDownloadReceiver()
            
            return currentDownloadId
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
            return -1
        }
    }
    
    /**
     * Descarga directa sin usar DownloadManager (para archivos pequeños)
     */
    fun downloadFileSimple(url: String, fileName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Si falla, usar DownloadManager
            downloadFile(url, fileName)
        }
    }
    
    /**
     * Registra un receiver para saber cuando termina la descarga
     */
    private fun registerDownloadReceiver() {
        if (downloadReceiver != null) return
        
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                
                if (id == currentDownloadId) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                Toast.makeText(context, "Descarga completada ✓", Toast.LENGTH_SHORT).show()
                                
                                // Abrir el archivo automáticamente (opcional)
                                // openDownloadedFile(id)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                Toast.makeText(context, "Error en la descarga", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }
    }
    
    /**
     * Abre el archivo descargado
     */
    fun openDownloadedFile(downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = downloadManager.getUriForDownloadedFile(downloadId)
            val mimeType = downloadManager.getMimeTypeForDownloadedFile(downloadId)
            
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se puede abrir el archivo", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Extrae el nombre del archivo de la URL o Content-Disposition
     */
    private fun extractFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        var fileName: String? = null
        
        // Intentar extraer del Content-Disposition
        if (!contentDisposition.isNullOrBlank()) {
            fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        }
        
        // Si no, extraer de la URL
        if (fileName.isNullOrBlank() || fileName == "downloadfile") {
            val uri = Uri.parse(url)
            
            // Buscar en parámetros
            fileName = uri.getQueryParameter("file") 
                ?: uri.getQueryParameter("filename")
                ?: uri.getQueryParameter("name")
            
            // O del path
            if (fileName.isNullOrBlank()) {
                fileName = uri.lastPathSegment
            }
        }
        
        // Generar nombre si todo falla
        if (fileName.isNullOrBlank()) {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            fileName = "archivo_${System.currentTimeMillis()}.$extension"
        }
        
        // Limpiar el nombre
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    /**
     * Obtiene el MIME type basado en la extensión
     */
    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MIME_TYPES[extension] 
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    
    /**
     * Verifica si tiene permisos de almacenamiento
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ no necesita permiso para descargar a Downloads
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Desregistrar el receiver al destruir
     */
    fun unregister() {
        try {
            downloadReceiver?.let {
                context.unregisterReceiver(it)
                downloadReceiver = null
            }
        } catch (e: Exception) {
            // Ignorar si no estaba registrado
        }
    }
}
