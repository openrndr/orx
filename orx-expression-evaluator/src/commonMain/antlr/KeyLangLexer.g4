lexer grammar KeyLangLexer;

channels { WHITESPACE }

// Whitespace
NEWLINE            : '\r\n' | '\r' | '\n' ;
WS                 : [\t ]+ -> channel(WHITESPACE) ;


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
LBRACKET           : '[' ;
RBRACKET           : ']' ;
LCURLY             : '{' ;
RCURLY             : '}' ;

QUESTION_MARK      : '?' ;
COLON              : ':' ;

ARROW              : '->' ;

COMMA              : ',' ;
DOT                : '.' ;

EQ             : '==' ;
LT             : '<' ;
LTEQ           : '<=' ;
GT             : '>' ;
GTEQ           : '>='  ;

AND            : '&&' ;
OR             : '||'  ;
NOT            : '!'   ;

STRING_OPEN        : '"' -> pushMode(MODE_IN_STRING);

UNMATCHED          : . ;

mode MODE_IN_STRING;

ESCAPE_STRING_DELIMITER : '\\"' ;
ESCAPE_SLASH            : '\\\\' ;
ESCAPE_NEWLINE          : '\\n' ;
ESCAPE_SHARP            : '\\#' ;
STRING_CLOSE            : '"' -> popMode ;
STRING_CONTENT          : ~["\n\r\t\\#]+ ;

