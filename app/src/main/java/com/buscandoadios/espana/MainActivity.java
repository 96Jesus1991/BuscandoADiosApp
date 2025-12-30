package com.buscandoadios.espana;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity - Actividad principal con WebView
 * Carga la web de Buscando a Dios España
 */
public class MainActivity extends AppCompatActivity {

    // =============================================
    // CONFIGURACIÓN - CAMBIAR AQUÍ LA URL
    // =============================================
    private static final String WEB_URL = "https://buscandoadios-espana.com/";
    // =============================================

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout noInternetLayout;
    private SwipeRefreshLayout swipeRefresh;

    // Para subir archivos
    private ValueCallback<Uri[]> filePathCallback;
    private String cameraPhotoPath;
    private static final int FILE_CHOOSER_REQUEST = 1;
    private static final int PERMISSION_REQUEST = 2;
    private static final int STORAGE_PERMISSION_REQUEST = 3;

    // Para descargas
    private long currentDownloadId = -1;
    private BroadcastReceiver downloadReceiver;
    private String pendingDownloadUrl;
    private String pendingDownloadMimeType;
    private String pendingDownloadFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        noInternetLayout = findViewById(R.id.noInternetLayout);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Configurar botón reintentar
        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> loadWebsite());

        // Configurar SwipeRefresh (deslizar para actualizar)
        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.dorado)
        );
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
        });

        // Configurar receptor de descargas completadas
        setupDownloadReceiver();

        // Configurar WebView
        setupWebView();

        // Cargar la web
        loadWebsite();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        // Habilitar JavaScript
        settings.setJavaScriptEnabled(true);
        
        // Habilitar DOM Storage (localStorage)
        settings.setDomStorageEnabled(true);
        
        // Habilitar caché
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Habilitar zoom
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // Ajustar contenido al ancho
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // Habilitar formularios
        settings.setSaveFormData(true);
        
        // Habilitar acceso a archivos
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // Habilitar mixed content (HTTP en HTTPS) si es necesario
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // User Agent personalizado
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " BuscandoADiosApp/1.0");

        // Habilitar cookies persistentes
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Configurar WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                noInternetLayout.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // Solo mostrar error si es la página principal
                if (request.isForMainFrame()) {
                    showNoInternet();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Abrir enlaces externos en el navegador
                if (!url.contains("buscandoadios-espana.com")) {
                    // Enlaces de teléfono
                    if (url.startsWith("tel:")) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                    // Enlaces de email
                    if (url.startsWith("mailto:")) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                    // Enlaces de WhatsApp
                    if (url.contains("whatsapp.com") || url.contains("wa.me")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                    // Otros enlaces externos
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }
                return false; // Cargar en WebView
            }
        });

        // Configurar WebChromeClient (para subir archivos, geolocalización, etc.)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            // Subir archivos (input type="file")
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;
                openFileChooser();
                return true;
            }

            // Permisos de geolocalización
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        // =============================================
        // DESCARGAS - USANDO DOWNLOADMANAGER
        // =============================================
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                
                // Obtener nombre del archivo
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                
                // Guardar para usar después de verificar permisos
                pendingDownloadUrl = url;
                pendingDownloadMimeType = mimeType;
                pendingDownloadFileName = fileName;
                
                // Verificar permisos y descargar
                if (checkStoragePermission()) {
                    startDownload(url, fileName, mimeType, userAgent);
                }
            }
        });
    }

    // =============================================
    // MÉTODOS PARA DESCARGAS
    // =============================================

    /**
     * Verifica si tiene permiso de almacenamiento
     */
    private boolean checkStoragePermission() {
        // Android 10+ no necesita permiso para descargar a Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    /**
     * Inicia la descarga usando DownloadManager
     */
    private void startDownload(String url, String fileName, String mimeType, String userAgent) {
        try {
            // Crear request de descarga
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            
            // Configurar notificación
            request.setTitle(fileName);
            request.setDescription("Descargando archivo...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // Destino: carpeta Descargas pública
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            // Permitir en datos móviles y roaming
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            
            // Headers
            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }
            
            // Cookies del WebView
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
            }
            
            // MIME type
            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            } else {
                // Intentar detectar por extensión
                String extension = MimeTypeMap.getFileExtensionFromUrl(url);
                if (extension != null) {
                    String detectedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if (detectedMime != null) {
                        request.setMimeType(detectedMime);
                    }
                }
            }
            
            // Obtener DownloadManager e iniciar descarga
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                currentDownloadId = downloadManager.enqueue(request);
                Toast.makeText(this, "📥 Descargando: " + fileName, Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al iniciar descarga: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Fallback: abrir en navegador
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "No se puede descargar el archivo", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Configura el receptor para saber cuando termina la descarga
     */
    private void setupDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                
                if (id == currentDownloadId) {
                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(id);
                        Cursor cursor = downloadManager.query(query);
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            int status = cursor.getInt(statusIndex);
                            
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                Toast.makeText(MainActivity.this, "✅ Descarga completada", Toast.LENGTH_SHORT).show();
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                Toast.makeText(MainActivity.this, "❌ Error en la descarga", Toast.LENGTH_SHORT).show();
                            }
                            cursor.close();
                        }
                    }
                }
            }
        };
        
        // Registrar el receptor
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
    }

    private void loadWebsite() {
        if (isNetworkAvailable()) {
            noInternetLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(WEB_URL);
        } else {
            showNoInternet();
        }
    }

    private void showNoInternet() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        webView.setVisibility(View.GONE);
        noInternetLayout.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // =============================================
    // SUBIR ARCHIVOS
    // =============================================

    private void openFileChooser() {
        // Verificar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
                return;
            }
        }

        // Intent para cámara
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error al crear archivo
            }

            if (photoFile != null) {
                cameraPhotoPath = photoFile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            }
        }

        // Intent para galería/archivos
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});

        // Combinar intents
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Seleccionar archivo");

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});
        }

        startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;

            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) {
                    // Foto de cámara
                    if (cameraPhotoPath != null) {
                        results = new Uri[]{Uri.fromFile(new File(cameraPhotoPath))};
                    }
                } else {
                    // Archivo seleccionado
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            cameraPhotoPath = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                Toast.makeText(this, "Se necesitan permisos para subir fotos", Toast.LENGTH_SHORT).show();
            }
        }
        
        // Permiso de almacenamiento para descargas
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Reintentar la descarga pendiente
                if (pendingDownloadUrl != null) {
                    startDownload(pendingDownloadUrl, pendingDownloadFileName, 
                                  pendingDownloadMimeType, webView.getSettings().getUserAgentString());
                    pendingDownloadUrl = null;
                }
            } else {
                Toast.makeText(this, "Se necesitan permisos para descargar archivos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =============================================
    // NAVEGACIÓN CON BOTÓN ATRÁS
    // =============================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Confirmar salida
            new AlertDialog.Builder(this)
                .setTitle("Salir")
                .setMessage("¿Deseas salir de Buscando a Dios?")
                .setPositiveButton("Sí", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
        }
    }

    // =============================================
    // CICLO DE VIDA
    // =============================================

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        // Desregistrar receptor de descargas
        if (downloadReceiver != null) {
            try {
                unregisterReceiver(downloadReceiver);
            } catch (Exception e) {
                // Ignorar si no estaba registrado
            }
        }
        
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

