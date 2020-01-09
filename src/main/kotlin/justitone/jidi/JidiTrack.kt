package justitone.jidi

class JidiTrack(var id: Int) {
    var events: MutableList<JidiEvent> = mutableListOf()

    fun add(e: JidiEvent) = events.add(e)
}
