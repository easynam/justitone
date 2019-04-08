grammar JI;
DIGIT: [0-9];

LABEL: [a-zA-Z][a-zA-Z0-9_]*;

MOD: '^';

integer: DIGIT+;
fraction: integer ('/' integer)?;

event: length=fraction ':' ratio=fraction WS* #note
     | MOD ratio=fraction WS* #modulation;

sequence : LABEL '>' WS* event+ WS*;

WS : [ \t\r\n]+ -> skip;
