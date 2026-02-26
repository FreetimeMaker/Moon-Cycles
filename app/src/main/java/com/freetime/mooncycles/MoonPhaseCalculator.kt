package com.freetime.mooncycles

import java.util.Calendar
import java.util.Date
import kotlin.math.*

data class MoonPhaseData(
    val phase: Double,           // 0.0 - 1.0
    val phaseName: String,
    val phaseNameDe: String,
    val emoji: String,
    val illumination: Double,    // 0.0 - 100.0
    val daysUntilNextFullMoon: Int,
    val daysUntilNextNewMoon: Int,
    val nextFullMoonDate: Date,
    val nextNewMoonDate: Date,
    val age: Double,             // days since last new moon
    val isWaxing: Boolean,
    val zodiacSign: String,
    val zodiacSignDe: String,
    val description: String
)

data class MonthlyMoonDay(
    val dayOfMonth: Int,
    val phase: Double,
    val emoji: String,
    val isToday: Boolean,
    val phaseName: String
)

object MoonPhaseCalculator {

    private const val LUNAR_CYCLE = 29.53058867

    fun getMoonPhase(date: Date = Date()): MoonPhaseData {
        val phase = calculatePhase(date)
        val age = phase * LUNAR_CYCLE
        val illumination = calculateIllumination(phase)
        val (name, nameDe, emoji, desc) = getPhaseMeta(phase)
        val isWaxing = phase < 0.5
        val nextFull = getNextPhaseDate(date, 0.5)
        val nextNew = getNextPhaseDate(date, 0.0)
        val daysToFull = daysBetween(date, nextFull)
        val daysToNew = daysBetween(date, nextNew)
        val (zodiac, zodiacDe) = getZodiacSign(date)

        return MoonPhaseData(
            phase = phase,
            phaseName = name,
            phaseNameDe = nameDe,
            emoji = emoji,
            illumination = illumination,
            daysUntilNextFullMoon = daysToFull,
            daysUntilNextNewMoon = daysToNew,
            nextFullMoonDate = nextFull,
            nextNewMoonDate = nextNew,
            age = age,
            isWaxing = isWaxing,
            zodiacSign = zodiac,
            zodiacSignDe = zodiacDe,
            description = desc
        )
    }

    fun getMonthlyCalendar(year: Int, month: Int): List<MonthlyMoonDay> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()

