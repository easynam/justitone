package justitone.jidi

import org.apache.commons.math3.fraction.BigFraction

class Periods {
    private var periods: MutableList<ClosedRange<BigFraction>> = mutableListOf()

    fun allocate(start: BigFraction, end: BigFraction) {
        periods.add(start..end)
    }

    fun canAllocate(start: BigFraction, end: BigFraction): Boolean {
        if (periods.size == 0) return true

        val period = start..end

        return periods.none { !overlaps(period, it) }
    }

    private fun overlaps(period1: ClosedRange<BigFraction>, period2: ClosedRange<BigFraction>): Boolean {
        return if (period1.start >= period2.endInclusive) true
        else period2.start >= period1.endInclusive
    }
}
