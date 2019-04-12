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
COMMA: ',';
ANGLE_BRACKET: '>';
JUMP: '<';

integer: DIGIT+;
signed: MINUS? integer;

fraction: integer ('/' integer)?;

angle: signed;

pitch: COLON ratio=fraction #pitchRatio
     | ANGLE_BRACKET angle          #pitchAngle
     | pitch pitch+       #pitchMultiple
     | pitch PLUS+         #pitchPower
     ;
     
event: (length=fraction)? pitch  #eventNote
     | (length=fraction)? REST   #eventRest
     | (length=fraction)? MINUS  #eventHold
     | (length=fraction)? OPEN_TUPLE polySequence CLOSE_TUPLE        #eventTuple
     | (lengthMultiplier=fraction)? OPEN_BAR polySequence CLOSE_BAR  #eventBar
     | MOD pitch                 #eventModulation
     ;

polySequence: sequence (COMMA sequence)*;
     
sequenceItem: event (REPEAT repeats=integer)?          #eventRepeat
	        | (lengthMultiplier=fraction)? JUMP (times=integer)  #jump
	        ;
     
sequence: WS* (sequenceItem WS+)* sequenceItem WS*;

song: tempo=integer COLON WS* sequence EOF;

WS : [ \t\r\n]+;
