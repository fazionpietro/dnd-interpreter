grammar DnDLang;
options { language = Java; }


HERO      : 'hero'      ;
INVENTORY : 'inventory' ;
FOE       : 'foe'       ;
QUEST     : 'quest'     ;
IF        : 'if'        ;
ELSE      : 'else'      ;
WHILE     : 'while'     ;
SWITCH    : 'switch'    ;
CASE      : 'case'      ;
DEFAULT   : 'default'   ;
PRINT     : 'print'     ;
RANDOM    : 'random'    ;

TYPE_INT    : 'Int'    ;
TYPE_FLOAT  : 'Float'  ;
TYPE_BOOL   : 'Bool'   ;
TYPE_STRING : 'String' ;
TYPE_HP     : 'HP'     ;
TYPE_AC     : 'AC'     ;
TYPE_GOLD   : 'Gold'   ;


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
    : heroSection? inventorySection? foeSection? questSection EOF
    ;

heroSection      : HERO COLON block ;
inventorySection : INVENTORY COLON block ;
foeSection       : FOE COLON block ;
questSection     : QUEST COLON block ;

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

printStmt : PRINT COLON (expr | ISTRING) SEMI ;


expr
    : LPAREN expr RPAREN                               # ParenExpr
    | ID (PLUS_PLUS | MINUS_MINUS)                     # PostIncExpr
    | (PLUS_PLUS | MINUS_MINUS) ID                     # PreIncExpr
    | (NOT | MINUS) expr                               # UnaryExpr
    | expr (STAR | SLASH | PERCENT) expr               # MulDivExpr
    | expr (PLUS | MINUS) expr                         # AddSubExpr
    | expr (LT | GT | LE | GE) expr                    # RelationalExpr
    | expr (EQ | NEQ) expr                             # EqualityExpr
    | expr AND expr                                    # AndExpr
    | expr OR expr                                     # OrExpr
    | expr QUESTION expr COLON expr                    # TernaryExpr
    | RANDOM                                           # RandomExpr
    | INT                                              # IntExpr
    | FLOAT                                            # FloatExpr
    | BOOL                                             # BoolExpr
    | STRING                                           # StringExpr
    | ISTRING                                          # IStringExpr
    | ID                                               # IdExpr
    ;
