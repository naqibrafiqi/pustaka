package my.naqibrafiqi.pustaka.data.library

/**
 * Compares strings the way people expect filenames to be sorted: runs of digits are compared as
 * numbers, so `Vol 9` comes before `Vol 10`, and letters are compared without regard to case.
 */
object NaturalOrder : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isAsciiDigit() && b[j].isAsciiDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isAsciiDigit()) i++
                while (j < b.length && b[j].isAsciiDigit()) j++
                val numberA = a.substring(startA, i).trimStart('0')
                val numberB = b.substring(startB, j).trimStart('0')
                // Without leading zeros, a longer run of digits is a bigger number.
                if (numberA.length != numberB.length) return numberA.length - numberB.length
                val byDigits = numberA.compareTo(numberB)
                if (byDigits != 0) return byDigits
            } else {
                val byChar = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
                if (byChar != 0) return byChar
                i++
                j++
            }
        }
        val byRemaining = (a.length - i) - (b.length - j)
        if (byRemaining != 0) return byRemaining
        // Equal so far (e.g. "Vol 01" and "vol 1"): fall back to a plain comparison so the order is
        // always the same.
        return a.compareTo(b)
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
