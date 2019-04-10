grammar JI;
DIGIT: [0-9];

LABEL: [a-zA-Z][a-zA-Z0-9_]*;

MOD: '^';
REST: '_';
MINUS: '-';
OPEN_TUPLE: '[';
CLOSE_TUPLE: ']';
OPEN_BAR: '{';
CLOSE_BAR: '}';
REPEAT: '*';
PLUS: '+';

integer: DIGIT+;
signed: MINUS? integer;

fraction: integer ('/' integer)?;

angle: signed;

pitch: ':' ratio=fraction #pitchRatio
     | '>' angle          #pitchAngle
     | pitch pitch+       #pitchMultiple
     | pitch PLUS+         #pitchPower
     ;
     
event: (length=fraction)? pitch  #eventNote
     | (length=fraction)? REST   #eventRest
     | (length=fraction)? MINUS  #eventHold
     | (length=fraction)? OPEN_TUPLE WS* (eventRepeat WS+)* eventRepeat WS* CLOSE_TUPLE #eventTuple
     | (length=fraction)? OPEN_BAR WS* (eventRepeat WS+)* eventRepeat WS* CLOSE_BAR     #eventBar
     | MOD pitch                 #eventModulation
     ;
     
eventRepeat: event (REPEAT repeats=integer)?;
     
sequence : tempo=integer ':' WS+ (eventRepeat WS+)* eventRepeat WS* EOF;

WS : [ \t\r\n]+ ;

