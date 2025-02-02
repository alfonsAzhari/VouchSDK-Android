package sg.vouch.vouchsdk.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import sg.vouch.vouchsdk.utils.Const.SG_LOCALE
import io.socket.client.Socket
import io.socket.engineio.client.Transport
import java.text.SimpleDateFormat
import java.util.*


/**
 * @author Radhika Yusuf Alifiansyah
 * Bandung, 26 Aug 2019
 */


fun Fragment.openDialPhone(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = Uri.parse("tel:$phoneNumber")
    startActivity(intent)
}

fun Fragment.openWebUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

fun Context.openWebUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

fun ImageView.setImageUrl(url: String) {
    Glide.with(this).load(url).centerCrop().into(this)
}

fun Activity.getScreenWidth(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

fun View.setFontFamily(font: String) {
    if (font.isEmpty()) {
        return
    }

    try {
        when (this) {
            is TextView -> {
                typeface = Typeface.createFromAsset(context.resources.assets, "fonts/${font.toLowerCase()}.ttf")
            }
            is EditText -> {
                typeface = Typeface.createFromAsset(context.resources.assets, "fonts/${font.toLowerCase()}.ttf")
            }
            is RadioButton -> {
                typeface = Typeface.createFromAsset(context.resources.assets, "fonts/${font.toLowerCase()}.ttf")
            }
            is Button -> {
                typeface = Typeface.createFromAsset(context.resources.assets, "fonts/${font.toLowerCase()}.ttf")
            }
        }
    } catch (e: Exception) {
        showErrorInflateFont()
    }
}

private fun showErrorInflateFont() = Log.e("FONTFACE", "error when set font face")

fun Activity.getScreenHeight(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}

fun ImageView.setImageUrls(url: String, screenWidth: Int, screenHeight: Int) {
    Glide.with(this).asBitmap().load(url).centerCrop().into(object : CustomViewTarget<ImageView, Bitmap>(this) {
        override fun onLoadFailed(errorDrawable: Drawable?) {

        }

        override fun onResourceCleared(placeholder: Drawable?) {

        }

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            var width = resource.width
            var height = resource.height

            while (width > (screenWidth / 2) && height > (screenHeight / 2)) {
                if (width > (screenWidth / 2)) width /= 2
                if (height > (screenHeight / 2)) height /= 2
            }

            this@setImageUrls.layoutParams?.width = width
            this@setImageUrls.layoutParams?.height = height
        }
    })
}

fun ImageView.setImageUrlFromVideo(url: String) {
    Glide.with(this).load(url).error(ColorDrawable(Color.BLACK)).into(this)
}

fun Int.basicToColorStateList(): ColorStateList {
    val states = arrayOf(
        intArrayOf(android.R.attr.state_enabled), // enabled
        intArrayOf(-android.R.attr.state_enabled), // disabled
        intArrayOf(-android.R.attr.state_checked), // unchecked
        intArrayOf(android.R.attr.state_pressed)  // pressed
    )
    val colors = intArrayOf(this, this, this, this)
    return ColorStateList(states, colors)
}

fun String.reformatFullDate(format: String, locale: Locale = SG_LOCALE): String {

    /*val sdf = SimpleDateFormat(Const.DATE_TIME_FORMAT, locale)
    val serverTime = sdf.parse(this)

    val dateTimeMillis = if (this.isNotEmpty()) {
        sdf.parse(this).time
    } else {
        System.currentTimeMillis()
    }

    val dateServer = Date(dateTimeMillis)

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateTimeMillis

    return if (dateTimeMillis != 0.toLong()) {
        SimpleDateFormat(format, locale).format(calendar.time)
    } else {
        SimpleDateFormat(format, locale).format(System.currentTimeMillis())
    }*/

    val inputFormat = SimpleDateFormat(Const.DATE_TIME_FORMAT, locale)
    val date = Date(System.currentTimeMillis())
    return SimpleDateFormat(format, locale).format(date)
}


fun Socket.addHeader(credentialToken: String) {
    on(Transport.EVENT_REQUEST_HEADERS) { args ->
        if (args.getOrNull(0) is MutableMap<*, *> && args.isNotEmpty()) {
            val headers = args[0] as MutableMap<String, String>
            headers["token"] = credentialToken
            headers["Content-Type"] = "application/json"
        }
    }
}

fun String?.parseColor(@ColorInt defaultColor: Int = Color.WHITE): Int {
    return try {
        if (this != null) {
            Color.parseColor(this)
        } else defaultColor
    } catch (e: Exception) {
        defaultColor
    }
}

fun String?.safe(default: String = ""): String {
    return this ?: default
}