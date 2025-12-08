package com.example.myapplication.ui.idcard.ocr

import com.example.myapplication.data.IdCardInfo
import java.util.Calendar

class IdCardInfoExtractor {

    companion object {
        const val ID_TYPE_UNKNOWN = 0
        const val ID_TYPE_RESIDENT = 1
        const val ID_TYPE_DRIVER = 2

        private val DRIVER_LICENSE_NUMBER_PATTERN =
            Regex("(\\d{2})[\\s\\-.,:;]*(\\d{2})[\\s\\-.,:;]*(\\d{6})[\\s\\-.,:;]*(\\d{2})")

        private val RESIDENT_NUMBER_PATTERN =
            Regex("(\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d)[\\s\\-.,:;]*([1-4][\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d[\\s\\-.,:;]*\\d)")

        private val NAME_PATTERN = Regex("[가-힣]{2,4}")
        private val DATE_PATTERN =
            Regex("(19|20)\\d{2}[.,:;\\-/년\\s]*\\d{1,2}[.,:;\\-/월\\s]*\\d{1,2}")
        private val ADDRESS_PATTERN = Regex("([가-힣]+(?:시|도))[\\s]*[가-힣]+")

        private val RESIDENT_KEYWORDS = listOf("주민등록증", "RESIDENT", "REGISTRATION", "주민번호")
        private val DRIVER_KEYWORDS = listOf("운전면허증", "운전면허", "DRIVER", "LICENSE", "면허번호")

        private val EXCLUDE_NAME_WORDS = listOf(
            "1종대형", "1종보통", "1종소형", "특수", "대형견인", "소형견인",
            "2종보통", "원동기", "자동차운전면허증", "적성검사", "갱신기간",
            "조건", "경찰청장", "주민등록증", "시장",
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충청북도", "충청남도", "전라북도", "전라남도",
            "경상북도", "경상남도", "제주"
        )

        private val CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR)
    }

    data class ExtractedInfo(
        val name: String?,
        val residentNumber: String?,
        val driverLicenseNumber: String?,
        val issueDate: String?,
        val address: String?,
        val idType: String?
    ) {
        fun isValid(): Boolean {
            return !name.isNullOrEmpty() &&
                    (!residentNumber.isNullOrEmpty() || !driverLicenseNumber.isNullOrEmpty())
        }
    }

    fun extract(text: String): ExtractedInfo {
        val idType = detectIdCardType(text)
        val residentNumber = extractResidentNumber(text)

        return ExtractedInfo(
            name = extractName(text, idType),
            residentNumber = residentNumber,
            driverLicenseNumber = extractDriverLicenseNumber(text),
            issueDate = extractIssueDate(text, residentNumber),
            address = extractAddress(text),
            idType = when (idType) {
                ID_TYPE_RESIDENT -> "resident"
                ID_TYPE_DRIVER -> "driver"
                else -> "resident"
            }
        )
    }

    fun applyToIdCardInfo(extracted: ExtractedInfo): IdCardInfo {
        return IdCardInfo.current.apply {
            this.name = extracted.name.toString()
            this.residentNumber = extracted.residentNumber.toString()
            this.driverLicenseNumber = extracted.driverLicenseNumber.toString()
            this.issueDate = extracted.issueDate.toString()
            this.address = extracted.address.toString()
            this.idType = extracted.idType.toString()
        }
    }

    fun detectIdCardType(text: String): Int {
        val driverScore = DRIVER_KEYWORDS.count { text.contains(it, ignoreCase = true) }
        val residentScore = RESIDENT_KEYWORDS.count { text.contains(it, ignoreCase = true) }
        val hasDriverLicenseNumber = DRIVER_LICENSE_NUMBER_PATTERN.containsMatchIn(text)

        return when {
            hasDriverLicenseNumber -> ID_TYPE_DRIVER
            driverScore >= 2 -> ID_TYPE_DRIVER
            residentScore >= 1 -> ID_TYPE_RESIDENT
            driverScore >= 1 -> ID_TYPE_DRIVER
            else -> ID_TYPE_UNKNOWN
        }
    }

    fun extractName(text: String, idType: Int): String {
        val lines = text.split("\n").filter { it.isNotBlank() }
        val startIndex = if (idType == ID_TYPE_DRIVER) 3 else 1
        for (i in startIndex until lines.size) {
            val line = lines[i]

            for (match in NAME_PATTERN.findAll(line)) {
                val candidate = match.value.replace(" ", "")

                if (isValidNameCandidate(candidate)) {
                    return match.value
                }
            }
        }
        return ""
    }

    private fun isValidNameCandidate(candidate: String): Boolean {
        if (candidate.length !in 2..4) return false
        if (!candidate.all { it in '가'..'힣' }) return false
        if (EXCLUDE_NAME_WORDS.any { hasOverlap(candidate, it, 2) }) return false

        return true
    }

    fun extractResidentNumber(text: String): String {
        val match = RESIDENT_NUMBER_PATTERN.find(text) ?: return ""
        val cleaned = match.value.replace(Regex("[^0-9]"), "")

        if (cleaned.length != 13) return ""

        if (!isValidResidentNumber(cleaned)) return ""

        return cleaned
    }

    fun extractDriverLicenseNumber(text: String): String {
        val match = DRIVER_LICENSE_NUMBER_PATTERN.find(text) ?: return ""
        val cleaned = match.value.replace(Regex("[^0-9]"), "")
        return if (cleaned.length == 12) cleaned else ""
    }

    fun extractIssueDate(text: String, residentNumber: String?): String {
        val matches = DATE_PATTERN.findAll(text).toList()
        if (matches.isEmpty()) return ""

        val birthYear = residentNumber?.let { getBirthYearFromResidentNumber(it) }

        for (match in matches.reversed()) {
            val numbers = Regex("\\d+").findAll(match.value).map { it.value }.toList()
            if (numbers.size < 3) continue

            val year = numbers[0].toIntOrNull() ?: continue
            val month = numbers[1].toIntOrNull() ?: continue
            val day = numbers[2].toIntOrNull() ?: continue

            val validation = validateDate(year, month, day, birthYear)
            if (validation == DateValidation.VALID) {
                return "${numbers[0]}${numbers[1].padStart(2, '0')}${numbers[2].padStart(2, '0')}"
            }
        }

        return ""
    }

    fun extractAddress(text: String): String {
        return ADDRESS_PATTERN.find(text)?.value ?: ""
    }

    private fun isValidResidentNumber(number: String): Boolean {
        if (number.length != 13) return false

        val birthPart = number.take(6)
        val genderDigit = number[6].toString().toIntOrNull() ?: return false

        val century = when (genderDigit) {
            1, 2, 5, 6 -> 1900
            3, 4, 7, 8 -> 2000
            else -> return false
        }

        val year = century + (birthPart.take(2).toIntOrNull() ?: return false)
        val month = birthPart.substring(2, 4).toIntOrNull() ?: return false
        val day = birthPart.substring(4, 6).toIntOrNull() ?: return false

        if (!isValidDateComponents(year, month, day)) return false

        if (year > CURRENT_YEAR) return false

        return true
    }

    private fun getBirthYearFromResidentNumber(number: String): Int? {
        if (number.length < 7) return null

        val yearPart = number.take(2).toIntOrNull() ?: return null
        val genderDigit = number[6].toString().toIntOrNull() ?: return null

        val century = when (genderDigit) {
            1, 2, 5, 6 -> 1900
            3, 4, 7, 8 -> 2000
            else -> return null
        }

        return century + yearPart
    }

    enum class DateValidation {
        VALID,
        INVALID_FORMAT,
        FUTURE_DATE,
        TOO_OLD,
        BEFORE_BIRTH
    }

    private fun validateDate(year: Int, month: Int, day: Int, birthYear: Int?): DateValidation {
        if (!isValidDateComponents(year, month, day)) {
            return DateValidation.INVALID_FORMAT
        }

        if (year > CURRENT_YEAR) {
            return DateValidation.FUTURE_DATE
        }

        if (year < 1950) {
            return DateValidation.TOO_OLD
        }

        birthYear?.let {
            val minIssueYear = it + 17
            if (year < minIssueYear) {
                return DateValidation.BEFORE_BIRTH
            }
        }

        return DateValidation.VALID
    }

    private fun isValidDateComponents(year: Int, month: Int, day: Int): Boolean {
        if (month !in 1..12) return false

        val maxDay = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> return false
        }

        if (day !in 1..maxDay) return false

        return true
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun hasOverlap(str1: String, str2: String, minOverlap: Int): Boolean {
        if (str1.length < minOverlap || str2.length < minOverlap) return false

        for (len in minOverlap..minOf(str1.length, str2.length)) {
            for (i in 0..str1.length - len) {
                val substr = str1.substring(i, i + len)
                if (str2.contains(substr)) return true
            }
        }
        return false
    }
}