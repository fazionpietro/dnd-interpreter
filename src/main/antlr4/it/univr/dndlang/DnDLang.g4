grammar DnDLang;
options { language = Java; }


HERO      : 'hero'      ;
FOE       : 'foe'       ;
DEF       : 'def'       ;
RETURN    : 'return'    ;
QUEST     : 'quest'     ;
IF        : 'if'        ;
ELSE      : 'else'      ;
WHILE     : 'while'     ;
SWITCH    : 'switch'    ;
CASE      : 'case'      ;
DEFAULT   : 'default'   ;
PRINT     : 'print'     ;

ADV  : 'adv' ;
DIS  : 'dis' ;
SAVE : 'save';
VS   : 'vs'  ;
D20  : 'd20' ;
D12  : 'd12' ;
D10  : 'd10' ;
D8   : 'd8'  ;
D6   : 'd6'  ;
D4   : 'd4'  ;
D3   : 'd3'  ;


TYPE_INT    : 'Int'    ;
TYPE_FLOAT  : 'Float'  ;
TYPE_BOOL   : 'Bool'   ;
TYPE_STRING : 'String' ;
TYPE_HP     : 'HP'     ;
TYPE_AC     : 'AC'     ;
TYPE_GOLD   : 'Gold'   ;
TYPE_VOID   : 'Void'   ;


BOOL    : 'true' | 'false' ;
INT     : [0-9]+ ;
FLOAT   : [0-9]+ '.' [0-9]+ ;
ISTRING : 'i"' (~'"')* '"' ;
STRING  : '"' (~'"')* '"' ;

PLUS    : '+' ; MINUS   : '-' ; STAR    : '*' ; SLASH   : '/' ; PERCENT : '%' ;
EQ      : '==' ; NEQ     : '!=' ; LT      : '<'  ; GT      : '>'  ; LE      : '<=' ; GE      : '>=' ;
AND     : '&&' ; OR      : '||' ; NOT     : '!'  ;
ASSIGN  : '=' ; PLUS_ASSIGN  : '+='; PLUS_PLUS  :  '++'; MINUS_ASSIGN  : '-='; MINUS_MINUS  : '--';
STAR_ASSIGN  : '*='; SLASH_ASSIGN  : '/='; QUESTION  : '?';

SEMI    : ';' ; COLON   : ':' ; COMMA   : ',' ;
LPAREN  : '(' ; RPAREN  : ')' ;
LBRACE  : '{' ; RBRACE  : '}' ;

ID : [a-zA-Z_][a-zA-Z_0-9]* ('.' [a-zA-Z_][a-zA-Z_0-9]*)* ;

WS            : [ \t\r\n]+ -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;


program
    : functionSection? heroSection? foeSection? questSection EOF
    ;

heroSection      : HERO COLON block     ;
foeSection       : FOE COLON block      ;
questSection     : QUEST COLON block    ;
functionSection  : functionDecl+        ;


functionDecl
    : DEF (TYPE_INT | TYPE_FLOAT | TYPE_BOOL | TYPE_STRING | TYPE_HP | TYPE_AC | TYPE_GOLD | TYPE_VOID) ID LPAREN paramList? RPAREN block
    ;

paramList : paramDecl (COMMA paramDecl)* ;
paramDecl : (TYPE_INT | TYPE_FLOAT | TYPE_BOOL | TYPE_STRING | TYPE_HP | TYPE_AC | TYPE_GOLD) ID ; 

block
    : LBRACE statement* RBRACE
    ;

statement
    : decl
    | assign
    | exprStmt
    | ifStmt
    | whileStmt
    | switchStmt
    | printStmt
    | returnStmt
    | block
    ;

exprStmt : expr SEMI ;
decl   : (TYPE_INT | TYPE_FLOAT | TYPE_BOOL | TYPE_STRING | TYPE_HP | TYPE_AC | TYPE_GOLD) ID ASSIGN expr SEMI ;
assign : ID (ASSIGN | PLUS_ASSIGN  | MINUS_ASSIGN |  STAR_ASSIGN | SLASH_ASSIGN) expr SEMI ;

ifStmt    : IF LPAREN expr RPAREN block (ELSE block)? ;
whileStmt : WHILE LPAREN expr RPAREN block ;

switchStmt   : SWITCH LPAREN expr RPAREN LBRACE caseBlock+ defaultBlock? RBRACE ;
caseBlock    : CASE expr COLON block ;
defaultBlock : DEFAULT COLON block ;

printStmt   : PRINT COLON (expr | ISTRING) SEMI ;
returnStmt  : RETURN expr? SEMI;

diceOnly   : D20 | D12 | D10 | D8 | D6 | D4 | D3;

expr
    : ID LPAREN (expr (COMMA expr)*)? RPAREN           # FunctionCallExpr
    | LPAREN expr RPAREN                               # ParenExpr
    | (ADV | DIS) diceOnly                             # DiceAdvDisExpr
    | ID (PLUS_PLUS | MINUS_MINUS)                     # PostIncExpr
    | (PLUS_PLUS | MINUS_MINUS) ID                     # PreIncExpr
    | (NOT | MINUS) expr                               # UnaryExpr
    | expr (STAR | SLASH | PERCENT) expr               # MulDivExpr
    | expr (PLUS | MINUS) expr                         # AddSubExpr
    | expr (LT | GT | LE | GE) expr                    # RelationalExpr
    | expr (SAVE | VS) expr                            # SaveVsExpr
    | expr (EQ | NEQ) expr                             # EqualityExpr
    | expr AND expr                                    # AndExpr
    | expr OR expr                                     # OrExpr
    | expr QUESTION expr COLON expr                    # TernaryExpr
    | D20                                              # D20Expr
    | D12                                              # D12Expr
    | D10                                              # D10Expr
    | D8                                               # D8Expr
    | D6                                               # D6Expr
    | D4                                               # D4Expr
    | D3                                               # D3Expr
    | INT                                              # IntExpr
    | FLOAT                                            # FloatExpr
    | BOOL                                             # BoolExpr
    | STRING                                           # StringExpr
    | ISTRING                                          # IStringExpr
    | ID                                               # IdExpr
    ;
