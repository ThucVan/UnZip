package com.thaiduong.unzip.ui.fragments

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentGoogleDriveBinding
import com.thaiduong.unzip.ui.bases.BaseFragment


class GoogleDriveFragment(override val layoutId: Int = R.layout.fragment_google_drive) :
    BaseFragment<FragmentGoogleDriveBinding>() {

    override fun initUi() {

//        binding.btnAddAccount.setOnClickListener {
//            GoogleSignIn()
//        }
    }

    override fun doWork() {

        binding.tvSignIn.setOnClickListener {
            GoogleSignIn()
        }

        binding.tvSignOut.setOnClickListener {
            signout()
        }

    }

    //region User Google Sign-in and sign-out Code

    fun getGoogleSinginClient(): GoogleSignInClient {
        /**
         * Configure sign-in to request the user's ID, email address, and basic
         * profile. ID and basic profile are included in DEFAULT_SIGN_IN.
         */
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()


        /**
         * Build a GoogleSignInClient with the options specified by gso.
         */
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun GoogleSignIn() {

        if (!isUserSignedIn()) {

            val signInIntent = getGoogleSinginClient().signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } else {
            toast("User already signed-in")
        }

    }

    private fun isUserSignedIn(): Boolean {

        val account =
            com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(requireContext())
        return account != null

    }

    private fun signout() {
        if (isUserSignedIn()) {
            getGoogleSinginClient().signOut().addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), " Signed out ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), " Error ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            handleSignData(data)

        }
    }

    private fun handleSignData(data: Intent?) {
        // The Task returned from this call is always completed, no need to attach
        // a listener.
        com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnCompleteListener {
                "isSuccessful ${it.isSuccessful}".print()
                if (it.isSuccessful) {
                    // user successfully logged-in
                    "account ${it.result?.account}".print()
                    "displayName ${it.result?.displayName}".print()
                    "Email ${it.result?.email}".print()
                } else {
                    // authentication failed
                    "exception ${it.exception}".print()
                }
            }

    }

    //endregion


    companion object {
        const val RC_SIGN_IN = 0
        const val TAG_KOTLIN = "TAG_KOTLIN"
    }


    fun Any.print() {
        Log.e(TAG_KOTLIN, " $this")
    }

    fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}

