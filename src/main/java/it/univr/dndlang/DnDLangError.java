package it.univr.dndlang;

/*
 * DESIGN
 * ------
 *
 * Simple wrapper for runtime errors in the interpreted language. It carries 
 * the line number where the error occurred in the source code to provide 
 * meaningful error messages to the user.
 */

/** 
 * Language runtime exception, holding the source code line number. 
 */
public class DnDLangError extends RuntimeException{
    private final int line;
    
    /** 
     * Creates a runtime error with a message and the source code line number. 
     */
    public DnDLangError(String message, int line){
        super(message);
        this.line = line;
    }

    /** 
     * Retrieves the line number where the error occurred. 
     */
    public int getLine() {
        return line;
    }
}
