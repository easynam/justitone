package justitone.jidi

data class JidiTrack(
        val id: Int,
        val events: MutableList<JidiEvent> = mutableListOf(),
        val periods: Periods = Periods()) {
    fun add(e: JidiEvent) = events.add(e)
}
