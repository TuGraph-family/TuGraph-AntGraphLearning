grammar Filter;
/*-----------------
Parser Rules
*/
start:(expr)*;

expr:
    literal_value                                                   #LiteralExp
    | unary_operator expr                                           #UnaryExp
    | expr ( STAR | DIV | MOD) expr                                 #StarDivExp
    | expr ( PLUS | MINUS) expr                                     #PlusMinusExp
    | expr (
        LT
        | LT_EQ
        | GT
        | GT_EQ
        | ASSIGN
        | EQ
        | NOT_EQ1
        | NOT_EQ2
        ) expr                                                      #CompareExp
    | expr ( IN_ | NOT_ IN_) expr                                   #CategoryExp
    | expr AND_ expr                                                #AndExp
    | expr OR_ expr                                                 #OrExp
    | OPEN_PAR expr (COMMA expr) * CLOSE_PAR                        #ParExp
    | column_name                                                   #ColumnExp
;

keyword:
    AND_
    | AS_
    | IN_
    | NO_
    | NOT_
    | OF_
    | ON_
    | OR_
;
any_name:
    IDENTIFIER
    | keyword
    | STRING_LITERAL
    | OPEN_PAR any_name CLOSE_PAR
;

column_name:
    any_name
;

unary_operator:
    MINUS
    | PLUS
    | NOT_
;


literal_value:
    NUMERIC_LITERAL
    | STRING_LITERAL
;
/*
 * Lexer Rules
 */

SCOL:      ';';
//DOT:       '.';
OPEN_PAR:  '(';
CLOSE_PAR: ')';
COMMA:     ',';
ASSIGN:    '=';
STAR:      '*';
PLUS:      '+';
MINUS:     '-';
DIV:       '/';
MOD:       '%';
LT:        '<';
LT_EQ:     '<=';
GT:        '>';
GT_EQ:     '>=';
EQ:        '==';
NOT_EQ1:   '!=';
NOT_EQ2:   '<>';

AND_:               'AND'|'and';
AS_:                'AS';
IN_:                'IN'|'in';
NO_:                'NO';
NOT_:               'NOT'|'not';
OF_:                'OF';
ON_:                'ON';
OR_:                'OR'|'or';

IDENTIFIER:
    '"' (~'"' | '""')* '"'
    | '`' (~'`' | '``')* '`'
    | '[' ~']'* ']'
    | [A-z_] [.A-z_0-9]*
; // TODO check: needs more chars in set

NUMERIC_LITERAL: ((DIGIT+ ('.' DIGIT*)?) | ('.' DIGIT+)) ('E' [-+]? DIGIT+)? | '0x' HEX_DIGIT+;

STRING_LITERAL: '\'' ( ~'\'' | '\'\'')* '\'';

SPACES: [ \u000B\t\r\n] -> channel(HIDDEN);

UNEXPECTED_CHAR: .;

fragment HEX_DIGIT: [0-9A-F];
fragment DIGIT:     [0-9];