        return (1..daysInMonth).map { day ->
            cal.set(year, month, day)
            val date = cal.time
            val phase = calculatePhase(date)
            val (_, _, emoji, _) = getPhaseMeta(phase)
            val isToday = (today.get(Calendar.YEAR) == year &&
                    today.get(Calendar.MONTH) == month &&
                    today.get(Calendar.DAY_OF_MONTH) == day)
            val phaseName = getSimplePhaseName(phase)
            MonthlyMoonDay(day, phase, emoji, isToday, phaseName)
        }
    }

    private fun calculatePhase(date: Date): Double {
        // Based on Jean Meeus "Astronomical Algorithms"
        val cal = Calendar.getInstance()
        cal.time = date
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val jd = toJulianDay(year, month, day.toDouble())
        val daysSinceKnownNewMoon = jd - 2451549.5
        val phase = (daysSinceKnownNewMoon % LUNAR_CYCLE) / LUNAR_CYCLE
        return if (phase < 0) phase + 1.0 else phase
    }

    private fun toJulianDay(year: Int, month: Int, day: Double): Double {
        var y = year
        var m = month
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun calculateIllumination(phase: Double): Double {
        val angle = phase * 2 * PI
        return ((1 - cos(angle)) / 2.0) * 100.0
    }

    private data class PhaseMeta(val name: String, val nameDe: String, val emoji: String, val desc: String)

    private fun getPhaseMeta(phase: Double): PhaseMeta {
        return when {
            phase < 0.0625 || phase >= 0.9375 -> PhaseMeta(
                "New Moon", "Neumond", "🌑",
                "Zeit der Stille und Neuanfänge. Setze Absichten und plane neue Projekte."
            )
            phase < 0.1875 -> PhaseMeta(
                "Waxing Crescent", "Zunehmende Sichel", "🌒",
                "Energie steigt. Beginne mit der Umsetzung deiner Pläne."
            )
            phase < 0.3125 -> PhaseMeta(
                "First Quarter", "Erstes Viertel", "🌓",
                "Entscheidungszeit. Überwinde Hindernisse und bleib fokussiert."
            )
            phase < 0.4375 -> PhaseMeta(
                "Waxing Gibbous", "Zunehmender Mond", "🌔",
                "Verfeinere und verbessere. Die Energie ist auf dem Höhepunkt."
            )
            phase < 0.5625 -> PhaseMeta(
                "Full Moon", "Vollmond", "🌕",
                "Höchste Energie und Klarheit. Feiere Erfolge und lass los."
            )
            phase < 0.6875 -> PhaseMeta(
                "Waning Gibbous", "Abnehmender Mond", "🌖",
                "Zeit des Dankens und Teilens. Gib Wissen weiter."
            )
            phase < 0.8125 -> PhaseMeta(
                "Last Quarter", "Letztes Viertel", "🌗",
                "Loslassen und Reinigen. Beende was nicht mehr dient."
            )
            else -> PhaseMeta(
                "Waning Crescent", "Abnehmende Sichel", "🌘",
                "Ruhe und Reflexion. Bereite dich auf den nächsten Zyklus vor."
            )
        }
    }

    private fun getSimplePhaseName(phase: Double): String {
        return when {
            phase < 0.0625 || phase >= 0.9375 -> "Neumond"
            phase < 0.1875 -> "Zunehmend"
            phase < 0.3125 -> "1. Viertel"
            phase < 0.4375 -> "Zunehmend"
            phase < 0.5625 -> "Vollmond"
            phase < 0.6875 -> "Abnehmend"
            phase < 0.8125 -> "Letztes ¼"
            else -> "Abnehmend"
        }
    }

    private fun getNextPhaseDate(from: Date, targetPhase: Double): Date {
        val cal = Calendar.getInstance()
        cal.time = from
        var searchDate = cal.time
        var currentPhase = calculatePhase(searchDate)

        // Find when we cross the target phase
        for (i in 1..35) {
            cal.add(Calendar.HOUR_OF_DAY, 12)
            searchDate = cal.time
            val newPhase = calculatePhase(searchDate)

            val crossedTarget = when {
                targetPhase == 0.0 -> (currentPhase > 0.9 && newPhase < 0.1)
                targetPhase == 0.5 -> (currentPhase < 0.5 && newPhase >= 0.5)
                else -> (currentPhase < targetPhase && newPhase >= targetPhase)
            }
            if (crossedTarget) return searchDate
            currentPhase = newPhase
        }
        // fallback: add ~half cycle or full cycle
        cal.time = from
        cal.add(Calendar.DAY_OF_MONTH, if (targetPhase == 0.5) 15 else 29)
        return cal.time
    }

    private fun daysBetween(from: Date, to: Date): Int {
        val diff = to.time - from.time
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun getZodiacSign(date: Date): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.time = date
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        return when {
            (month == 3 && day >= 21) || (month == 4 && day <= 19) -> Pair("Aries", "Widder ♈")
            (month == 4 && day >= 20) || (month == 5 && day <= 20) -> Pair("Taurus", "Stier ♉")
            (month == 5 && day >= 21) || (month == 6 && day <= 20) -> Pair("Gemini", "Zwillinge ♊")
            (month == 6 && day >= 21) || (month == 7 && day <= 22) -> Pair("Cancer", "Krebs ♋")
            (month == 7 && day >= 23) || (month == 8 && day <= 22) -> Pair("Leo", "Löwe ♌")
            (month == 8 && day >= 23) || (month == 9 && day <= 22) -> Pair("Virgo", "Jungfrau ♍")
            (month == 9 && day >= 23) || (month == 10 && day <= 22) -> Pair("Libra", "Waage ♎")
            (month == 10 && day >= 23) || (month == 11 && day <= 21) -> Pair("Scorpio", "Skorpion ♏")
            (month == 11 && day >= 22) || (month == 12 && day <= 21) -> Pair("Sagittarius", "Schütze ♐")
            (month == 12 && day >= 22) || (month == 1 && day <= 19) -> Pair("Capricorn", "Steinbock ♑")
            (month == 1 && day >= 20) || (month == 2 && day <= 18) -> Pair("Aquarius", "Wassermann ♒")
            else -> Pair("Pisces", "Fische ♓")
        }
    }
}