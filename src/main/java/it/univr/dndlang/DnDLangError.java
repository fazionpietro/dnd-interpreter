package it.univr.dndlang;

public class DnDLangError extends RuntimeException{
    private final int line;
    public DnDLangError(String message, int line){
        super(message);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
