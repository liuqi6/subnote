package com.mindforge.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveApi
import com.google.android.gms.drive.DriveFile
import com.google.android.gms.drive.DriveId
import com.mindforge.graphics.Screen
import com.mindforge.graphics.android.*
import com.mindforge.graphics.observableIterable
import kotlinx.android.synthetic.activity_main.mainTextView
import org.xmind.core.ITopic
import org.xmind.core.internal.Topic
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.properties.Delegates

public class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item!!.getItemId()

        return when (id) {
            R.id.action_settings -> {
                true
            }
            R.id.open_from_drive -> {
                openFromDrive()
                true
            }
            R.id.open_from_documents -> {
                openFromDocuments()
                true
            }
            R.id.drive_example -> {
                startDriveAPIExampleActivity()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun openFromDocuments() {
        open(File("/storage/emulated/0/documents/Projects.xmind"))
    }

    private fun openFromDrive() {
        if (!driveFileOpenerApiClient.isConnected()) {
            driveFileOpenerApiClient.connect()
            // chooseFileFromDrive will be called in onConnected.
            return
        }
        chooseFileFromDrive()
    }

    private fun startDriveAPIExampleActivity() {
        startActivity(Intent(this, javaClass<DriveSampleActivity>()))
    }

    private fun open(file: File) {
        val reader = XMindFileReader(cacheDirectory = getCacheDir())
        // mainTextView.setText(reader(file))

        setDemoScreen(reader.rootTopics(file))
    }

    private val driveFileOpenerApiClient: GoogleApiClient by Delegates.lazy {
        GoogleApiClient.Builder(this).addApi(Drive.API).addScope(Drive.SCOPE_FILE).addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
            override fun onConnected(connectionHint: Bundle?) {
                chooseFileFromDrive()
            }

            override fun onConnectionSuspended(cause: Int) {
                throw UnsupportedOperationException("GoogleApiClient connection suspended")
            }
        }).addOnConnectionFailedListener(object : GoogleApiClient.OnConnectionFailedListener {
            override fun onConnectionFailed(result: ConnectionResult) {
                if (!result.hasResolution()) {
                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this@MainActivity, 0).show()
                    return
                }
                // Called typically when the app is not yet authorized, and an authorization dialog is displayed to the user.
                result.startResolutionForResult(this@MainActivity, IntentCode.googleClientResolution)
            }
        }).build()
    }

    private fun chooseFileFromDrive() {
        val intentSender = Drive.DriveApi.newOpenFileActivityBuilder().build(driveFileOpenerApiClient)

        startIntentSenderForResult(intentSender, IntentCode.openFileFromDrive, null, 0, 0, 0);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IntentCode.openFileFromDrive ->
                if (resultCode == Activity.RESULT_OK) {
                    val driveFile = Drive.DriveApi.getFile(driveFileOpenerApiClient, data!!.getExtras().get("response_drive_id") as DriveId)

                    driveFile.open(driveFileOpenerApiClient, DriveFile.MODE_READ_ONLY, object : DriveFile.DownloadProgressListener {
                        override fun onProgress(bytesDownloaded: Long, bytesExpected: Long) {
                            mainTextView.setText("loading... " + if(bytesExpected > 0) "$bytesDownloaded / $bytesExpected bytes" else "")
                        }
                    }).setResultCallback (object : ResultCallback<DriveApi.DriveContentsResult> {
                        override fun onResult(result: DriveApi.DriveContentsResult) {
                            if (!result.getStatus().isSuccess()) throw UnsupportedOperationException()

                            val file = File(getCacheDir(), "temp.xmind")
                            result.getDriveContents().getInputStream().writeToFile(file)
                            open(file)
                        }

                    })

                }
        }
    }

    private fun setDemoScreen(rootTopics : List<ITopic>) {
        val screen = GlScreen(this) {
            Shell(it, observableIterable(listOf(it.pointerKeys)), it.keyboard, GlFont(getResources()!!), rootTopics)
        }

        setContentView(screen)
    }

    private fun InputStream.writeToFile(file: File) {
        try {
            val output = FileOutputStream(file)
            try {
                val buffer = ByteArray(1024)

                while (true) {
                    val read = read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            } finally {
                output.close()
            }
        } finally {
            close()
        }
    }

    private object IntentCode {
        val googleClientResolution = 0
        val openFileFromDrive = 1
    }
}