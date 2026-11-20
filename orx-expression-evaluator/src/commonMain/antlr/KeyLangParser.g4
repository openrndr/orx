
parser grammar KeyLangParser;

options { tokenVocab=KeyLangLexer; }

// statements are separated by semicolons or new lines; a program gives the value of its last statement, when that is
// an expression
keyLangFile : separator* lines=line+ ;

line      : statement (separator+ | EOF) ;

separator : NEWLINE | SEMICOLON ;

statement :
           VAL ID ASSIGN expressionRoot       # valDeclaration
         | FUN ID LPAREN (ID ( COMMA ID )*)? RPAREN (ASSIGN expressionRoot | block)  # funDeclaration
         | expressionRoot                     # expressionStatement ;

// the body of a function: statements that end in the expression that gives its value
block : LCURLY separator* (statement separator+)* expressionRoot separator* RCURLY ;

rangeExpression: expression operator=(RANGE_INCLUSIVE|RANGE_EXCLUSIVE|RANGE_EXCLUSIVE_UNTIL|RANGE_DOWNTO) expression (step=STEP expression)?;

expressionRoot: rangeExpression
              | expression
              ;

// the body of a lambda can span lines: statements that end in the expression that gives its value
lambda: LCURLY separator* (ID ( COMMA ID )* ARROW separator* )? (statement separator+)* expressionRoot separator* RCURLY  # functionLiteral;

expression : INTLIT                                                        # intLiteral
           | DECLIT                                                        # decimalLiteral
           | LBRACKET (expressionRoot ( COMMA expressionRoot )*)? RBRACKET             # listLiteral
           | expression DOT ID lambda                                      # memberFunctionCall0LambdaExpression
           | lambda                                                        # lambdaExpression
           | expression DOT ID LPAREN RPAREN                               # memberFunctionCall0Expression
           | expression DOT ID LPAREN expression RPAREN                    # memberFunctionCall1Expression
           | expression DOT ID LPAREN expression COMMA expression RPAREN   # memberFunctionCall2Expression
           | expression DOT ID LPAREN expression COMMA expression COMMA expression RPAREN  # memberFunctionCall3Expression
           | expression DOT ID LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN  # memberFunctionCall4Expression
           | expression LBRACKET expression RBRACKET                       # indexExpression
           | ID LPAREN RPAREN                                              # functionCall0Expression
           | ID LPAREN expression RPAREN                                   # functionCall1Expression
           | ID LPAREN expression COMMA expression RPAREN                  # functionCall2Expression
           | ID LPAREN expression COMMA expression COMMA expression RPAREN # functionCall3Expression
           | ID LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN # functionCall4Expression
           | ID LPAREN expression COMMA expression COMMA expression COMMA expression COMMA expression RPAREN # functionCall5Expression
           | ID                                                            # valueReference
           | STRING_OPEN (parts+=stringLiteralContent)* STRING_CLOSE       # stringLiteral
           | expression DOT ID                                             # propReference
           | LPAREN expressionRoot RPAREN                                      # parenExpression
           | MINUS expression                                              # minusExpression
           | NOT expression                                                # negateExpression
           | expression operator=(DIVISION|ASTERISK|PERCENTAGE) expression # binaryOperation1
           | expression operator=(PLUS|MINUS) expression        # binaryOperation2
           | expression operator=(EQ|LT|LTEQ|GT|GTEQ) expression # comparisonOperation
           | expression operator=(AND|OR) expression             # joinOperation
           | expression QUESTION_MARK expression COLON expression          # ternaryExpression;

stringLiteralContent    : STRING_CONTENT;