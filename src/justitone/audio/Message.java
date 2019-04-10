package justitone.audio;

import justitone.Track;

public abstract class Message {
    public static class SetTrack extends Message {
        public Track track;
        public SetTrack(Track track) {
            this.track = track;
        }
    }
    public static class Play extends Message {}
    public static class Stop extends Message {}
}