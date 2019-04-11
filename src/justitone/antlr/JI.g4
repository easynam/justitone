grammar JI;
DIGIT: [0-9];

//LABEL: [a-zA-Z][a-zA-Z0-9_]*;

MOD: '^';
REST: '_';
MINUS: '-';
OPEN_TUPLE: '[';
CLOSE_TUPLE: ']';
OPEN_BAR: '{';
CLOSE_BAR: '}';
REPEAT: '*';
PLUS: '+';
COLON: ':';

integer: DIGIT+;
signed: MINUS? integer;

fraction: integer ('/' integer)?;

angle: signed;

pitch: COLON ratio=fraction #pitchRatio
     | '>' angle          #pitchAngle
     | pitch pitch+       #pitchMultiple
     | pitch PLUS+         #pitchPower
     ;
     
event: (length=fraction)? pitch  #eventNote
     | (length=fraction)? REST   #eventRest
     | (length=fraction)? MINUS  #eventHold
     | (length=fraction)? OPEN_TUPLE sequence CLOSE_TUPLE #eventTuple
     | (length=fraction)? OPEN_BAR sequence CLOSE_BAR     #eventBar
     | MOD pitch                 #eventModulation
     ;
     
eventRepeat: event (REPEAT repeats=integer)?;
     
sequence : WS* (eventRepeat WS+)* eventRepeat WS*;

song: tempo=integer COLON WS* sequence EOF;

WS : [ \t\r\n]+;
