package com.example.myapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IdCardInfo(
    var name: String = "",
    var residentNumber: String = "",
    var issueDate: String = "",
    var address: String = "",
    var imagePath: String = ""
) : Parcelable {
    fun isValid(): Boolean {
        return name.isNotEmpty() && residentNumber.isNotEmpty()
    }
}