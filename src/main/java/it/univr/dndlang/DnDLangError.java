package it.univr.dndlang;

/** Eccezione runtime del linguaggio, riporta il numero di riga dell'errore. */
public class DnDLangError extends RuntimeException{
    private final int line;
    /** Crea un errore runtime con messaggio e numero di riga nel sorgente. */
    public DnDLangError(String message, int line){
        super(message);
        this.line = line;
    }

    /** Restituisce il numero di riga in cui si è verificato l'errore. */
    public int getLine() {
        return line;
    }
}
