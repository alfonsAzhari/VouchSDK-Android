package sg.vouch.vouchsdk.data.model.message.response


import com.google.gson.annotations.SerializedName

data class ElementListModel(
    @SerializedName("buttons")
    val buttons: List<ButtonModel>? = listOf(),
    @SerializedName("default_action")
    val defaultAction: DefaultAction? = DefaultAction(),
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("subtitle")
    val subtitle: String? = null,
    @SerializedName("title")
    val title: String? = null
)