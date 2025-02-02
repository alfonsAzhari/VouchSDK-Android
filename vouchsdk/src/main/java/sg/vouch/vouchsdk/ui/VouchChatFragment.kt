package sg.vouch.vouchsdk.ui


import android.Manifest
import android.arch.lifecycle.Observer
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import sg.vouch.vouchsdk.R
import sg.vouch.vouchsdk.VouchSDK
import sg.vouch.vouchsdk.data.model.message.body.MessageBodyModel
import sg.vouch.vouchsdk.ui.VouchChatEnum.*
import sg.vouch.vouchsdk.ui.adapter.VouchChatAdapter
import sg.vouch.vouchsdk.ui.model.VouchChatModel
import sg.vouch.vouchsdk.ui.model.VouchChatType.TYPE_LIST
import sg.vouch.vouchsdk.utils.*
import sg.vouch.vouchsdk.utils.Const.CHAT_BUTTON_TYPE_PHONE
import sg.vouch.vouchsdk.utils.Const.CHAT_BUTTON_TYPE_POSTBACK
import sg.vouch.vouchsdk.utils.Const.CHAT_BUTTON_TYPE_WEB
import sg.vouch.vouchsdk.utils.Const.IMAGE_UPLOAD_KEY
import sg.vouch.vouchsdk.utils.Const.MIME_TYPE_JPEG
import sg.vouch.vouchsdk.utils.Const.PAGE_SIZE
import sg.vouch.vouchsdk.utils.Const.URI_SCHEME_FILE
import kotlinx.android.synthetic.main.fragment_vouch_chat.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*


/**
 * @Author by Radhika Yusuf
 * Bandung, on 2019-08-28
 */
