package com.yasir.chaaponotes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberImagePainter
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.yasir.chaaponotes.ui.theme.ChaapoNotesTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            ChaapoNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var imageUris by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }
                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = {
                            if (it.resultCode == RESULT_OK) {
                                val result =
                                    GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                                imageUris = result?.pages?.map { it.imageUri } ?: emptyList()
                                result?.pdf?.let { pdf ->
                                    val fos = FileOutputStream(File(filesDir, "scan.pdf"))
                                    contentResolver.openInputStream(pdf.uri)?.use { inputStream ->
                                        inputStream.copyTo(fos)
                                    }
                                }
                            }
                        }
                    )
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {
                            val lazyListState = rememberLazyListState()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(imageUris) { uri ->
                                    Image(
                                        painter = rememberImagePainter(uri),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .height(400.dp)
                                            .fillMaxWidth()
                                            .border(1.dp, Color.Gray)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ){
                                Button(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterVertically),
                                    onClick = {
                                        saveAsPdf(applicationContext,imageUris)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.save_ic),
                                        contentDescription = "save pdf btn"
                                    )
                                }

                            FloatingActionButton(
                                onClick = {
                                    scanner.getStartScanIntent(this@MainActivity)
                                        .addOnSuccessListener { intentSender ->
                                            scannerLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        }
                                        .addOnFailureListener { exception ->
                                            Toast.makeText(
                                                applicationContext,
                                                exception.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                },
                                modifier = Modifier
                                    .size(60.dp)
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 12.dp, bottom = 12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.camera_ic),
                                    contentDescription = "Take a photo"
                                )
                            }

                        }
                    }
                }
            }
        }
    }
}
fun saveAsPdf(context: Context, imageUris: List<Uri>) {
    val pdfDocument = PdfDocument()
    try {
        for (uri in imageUris) {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
            bitmap.recycle()
        }
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        Toast.makeText(context, "path:${directory?.path}", Toast.LENGTH_SHORT).show()
        val file = File(directory, "scan.pdf")
        val fos = FileOutputStream(file)
        Toast.makeText(context, "path:${file}", Toast.LENGTH_SHORT).show()
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
    }
}

