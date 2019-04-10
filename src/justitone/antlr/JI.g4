grammar JI;
DIGIT: [0-9];

LABEL: [a-zA-Z][a-zA-Z0-9_]*;

MOD: '^';
REST: '_';
MINUS: '-';
OPEN_TUPLE: '[';
CLOSE_TUPLE: ']';
REPEAT: '*';

integer: DIGIT+;
signed: MINUS? integer;

fraction: integer ('/' integer)?;

angle: signed;

pitch: ':' ratio=fraction #pitchRatio
     | '>' angle          #pitchAngle
     | pitch pitch+        #pitchMultiple
     ;
     
event: (length=fraction)? pitch  #eventNote
     | (length=fraction)? REST   #eventRest
     | (length=fraction)? MINUS  #eventHold
     | (length=fraction)? OPEN_TUPLE WS* (eventRepeat WS+)* eventRepeat WS* CLOSE_TUPLE #eventTuple
     | MOD pitch                 #eventModulation
     ;
     
eventRepeat: event (REPEAT repeats=integer)?;
     
sequence : tempo=integer ':' WS+ (eventRepeat WS+)* eventRepeat EOF;

WS : [ \t\r\n]+ ;

