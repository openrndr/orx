lexer grammar KeyLangLexer;

@header {
package org.openrndr.extra.keyframer.antlr;
}

channels { WHITESPACE }

// Whitespace
NEWLINE            : '\r\n' | '\r' | '\n' ;
WS                 : [\t ]+ -> channel(WHITESPACE) ;

// Keywords
INPUT              : 'input' ;
VAR                : 'var' ;
PRINT              : 'print';
AS                 : 'as';
INT                : 'Int';
DECIMAL            : 'Decimal';
STRING             : 'String';

// Identifiers
ID                 : [$_]*[a-zA-Z][A-Za-z0-9_]* | '`'[$_]*[A-Za-z0-9_-]*'`';
FUNCTION_ID        : [$_]*[a-z][A-Za-z0-9_]* ;

// Literals

DECLIT             : [0-9][0-9]* '.' [0-9]+ ;
INTLIT             : '0'|[0-9][0-9]* ;

// Operators
PLUS               : '+' ;
PERCENTAGE         : '%' ;
MINUS              : '-' ;
ASTERISK           : '*' ;
DIVISION           : '/' ;
ASSIGN             : '=' ;
LPAREN             : '(' ;
RPAREN             : ')' ;


COMMA              : ',' ;

STRING_OPEN        : '"' -> pushMode(MODE_IN_STRING);

UNMATCHED          : . ;

mode MODE_IN_STRING;

ESCAPE_STRING_DELIMITER : '\\"' ;
ESCAPE_SLASH            : '\\\\' ;
ESCAPE_NEWLINE          : '\\n' ;
ESCAPE_SHARP            : '\\#' ;
STRING_CLOSE            : '"' -> popMode ;
STRING_CONTENT          : ~["\n\r\t\\#]+ ;

