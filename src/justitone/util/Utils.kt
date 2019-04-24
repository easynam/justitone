package justitone.util

import org.apache.commons.math3.fraction.BigFraction
import java.math.BigInteger

infix fun BigInteger.over(denominator: BigInteger) = BigFraction(this, denominator)
infix fun Long.over(denominator: Long) = BigFraction(this, denominator)
infix fun Int.over(denominator: Int) = BigFraction(this, denominator)
operator fun BigFraction.plus(other: BigFraction) = add(other)
operator fun BigFraction.plus(other: Int) = add(other)
operator fun BigFraction.plus(other: Long) = add(other)
operator fun BigFraction.plus(other: BigInteger) = add(other)
operator fun BigFraction.minus(other: BigFraction) = subtract(other)
operator fun BigFraction.minus(other: Int) = subtract(other)
operator fun BigFraction.minus(other: Long) = subtract(other)
operator fun BigFraction.minus(other: BigInteger) = subtract(other)
operator fun BigFraction.times(other: BigFraction) = multiply(other)
operator fun BigFraction.times(other: Int) = multiply(other)
operator fun BigFraction.times(other: Long) = multiply(other)
operator fun BigFraction.times(other: BigInteger) = multiply(other)
operator fun BigFraction.div(other: BigFraction) = divide(other)
operator fun BigFraction.div(other: Int) = divide(other)
operator fun BigFraction.div(other: Long) = divide(other)
operator fun BigFraction.div(other: BigInteger) = divide(other)
