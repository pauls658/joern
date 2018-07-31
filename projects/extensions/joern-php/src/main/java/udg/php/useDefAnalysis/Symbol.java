package udg.php.useDefAnalysis;

import java.util.LinkedList;

public class Symbol {
    public String name; // the symbol's name
    public String origName; // The symbol's unprefixed original name
    public boolean isArg; // is the symbol an arg?
    public Long argId; // The id of the arg

    public boolean isArray;
    public boolean isIndexVar; // is the index statically unknown?
    public String origIndex;
    public String index;

    public void setIndex(String i) {
        this.index = i;
        this.origIndex = i;
    }

    public boolean isField;

    public Symbol(String name) {
        this.origName = name;
        this.name = name;
    }

}
