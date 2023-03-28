

package com.example.googlesignin.presentation


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Text
import com.example.googlesignin.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await

/**
 * Demonstrates using Google Sign-In on Android Wear
 */
class MainActivity : ComponentActivity() {

    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var googleSignInAccount by remember {
                mutableStateOf(GoogleSignIn.getLastSignedInAccount(this))
            }

            val signInRequestLauncher = rememberLauncherForActivityResult(
                contract = GoogleSignInContract(googleSignInClient)
            ) {
                googleSignInAccount = it
                if (googleSignInAccount != null) {
                    Toast.makeText(
                        this,
                        "Signed in as ${googleSignInAccount?.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val coroutineScope = rememberCoroutineScope()

            GoogleSignInScreen(
                googleSignInAccount = googleSignInAccount,
                onSignInClicked = { signInRequestLauncher.launch(Unit) },
                onSignOutClicked = {
                    coroutineScope.launch {
                        try {
                            googleSignInClient.signOut()

                            googleSignInAccount = null

                            Toast.makeText(
                                this@MainActivity,
                                "Signed out",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (apiException: ApiException) {
                            Log.w("GoogleSignInActivity", "Sign out failed: $apiException")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GoogleSignInScreen(
    googleSignInAccount: GoogleSignInAccount?,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (googleSignInAccount == null) {
            AndroidView(::SignInButton) { signInButton ->
                signInButton.setOnClickListener {
                    onSignInClicked()
                }
            }
        } else {
            Chip(
                onClick = onSignOutClicked,
                label = {
                    Text(text = "Logout" /*stringResource(id = R.string.wear_signout_button_text)*/)
                }
            )
        }
    }
}

/**
 * An [ActivityResultContract] for signing in with the given [GoogleSignInClient].
 */
private class GoogleSignInContract(
    private val googleSignInClient: GoogleSignInClient
) : ActivityResultContract<Unit, GoogleSignInAccount?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        googleSignInClient.signInIntent

    override fun parseResult(resultCode: Int, intent: Intent?): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        // As documented, this task must be complete
        check(task.isComplete)

        return if (task.isSuccessful) {
            task.result
        } else {
            val exception = task.exception
            check(exception is ApiException)
            Log.w(
                "GoogleSignInContract",
                "Sign in failed: code=${
                    exception.statusCode
                }, message=${
                    GoogleSignInStatusCodes.getStatusCodeString(exception.statusCode)
                }"
            )
            null
        }
    }
}