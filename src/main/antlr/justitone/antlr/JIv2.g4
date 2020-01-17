grammar JIv2;
DIGIT: [0-9];

DEF: 'def';
LABEL: [a-zA-Z][a-zA-Z0-9_]*;

REST: '_';
MINUS: '-';
DIVIDE: '/';
SUPER: '\'';
OPEN_GROUP: '[';
CLOSE_GROUP: ']';
PLUS: '+';
COLON: ':';
COMMA: ',';

integer: DIGIT+;

fraction: num=integer (DIVIDE den=integer)?
        | DIVIDE den=integer;

pitch: MINUS? COLON ratio=fraction         # pitchRatio
     | MINUS? SUPER integer                # pitchSuperparticular
     ;

event: pitch                               # eventPitch
     | fraction                            # eventDuration
     | OPEN_GROUP polyGroup CLOSE_GROUP    # eventGroup
     | event event                         # eventMultiplied
     ;

polyGroup: group (COMMA group)*;

group: WS* (event WS+)* event?;

song: WS* tempo=integer COLON WS* event WS* EOF;

//def: OPEN_BRACKET WS* DEF WS+ identifier WS+ event WS* CLOSE_BRACKET;

COMMENT
    : '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> channel(HIDDEN)
    ;

WS : [ \t\r\n]+;
