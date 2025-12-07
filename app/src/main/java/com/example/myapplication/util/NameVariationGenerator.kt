package com.example.myapplication.util

class NameVariationGenerator {

    // 초성 19개
    private val chosung = listOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )

    // 중성 21개
    private val jungsung = listOf(
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
        'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    )

    // 종성 28개 (없음 포함)
    private val jongsung = listOf(
        ' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
        'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )

    // 비슷한 초성 매핑
    private val similarChosung = mapOf(
        'ㄱ' to listOf('ㅋ', 'ㄲ'),
        'ㄲ' to listOf('ㄱ', 'ㅋ'),
        'ㄴ' to listOf('ㄹ', 'ㅁ'),
        'ㄷ' to listOf('ㅌ', 'ㄸ'),
        'ㄸ' to listOf('ㄷ', 'ㅌ'),
        'ㄹ' to listOf('ㄴ', 'ㅇ'),
        'ㅁ' to listOf('ㅂ', 'ㅇ'),
        'ㅂ' to listOf('ㅍ', 'ㅃ', 'ㅁ'),
        'ㅃ' to listOf('ㅂ', 'ㅍ'),
        'ㅅ' to listOf('ㅆ', 'ㅈ'),
        'ㅆ' to listOf('ㅅ', 'ㅈ'),
        'ㅇ' to listOf('ㅎ', 'ㄹ'),
        'ㅈ' to listOf('ㅊ', 'ㅉ', 'ㅅ'),
        'ㅉ' to listOf('ㅈ', 'ㅊ'),
        'ㅊ' to listOf('ㅈ', 'ㅌ'),
        'ㅋ' to listOf('ㄱ', 'ㅎ'),
        'ㅌ' to listOf('ㄷ', 'ㅊ'),
        'ㅍ' to listOf('ㅂ', 'ㅎ'),
        'ㅎ' to listOf('ㅇ', 'ㅋ')
    )

    // 비슷한 중성 매핑
    private val similarJungsung = mapOf(
        'ㅏ' to listOf('ㅑ', 'ㅓ', 'ㅐ'),
        'ㅐ' to listOf('ㅔ', 'ㅏ', 'ㅒ'),
        'ㅑ' to listOf('ㅏ', 'ㅕ', 'ㅒ'),
        'ㅒ' to listOf('ㅖ', 'ㅑ', 'ㅐ'),
        'ㅓ' to listOf('ㅕ', 'ㅏ', 'ㅔ'),
        'ㅔ' to listOf('ㅐ', 'ㅓ', 'ㅖ'),
        'ㅕ' to listOf('ㅓ', 'ㅑ', 'ㅖ'),
        'ㅖ' to listOf('ㅔ', 'ㅕ', 'ㅒ'),
        'ㅗ' to listOf('ㅛ', 'ㅜ', 'ㅘ'),
        'ㅘ' to listOf('ㅗ', 'ㅙ', 'ㅏ'),
        'ㅙ' to listOf('ㅘ', 'ㅚ', 'ㅐ'),
        'ㅚ' to listOf('ㅙ', 'ㅟ', 'ㅔ'),
        'ㅛ' to listOf('ㅗ', 'ㅠ', 'ㅕ'),
        'ㅜ' to listOf('ㅠ', 'ㅗ', 'ㅝ'),
        'ㅝ' to listOf('ㅜ', 'ㅞ', 'ㅓ'),
        'ㅞ' to listOf('ㅝ', 'ㅟ', 'ㅔ'),
        'ㅟ' to listOf('ㅚ', 'ㅞ', 'ㅣ'),
        'ㅠ' to listOf('ㅜ', 'ㅛ', 'ㅕ'),
        'ㅡ' to listOf('ㅣ', 'ㅜ', 'ㅢ'),
        'ㅢ' to listOf('ㅡ', 'ㅣ', 'ㅟ'),
        'ㅣ' to listOf('ㅡ', 'ㅢ', 'ㅟ')
    )

    // 비슷한 종성 매핑
    private val similarJongsung = mapOf(
        ' ' to listOf('ㅇ', 'ㄴ'),
        'ㄱ' to listOf('ㅋ', 'ㄲ', ' '),
        'ㄲ' to listOf('ㄱ', 'ㅋ'),
        'ㄴ' to listOf('ㄹ', 'ㅁ', 'ㅇ'),
        'ㄷ' to listOf('ㅅ', 'ㅌ', 'ㅈ'),
        'ㄹ' to listOf('ㄴ', 'ㅇ', ' '),
        'ㅁ' to listOf('ㄴ', 'ㅂ', 'ㅇ'),
        'ㅂ' to listOf('ㅁ', 'ㅍ', ' '),
        'ㅅ' to listOf('ㄷ', 'ㅆ', 'ㅈ'),
        'ㅆ' to listOf('ㅅ', 'ㄷ'),
        'ㅇ' to listOf('ㄴ', 'ㅁ', ' '),
        'ㅈ' to listOf('ㅅ', 'ㄷ', 'ㅊ'),
        'ㅊ' to listOf('ㅈ', 'ㅅ'),
        'ㅋ' to listOf('ㄱ', 'ㄲ'),
        'ㅌ' to listOf('ㄷ', 'ㅅ'),
        'ㅍ' to listOf('ㅂ', ' '),
        'ㅎ' to listOf('ㅇ', ' ')
    )

    private val commonNameChars = setOf(
        '가', '강', '건', '경', '고', '광', '구', '규', '근', '기',
        '나', '남', '다', '단', '대', '도', '동', '두',
        '라', '란', '래', '려', '련', '령', '로', '리', '린',
        '마', '만', '명', '민', '미',
        '바', '배', '백', '범', '병', '보', '빈',
        '사', '상', '서', '석', '선', '설', '섭', '성', '세', '소', '솔', '수', '숙', '순', '슬', '승', '시', '신',
        '아', '안', '애', '연', '영', '예', '오', '용', '우', '욱', '운', '원', '월', '유', '윤', '율', '은', '의', '이', '인', '일', '임',
        '자', '장', '재', '전', '정', '제', '조', '종', '주', '준', '중', '지', '진',
        '찬', '창', '채', '천', '철', '청', '초', '춘', '충',
        '태', '택', '하', '한', '해', '혁', '현', '형', '혜', '호', '홍', '화', '환', '효', '훈', '휘', '희'
    )

    // 한글 글자를 초성, 중성, 종성으로 분리
    private fun decompose(char: Char): Triple<Int, Int, Int>? {
        val code = char.code - 0xAC00
        if (code < 0 || code > 11171) return null

        val cho = code / (21 * 28)
        val jung = (code % (21 * 28)) / 28
        val jong = code % 28

        return Triple(cho, jung, jong)
    }

    // 초성, 중성, 종성을 합쳐서 한글 글자로
    private fun compose(cho: Int, jung: Int, jong: Int): Char {
        return (0xAC00 + cho * 21 * 28 + jung * 28 + jong).toChar()
    }

    // 한 글자에서 비슷한 글자들 생성
    private fun generateSimilarChars(char: Char): List<Char> {
        val decomposed = decompose(char) ?: return emptyList()
        val (cho, jung, jong) = decomposed

        val results = mutableListOf<Char>()

        // 초성 변경
        val choChar = chosung[cho]
        similarChosung[choChar]?.forEach { newCho ->
            val newChoIndex = chosung.indexOf(newCho)
            if (newChoIndex >= 0) {
                results.add(compose(newChoIndex, jung, jong))
            }
        }

        // 중성 변경
        val jungChar = jungsung[jung]
        similarJungsung[jungChar]?.forEach { newJung ->
            val newJungIndex = jungsung.indexOf(newJung)
            if (newJungIndex >= 0) {
                results.add(compose(cho, newJungIndex, jong))
            }
        }

        // 종성 변경
        val jongChar = jongsung[jong]
        similarJongsung[jongChar]?.forEach { newJong ->
            val newJongIndex = jongsung.indexOf(newJong)
            if (newJongIndex >= 0) {
                results.add(compose(cho, jung, newJongIndex))
            }
        }

        return results.distinct()
    }

    fun generateVariations(name: String, count: Int = 3): List<String> {
        if (name.length < 2) return emptyList()

        val surname = name.first()
        val givenName = name.drop(1)

        val variations = mutableSetOf<String>()

        for (i in givenName.indices) {
            val char = givenName[i]
            val similars = generateSimilarChars(char)

            for (similar in similars) {
                if (similar != char) {
                    val newGivenName = StringBuilder(givenName)
                    newGivenName.setCharAt(i, similar)
                    val newName = "$surname$newGivenName"

                    if (newName != name) {
                        variations.add(newName)
                    }
                }
            }
        }

        // 자연스러운 이름만 필터링
        return variations
            .filter { newName ->
                newName.drop(1).all { it in commonNameChars }
            }
            .shuffled()
            .take(count)
    }
}