Ecco la documentazione `doc.md` generata per il tuo linguaggio `DnDLang`, strutturata seguendo il modello che hai fornito.

---

# DnDLang

Un Domain Specific Language (DSL) per la descrizione, simulazione e risoluzione di incontri e sessioni di combattimento in stile Dungeons & Dragons.

## Indice

1. [Introduzione](https://www.google.com/search?q=%231-introduzione)
2. [Guida Rapida](https://www.google.com/search?q=%232-guida-rapida)
3. [Sintassi](https://www.google.com/search?q=%233-sintassi)
4. [Semantica](https://www.google.com/search?q=%234-semantica)
5. [Implementazione](https://www.google.com/search?q=%235-implementazione)
6. [Programmi di Test](https://www.google.com/search?q=%236-programmi-di-test)

---

## 1. Introduzione

**DnDLang** è un linguaggio di programmazione imperativo sviluppato come DSL per la gestione e la simulazione di schede personaggio e combattimenti (encounter) nei giochi di ruolo cartacei.

### Caratteristiche principali

* **Tipizzazione statica esplicita** con tipi di dominio dedicati all'universo di D&D (`HP`, `AC`, `Gold`).
* **Struttura a blocchi semantici**: sezioni distinte per definire le entità (`hero`, `foe`, `inventory`) e una per il flusso di esecuzione principale (`quest`).
* **Costrutti iterativi e condizionali**: `while`, `if`-`else` e `switch`-`case`-`default`.
* **Meccaniche RPG native**: keyword `random` integrata per simulare il lancio di un dado a 20 facce (d20).
* **Zucchero sintattico**: assegnamenti composti (`+=`, `-=`, `*=`, `/=`), incremento/decremento (`++`, `--`), operatore ternario (`_ ? _ : _`) e stringhe interpolate (`i"...${expr}..."`).
* **Gestione tollerante degli errori (Resilience)**: in caso di errori a runtime (come divisioni per zero o variabili non trovate nell'interpolazione), l'interprete segnala il problema fornendo valori di default senza far crashare la simulazione.

### Contesto applicativo

DnDLang è pensato per Game Master o giocatori che vogliono automatizzare la risoluzione di scontri complessi, calcolare statistiche di combattimento, o testare l'efficacia (playtest) di mostri ed eroi prima di una sessione effettiva.

---

## 2. Guida Rapida

### Requisiti

* **Java**: Necessario per compilare ed eseguire il parser e l'interprete.
* **ANTLR 4**: Per la generazione del lexer e del parser a partire dalla grammatica `DnDLang.g4`.
* **Maven**: Consigliato per la gestione delle dipendenze e l'esecuzione.

### Eseguire un programma

L'interprete può essere lanciato tramite Maven, specificando il file sorgente `.dnd` come argomento:

```bash
mvn exec:java -Dexec.mainClass="it.univr.dndlang.Main" -Dexec.args="programs/nome_file.dnd"

```

### Hello World

Un semplice programma in DnDLang che istanzia un eroe e ne stampa le caratteristiche.

```dnd
hero: {
    String name = "Aelar";
    String class= "Monk";
    HP hp       = 24;
    AC ac       = 15;
}
inventory: { Gold gold = 50.0; }
quest: {
    print: i"Eroe: ${hero.name} | Class: ${hero.class} | HP: ${hero.hp} | AC: ${hero.ac} | Gold: ${hero.gold}";
}

```

> **Output** 
> 
> 
> 
> 
> Eroe: Aelar | Class: Monk | HP: 24 HP | AC: 15 AC | Gold: 50.0 gp

---

## 3. Sintassi

### 3.1 Struttura di un programma

Un programma DnDLang è diviso in quattro sezioni opzionali, ma solitamente si chiude sempre con la sezione `quest`:

```antlr
program : heroSection? inventorySection? foeSection? questSection EOF ;

```

* **`heroSection`** (`hero: { ... }`): Definisce le statistiche e l'equipaggiamento dell'eroe. Le variabili dichiarate qui acquisiscono automaticamente il prefisso `hero.`.
* **`foeSection`** (`foe: { ... }`): Definisce le statistiche del mostro o nemico. Le variabili acquisiscono il prefisso `foe.`.
* **`inventorySection`** (`inventory: { ... }`): Definisce le risorse o l'oro.
* **`questSection`** (`quest: { ... }`): Il blocco logico principale dove avvengono le valutazioni, gli attacchi e i calcoli.

### 3.2 Tipi di dato

| Tipo | Descrizione | Esempio |
| --- | --- | --- |
| `Int` | Numero intero | `10`, `-3` |
| `Float` | Numero con virgola mobile | `3.14` |
| `Bool` | Valore logico | `true`, `false` |
| `String` | Stringa di testo | `"Spada"` |
| `HP` | Punti Ferita (Health Points, compatibile con numerici) | `42` |
| `AC` | Classe Armatura (Armor Class, compatibile con numerici) | `18` |
| `Gold` | Monete d'oro (compatibile con virgola mobile) | `340.5` |

I tipi di dominio `HP`, `AC` e `Gold` aggiungono valore semantico e vengono visualizzati con suffissi specifici (es. `HP`, `AC`, `gp`) quando stampati tramite interpolazione.

### 3.3 Espressioni e Operatori

DnDLang supporta i classici operatori aritmetico-logici e relazionali:

* Aritmetici: `+`, `-`, `*`, `/`, `%`
* Incremento: `++`, `--` (pre e post)
* Assegnamento: `=`, `+=`, `-=`, `*=`, `/=`
* Relazionali: `==`, `!=`, `<`, `>`, `<=`, `>=`
* Logici: `&&`, `||`, `!`
* Ternario: `condizione ? vero : falso`

Inoltre, è presente l'operatore speciale **`random`**, che restituisce un intero generato casualmente tra $1$ e $20$, simulando un tiro di d20.

### 3.4 Costrutti di controllo

**If-Else:**

```dnd
if (foeAttack >= hero.ac) {
    hero.hp -= foe.spikeDamage;
} else {
    print: "PARATO!";
}

```

**Ciclo While:**

```dnd
while (hero.hp > 0 && foe.hp > 0) {
    round++;
}

```

**Switch-Case:**

```dnd
switch (roll) {
    case 1: { print: "Fallimento Critico!"; }
    case 20: { print: "Successo Critico!"; }
    default: { print: "Attacco normale"; }
}

```

### 3.5 Stampa e interpolazione

```dnd
print: "Testo semplice";
print: i"Danni inflitti: ${hero.strMod + 5}";

```

Il costrutto `print:` accetta una stringa (semplice o interpolata `i"..."`) o una singola espressione, stampandone il risultato. Le espressioni racchiuse in `${}` all'interno di stringhe interpolate vengono valutate a runtime.

---

## 4. Semantica

### 4.1 Ambiente e Scoping (Namespace)

L'ambiente di esecuzione (`Enviroment`) è strutturato a stack di memorie per supportare lo **scoping lessicale** all'interno dei blocchi `{ ... }`.
Una particolarità fondamentale di DnDLang è l'autogenerazione dei namespace:

* Ogni variabile dichiarata dentro `hero: { ... }` viene salvata nell'ambiente con il nome reale `hero.nomeVariabile`.
* Ogni variabile in `foe: { ... }` viene salvata come `foe.nomeVariabile`.
Questo permette di usare nomi puliti e generici nelle dichiarazioni (es. `String name = "Drago"`) e di accedervi in modo qualificato (es. `foe.name`) nella `quest`.

### 4.2 Coercizione di Tipo

I tipi numerici di dominio subiscono troncamento o conversione automatica:

* Se a un tipo `HP`, `AC` o `Int` viene assegnato un valore calcolato come `Double`, esso viene troncato esplicitamente a intero.
* Se al tipo `Gold` viene assegnato un `Integer`, viene promosso a `Double`.

### 4.3 Tolleranza agli Errori

A differenza dei linguaggi tradizionali che interrompono l'esecuzione al primo errore non gestito, DnDLang favorisce il proseguimento della sessione (resilience):

* **Variabile non dichiarata in assegnamento:** L'operazione viene saltata e l'errore stampato su `stderr`.
* **Divisione per zero:** Viene intercettata, viene assegnato il valore di default per quel tipo (`0` per gli interi) e lo script continua.

### 4.4 Semantica Operazionale

L'operatore nativo `random` implementa un comportamento non deterministico essenziale per i GDR. Dato lo stato $(\overline{\sigma}, c)$, la sua valutazione produce:

$$    \text{Random} ~ \frac{
        -
    }{
        (\overline{\sigma},\ \mathtt{random}) \rightarrow (\overline{\sigma},\ n)
    }
    ~ n \in \mathbb{N}, \ 1 \le n \le 20$$

Per un assegnamento all'interno di una sezione speciale (es. `heroSection`), l'ambiente $\sigma$ applica un prefisso $p = \text{"hero."}$ all'identificatore $id$:

$$    \text{DeclPrefix} ~ \frac{
        (\overline{\sigma},\ e) \rightarrow (\overline{\sigma},\ v)
    }{
        (\overline{\sigma},\ \mathtt{Type}\ id \mathtt{=} \ e) \rightarrow (\sigma[p \cdot id \mapsto v] \cdot \overline{\sigma}',\ \epsilon)
    }$$

---

## 5. Implementazione

### 5.1 Struttura del Progetto

Il DSL è implementato in Java utilizzando **ANTLR 4** per la fase di analisi lessicale e sintattica.

* `DnDLang.g4`: Grammatica sorgente.
* `Main.java`: Entry point che legge il file `.dnd`, genera l'AST ed invoca l'interprete.
* `DnDInterpreter.java`: Classe centrale che estende `DnDLangBaseVisitor`, implementando la logica di valutazione dei nodi dell'AST.
* `Enviroment.java`: Struttura dati per la gestione dello scoping e dello stack dei blocchi.
* `DnDLangError.java`: Eccezione runtime custom che mappa gli errori alla riga del file sorgente.

### 5.2 Compiti dell'Interprete e Zucchero Sintattico

L'interprete `DnDInterpreter` risolve on-the-fly lo zucchero sintattico (come `+=` o `++`) traducendolo nelle rispettive operazioni aritmetiche binarie e aggiornando il record dell'ambiente tramite il metodo `env.assign()`.

### 5.3 Sfide Tecniche: Interpolazione di Stringhe

Una delle feature tecnicamente più complesse è l'**interpolazione delle stringhe** (`i"...${expr}..."`). Per consentire a espressioni complesse (es. `round - 1` o chiamate a variabili namespace) di essere valutate correttamente *all'interno* del testo, l'interprete:

1. Usa una Regex per estrarre la substringa `exprText` contenuta in `${...}`.
2. Istanzia a runtime un nuovo **sotto-parser ANTLR** (`DnDLangLexer` e `DnDLangParser`) passando l'espressione estratta.
3. Valuta il frammento di AST risultante richiamando `visit(exprCtx)`.
4. Applica suffissi automatici (es. " HP" o " gp") se riconosce che l'espressione è una variabile di dominio dichiarata nell'ambiente.

---

## 6. Programmi di Test

### `errors.dnd` – Tolleranza e continuità d'esecuzione

Dimostra la gestione non bloccante degli errori a runtime.

```dnd
hero: { HP hp = 20; }
quest: {
    print: "Test 1: Divisione per zero";
    Int x = 10;
    Int y = 0;
    Int z = x / y;
    print: i"Valore di z dopo div/0: ${z}";

    print: "Test 3: Uso di variabile non dichiarata nell'interpolazione";
    print: i"Tentativo di stampare ${variabile_fantasma}";

    print: "🚀 Fine del test degli errori. Il programma è continuato con successo senza crashare!";
}

```

*(Durante l'esecuzione, il motore Java stamperà gli errori sullo Standard Error, mentre il programma continuerà allocando valori di fallback sullo Standard Output)*.

### `quest.dnd` – Combattimento simulato e Namespace

Esempio completo di incontro tra un Paladino e un Drago Rosso.

```dnd
hero: {
    String name = "Garrick";
    String class = "Paladin";
    HP hp = 30;
    AC ac = 15;
    String rightHandWeapon = "Spada dritta";
    Int strBonus = 3;
}
foe: {
    String name = "Drago Rosso";
    HP hp = 40;
    AC ac = 14;
}
quest: {
    Int round = 1;
    print: i"Inizia la quest contro il ${foe.name}!";
    
    while (hero.hp > 0 && foe.hp > 0) {
        print: i"--- Round ${round} ---";
        Int roll = random;
        
        switch (roll) {
            case 1: { hero.hp -= 2; }
            case 20: {
                print: i"Successo Critico! la tua ${hero.rightHandWeapon} trafigge il ${foe.name}";
                foe.hp -= 10;
            }
            default: {
                if (roll + hero.strBonus >= foe.ac) {
                    foe.hp -= 5;
                }
            }
        }
        
        if (foe.hp > 0) {
            Int foe_roll = random;
            if (foe_roll >= hero.ac) { hero.hp -= 4; }
        }
        round++;
    }
    
    String esito = (hero.hp > 0) ? "VITTORIA" : "SCONFITTA";
    print: i"Esito finale della Quest: ${esito} al round ${round - 1}";
}

```