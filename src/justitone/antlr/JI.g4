grammar JI;
DIGIT: [0-9];

LABEL: [a-zA-Z][a-zA-Z0-9_]*;

MOD: '^';
REST: '_';
MINUS: '-';
OPEN_TUPLE: '[';
CLOSE_TUPLE: ']';

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
     | (length=fraction)? OPEN_TUPLE WS* (event WS+)* event WS* CLOSE_TUPLE #eventTuple
     | MOD pitch                 #eventModulation
     ;
     
     
sequence : LABEL ':' WS* (event WS+)* event ;

WS : [ \t\r\n]+ ;
