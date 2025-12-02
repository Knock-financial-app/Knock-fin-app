package com.example.myapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IdCardInfo(
    var name: String = "",
    var driverLicenseNumber: String = "",
    var residentNumber: String = "",
    var issueDate: String = "",
    var address: String = "",
    var imagePath: String = "",
    var idType: String = ""
) : Parcelable {
    fun isValid(): Boolean {
        return name.isNotEmpty() && (residentNumber.isNotEmpty() || driverLicenseNumber.isNotEmpty())
    }

    fun isResidentCard(): Boolean = idType == "resident"
    fun isDriverLicense(): Boolean = idType == "driver"
}