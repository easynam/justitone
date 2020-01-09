grammar JI;
DIGIT: [0-9];

DEF: 'def';
LABEL: [a-zA-Z][a-zA-Z0-9_]*;

MOD: '^';
REST: '_';
MINUS: '-';
OPEN_TUPLE: '[';
OPEN_FILL: '[*';
CLOSE_TUPLE: ']';
OPEN_BAR: '{';
OPEN_MOD_GROUP: '{^';
CLOSE_BAR: '}';
OPEN_BRACKET: '(';
CLOSE_BRACKET: ')';
REPEAT: '*';
PLUS: '+';
COLON: ':';
COMMA: ',';
ANGLE_BRACKET: '>';
JUMP: '<';

identifier: LABEL;

integer: DIGIT+;

fraction: num=integer ('/' den=integer)?
        | '/' den=integer;

pitch: MINUS? COLON ratio=fraction         #pitchRatio
     | MINUS? ANGLE_BRACKET angle=integer  #pitchAngle
     | MINUS? '\'' integer                 #pitchSuperparticular
     | pitch pitch+                        #pitchMultiple
     | pitch PLUS+                         #pitchPower
     ;

event: (length=fraction)? pitch  #eventNote
     | (length=fraction) pitch?  #eventNote
     | (length=fraction)? REST   #eventRest
     | (length=fraction)? MINUS  #eventHold
     | (length=fraction)? pitch? OPEN_TUPLE polySequence CLOSE_TUPLE        #eventTuple
     | (lengthMultiplier=fraction)? pitch? OPEN_BAR polySequence CLOSE_BAR  #eventBar
     | (lengthMultiplier=fraction)? pitch? OPEN_MOD_GROUP polySequence CLOSE_BAR  #eventModGroup
     | (lengthMultiplier=fraction)? pitch? OPEN_FILL (start=sequence '|')? loop=sequence CLOSE_TUPLE  #eventFill
     | event ('.' pitch)+        #eventChord
     | ('.' pitch)+              #eventChord
     | MOD pitch                 #eventModulation
     | '@' integer               #eventInstrument
     | (length=fraction)? pitch? identifier #eventDef
     ;

polySequence: sequence (COMMA sequence)*;

sequenceItem: event (REPEAT repeats=integer)?                     #eventRepeat
	        | (lengthMultiplier=fraction)? pitch? JUMP (times=integer)?  #jump
	        ;

sequence: WS* (sequenceItem WS+)* sequenceItem? WS*;

song: (def WS+)* tempo=integer COLON WS* sequence EOF;

def: OPEN_BRACKET WS* DEF WS+ identifier WS+ event WS* CLOSE_BRACKET;

COMMENT
    : '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> channel(HIDDEN)
    ;

WS : [ \t\r\n]+;
