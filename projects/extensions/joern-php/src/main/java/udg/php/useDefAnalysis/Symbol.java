package udg.php.useDefAnalysis;

import java.util.LinkedList;

public class Symbol {
    public String name; // the symbol's name
    public boolean isArg; // is the symbol an arg?
    public Long argId; // The id of the arg

    public boolean isArray;
    public boolean isIndexVar; // is the index statically unknown?
    public String index;

    public boolean isField;

    public Symbol(String name) {
        this.name = name;
    }

}
