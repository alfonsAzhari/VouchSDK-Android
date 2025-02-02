package sg.vouch.vouchsdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.util.Log


/**
 * @author Radhika Yusuf Alifiansyah
 * Bandung, 26 Aug 2019
 */

object Helper {

    fun getCredentialKey(context: Context): String {
        try {
            val app =
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val bundle = app.metaData
            return bundle.getString("sg.vouch.chat", "empty") ?: "empty"

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Error", e.message ?: e.localizedMessage)
        } catch (e: NullPointerException) {
            Log.e("Error", e.message ?: e.localizedMessage)
        }
        return ""
    }

    fun checkConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        //should check null because in airplane mode it will be null
        return netInfo != null && netInfo.isConnected
    }


}