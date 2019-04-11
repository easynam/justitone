package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

import justitone.Sequence;

//java intonation digital interface
public class JidiSequence {
    public List<JidiTrack> tracks;
    
    public JidiSequence(Sequence track) {
        tracks = new ArrayList<>();
        
        tracks.add(new JidiTrack(track));
    }
}
