
parser grammar KeyLangParser;

@header {
package org.openrndr.extra.keyframer.antlr;
}

options { tokenVocab=KeyLangLexer; }

keyLangFile : lines=line+ ;

line      : statement (NEWLINE | EOF) ;

statement : inputDeclaration # inputDeclarationStatement
          | varDeclaration   # varDeclarationStatement
          | assignment       # assignmentStatement
          | print            # printStatement
          | expression       # expressionStatement ;

print : PRINT LPAREN expression RPAREN ;

inputDeclaration : INPUT type name=ID ;

varDeclaration : VAR assignment ;

assignment : ID ASSIGN expression ;

expression : INTLIT                                                        # intLiteral
           | DECLIT                                                        # decimalLiteral
           | ID LPAREN RPAREN                                              # functionCall0Expression
           | ID LPAREN expression RPAREN                                   # functionCall1Expression
           | ID LPAREN expression COMMA expression RPAREN                  # functionCall2Expression
           | ID LPAREN expression COMMA expression COMMA expression RPAREN # functionCall3Expression
           | ID LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN # functionCall4Expression
           | ID LPAREN expression COMMA expression COMMA expression COMMA expression COMMA expression RPAREN # functionCall5Expression
           | ID                                                            # valueReference
           | LPAREN expression RPAREN                                      # parenExpression
           | MINUS expression                                              # minusExpression
           | expression operator=(DIVISION|ASTERISK|PERCENTAGE) expression # binaryOperation1
           | expression operator=(PLUS|MINUS) expression        # binaryOperation2;

type : DECIMAL # decimal
     | INT     # integer
     | STRING  # string ;


