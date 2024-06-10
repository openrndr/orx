
parser grammar KeyLangParser;

options { tokenVocab=KeyLangLexer; }

keyLangFile : lines=line+ ;

line      : statement (NEWLINE | EOF) ;

statement :
           expression       # expressionStatement ;

expression : INTLIT                                                        # intLiteral
           | DECLIT                                                        # decimalLiteral
           | expression DOT ID LPAREN RPAREN                               # memberFunctionCall0Expression
           | expression DOT ID LPAREN expression RPAREN                    # memberFunctionCall1Expression
           | expression DOT ID LPAREN expression COMMA expression RPAREN   # memberFunctionCall2Expression
           | expression DOT ID LPAREN expression COMMA expression COMMA expression RPAREN  # memberFunctionCall3Expression
           | expression DOT ID LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN  # memberFunctionCall4Expression
           | ID LPAREN RPAREN                                              # functionCall0Expression
           | ID LPAREN expression RPAREN                                   # functionCall1Expression
           | ID LPAREN expression COMMA expression RPAREN                  # functionCall2Expression
           | ID LPAREN expression COMMA expression COMMA expression RPAREN # functionCall3Expression
           | ID LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN # functionCall4Expression
           | ID LPAREN expression COMMA expression COMMA expression COMMA expression COMMA expression RPAREN # functionCall5Expression
           | ID                                                            # valueReference
           | STRING_OPEN (parts+=stringLiteralContent)* STRING_CLOSE       # stringLiteral
           | expression DOT ID                                             # propReference
           | LPAREN expression RPAREN                                      # parenExpression
           | MINUS expression                                              # minusExpression
           | NOT expression                                                # negateExpression
           | expression operator=(DIVISION|ASTERISK|PERCENTAGE) expression # binaryOperation1
           | expression operator=(PLUS|MINUS) expression        # binaryOperation2
           | expression operator=(EQ|LT|LTEQ|GT|GTEQ) expression # comparisonOperation
           | expression operator=(AND|OR) expression             # joinOperation
           | expression QUESTION_MARK expression COLON expression          # ternaryExpression;

stringLiteralContent    : STRING_CONTENT;