class VouchChatFragment : Fragment(), TextWatcher, View.OnClickListener, VouchChatClickListener,
    MediaPlayer.OnPreparedListener {

    private lateinit var mViewModel: VouchChatViewModel
    private lateinit var mLayoutManager: LinearLayoutManager
    private var mMediaPlayer: MediaPlayer? = null
    private var mLocationService: FusedLocationProviderClient? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mViewModel = createViewModel()
        observeLiveData()
        return inflater.inflate(R.layout.fragment_vouch_chat, container, false)
    }

    private fun createViewModel(): VouchChatViewModel {
        return VouchChatViewModel(requireActivity().application).apply {
            mVouchSDK = VouchSDK.createSDK(requireActivity().application)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewChat.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                Handler().post { (v as RecyclerView).smoothScrollToPosition(0) }
            }
        }
        setupListData()
        setupTextListener()
        mViewModel.start()

        buttonGreeting.setOnClickListener(this@VouchChatFragment)
        attachmentButton.setOnClickListener(this@VouchChatFragment)
        sendButton.setOnClickListener(this@VouchChatFragment)
    }

    private fun setupTextListener() {
        fieldContent.addTextChangedListener(this@VouchChatFragment)
    }

    private fun setupListData() {
        mLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        recyclerViewChat.layoutManager = mLayoutManager
        recyclerViewChat.adapter = VouchChatAdapter(mViewModel, this@VouchChatFragment)
        recyclerViewChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = mLayoutManager.childCount
                val totalItemCount = mLayoutManager.itemCount
                val firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition()

                if ((visibleItemCount + firstVisibleItemPosition >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE
                            && mViewModel.isRequesting.value == false)
                    && !mViewModel.isPaginating
                    && !mViewModel.isLastPage
                ) {
                    mViewModel.isPaginating = true

                    Handler().postDelayed({
                        mViewModel.getChatContent()
                    }, 1000)

                }
            }
        })
    }

    private fun observeLiveData() {
        mViewModel.apply {

            isRequesting.observe(this@VouchChatFragment, Observer {
                progressIndicator.visibility = if (it == true) View.VISIBLE else View.GONE
            })

            changeConnectStatus.observe(this@VouchChatFragment, Observer {
                imageIndicator.setImageResource(if (it == true) R.drawable.circle_green else R.drawable.circle_red)
                inputField.visibility =
                    if (it == true && eventChangeStateToGreeting.value != true) View.VISIBLE else View.GONE
            })

            eventShowMessage.observe(this@VouchChatFragment, Observer {
                Snackbar.make(view ?: return@Observer, it.safe(), Snackbar.LENGTH_LONG).show()
            })

            loadConfiguration.observe(this@VouchChatFragment, Observer {
                if (it != null) {
                    titleChat.text = it.title
                    titleChat.setFontFamily(it.fontStyle.safe())
                    toolbarChat.background = ColorDrawable(it.headerBgColor.parseColor(Color.BLACK))
                    poweredText.setFontFamily(it.fontStyle.safe())
                    poweredText.background = ColorDrawable(it.headerBgColor.parseColor(Color.BLACK))
                    poweredText.visibility =
                        if (it.poweredByVouch == true) View.VISIBLE else View.GONE
                    poweredText.setFontFamily(it.fontStyle.safe())

                    backgroundContent.background =
                        ColorDrawable(it.backgroundColorChat.parseColor(Color.WHITE))
                    imageProfileChat.setImageUrl(it.avatar?:"")

                    inputField.setCardBackgroundColor(it.inputTextBackgroundColor.parseColor())
                    inputField.setFontFamily(it.fontStyle.safe())

                    fieldContent.setTextColor(it.inputTextColor.parseColor())
                    fieldContent.setFontFamily(it.fontStyle.safe())

                    backgroundSend.setImageDrawable(ColorDrawable(it.sendButtonColor.parseColor()))
                    backgroundAttachment.setImageDrawable(ColorDrawable(it.attachmentButtonColor.parseColor()))

                    imageSend.setColorFilter(it.sendIconColor.parseColor(), PorterDuff.Mode.SRC_IN)
                    imageAttachment.setColorFilter(
                        it.attachmentIconColor.parseColor(),
                        PorterDuff.Mode.SRC_IN
                    )

                    buttonGreeting.text = it.greetingButtonTitle ?: "Get Started"
                    buttonGreeting.setBackgroundColor(it.btnBgColor.parseColor())
                    buttonGreeting.setTextColor(it.xButtonColor.parseColor())
                    buttonGreeting.setFontFamily(it.fontStyle.safe())

                    frameGreeting.visibility = View.GONE
                    inputField.visibility = View.GONE
                }
            })

            eventChangeStateToGreeting.observe(this@VouchChatFragment, Observer {
                frameGreeting.visibility = if (it == true) View.VISIBLE else View.GONE
                inputField.visibility = if (it == true) View.GONE else View.VISIBLE
            })

            eventUpdateList.observe(this@VouchChatFragment, Observer {
                if (it != null) {
                    when (it.type) {
                        TYPE_INSERTED -> {
                            recyclerViewChat.adapter?.notifyItemInserted(it.startPosition)
                            if (it.startPosition == 0) recyclerViewChat.smoothScrollToPosition(0)
                        }
                        TYPE_UPDATE -> {
                            recyclerViewChat.adapter?.notifyItemRangeChanged(
                                it.startPosition,
                                it.endPosition ?: it.startPosition + 1
                            )
                        }
                        TYPE_REMOVE -> {
                            recyclerViewChat.adapter?.notifyItemRangeRemoved(
                                it.startPosition,
                                it.endPosition ?: it.startPosition + 1
                            )
                        }
                        TYPE_FORCE_UPDATE -> {
                            recyclerViewChat.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            })

            eventScroll.observe(this@VouchChatFragment, Observer {
                if (mViewModel.lastScrollPosition != -1) {
                    Handler().postDelayed({
                        recyclerViewChat.smoothScrollBy(0, mViewModel.lastScrollPosition)
                        mViewModel.lastScrollPosition = -1
                    }, 1500)
                }
            })

        }
    }

    override fun onClickQuickReply(data: MessageBodyModel) {
        if (data.msgType == "location") {
            checkMapsPermissions()
        } else {
            mViewModel.sendReplyMessage(data)
        }
    }

    private fun createLocationListener() {
        mLocationService = LocationServices.getFusedLocationProviderClient(requireActivity())
        mLocationService?.lastLocation?.addOnSuccessListener(mViewModel)
    }

    override fun onResume() {
        super.onResume()
        mViewModel.reconnectSocket()
    }

    override fun onPause() {
        super.onPause()
        mViewModel.disconnectSocket()
        mMediaPlayer?.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (recyclerViewChat.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition().let {
            mViewModel.lastScrollPosition = if (it < 0) 0 else it
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonGreeting -> {
                mViewModel.sendReference()
            }
            R.id.sendButton -> {
                mViewModel.sendReplyMessage(
                    MessageBodyModel(
                        msgType = "text",
                        text = fieldContent.text.trim().toString(),
                        type = "text"
                    )
                )
                fieldContent.setText("")
            }
            R.id.attachmentButton -> {
                CameraGalleryHelper.openImagePickerOption(requireActivity())
            }
            else -> {
            }
        }
    }

    override fun onClickChatButton(type: String, data: VouchChatModel) {
        when (type) {
            CHAT_BUTTON_TYPE_POSTBACK -> {
                mViewModel.sendReplyMessage(
                    MessageBodyModel(
                        msgType = "text",
                        payload = data.payload,
                        text = if (data.type == TYPE_LIST) data.buttonTitle else data.title,
                        type = "quick_reply"
                    )
                )
            }
            CHAT_BUTTON_TYPE_WEB -> {
                openWebUrl(data.payload)
            }
            CHAT_BUTTON_TYPE_PHONE -> {
                openDialPhone(data.payload)
            }
        }
    }

    override fun onClickPlayAudio(mediaPlayer: MediaPlayer) {
        mMediaPlayer?.stop()
        mMediaPlayer?.release()
        mMediaPlayer = mediaPlayer
        mMediaPlayer?.prepareAsync()
        mMediaPlayer?.setOnPreparedListener(this@VouchChatFragment)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mMediaPlayer?.start()
    }

    override fun onClickPlayVideo(data: VouchChatModel, imageView: ImageView) {
        VouchChatVideoPlayerActivity.startThisActivity(requireActivity(), data.mediaUrl, imageView)
    }

    override fun afterTextChanged(s: Editable?) = Unit

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        s?.toString().safe().trim().isNotEmpty().let {
            sendButton.isEnabled = it
            sendButton.isClickable = it
            sendButton.isFocusable = it

            if (it) {
                imageSend.setImageResource(R.drawable.ic_send_white_24dp)
            } else {
                imageSend.setImageResource(R.drawable.ic_mic_white_24dp)
            }
        }

        imageSend.setColorFilter(
            mViewModel.loadConfiguration.value?.sendIconColor.parseColor(),
            PorterDuff.Mode.SRC_IN
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 5115) {
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
        }
    }

    fun sendImageChat(imageUri: Uri) {
        val resolver = context?.contentResolver
        val scheme = imageUri.scheme
        val requestBody: RequestBody?
        val requestPart: MultipartBody.Part?

        if (scheme != null && scheme == URI_SCHEME_FILE) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(resolver, imageUri)
                val stream = FileOutputStream(imageUri.path)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                stream.flush()
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val file = File(imageUri.path)
            requestBody = file.asRequestBody(MIME_TYPE_JPEG.toMediaTypeOrNull())
            requestPart =
                MultipartBody.Part.createFormData(IMAGE_UPLOAD_KEY, file.name, requestBody)
        } else {
            val mimeType = resolver?.getType(imageUri)
            val inputStream = resolver?.openInputStream(imageUri)
            val file = createFileFromInputStream(inputStream!!, mimeType)

            requestBody = file!!.asRequestBody(mimeType?.toMediaTypeOrNull())
            requestPart =
                MultipartBody.Part.createFormData(IMAGE_UPLOAD_KEY, file.name, requestBody)
        }

        mViewModel.sendImageMessage(requestPart)

        val bitmap = MediaStore.Images.Media.getBitmap(resolver, imageUri)
        ivPreview.setImageBitmap(bitmap)
        ivPreview.visibility = View.GONE
    }

    private fun createFileFromInputStream(inputStream: InputStream, mimeType: String?): File? {
        try {
            val millis = System.currentTimeMillis()
            val path = context?.externalCacheDir
            val fileName = "$millis${createFileExtensionFromMimeType(mimeType)}"
            val file = File(path, fileName)

            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var length = inputStream.read(buffer)

            while (length > 0) {
                outputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }

            outputStream.close()
            inputStream.close()

            return file
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    private fun createFileExtensionFromMimeType(mimeType: String?): String {
        if (mimeType == null) return ""

        if (mimeType.contains("image/")) {
            return when (val type = mimeType.removePrefix("image/")) {
                "vnd.microsoft.icon" -> ".ico"
                "svg+xml" -> ".svg"
                else -> ".$type"
            }
        }

        return ""
    }

    private fun checkMapsPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fineLocationPermissionState = ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val coarseLocationPermissionState = ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val mapsPermissionGranted =
                fineLocationPermissionState == PackageManager.PERMISSION_GRANTED
                        && coarseLocationPermissionState == PackageManager.PERMISSION_GRANTED
            if (!mapsPermissionGranted) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 5115
                )
                return false
            } else {
                if (mViewModel.currentLocation.first == 0.0 && mViewModel.currentLocation.second == 0.0) {
                    createLocationListener()
                } else {
                    mViewModel.sendLocation()
                }
            }
            return true
        }
        return true
    }
}
