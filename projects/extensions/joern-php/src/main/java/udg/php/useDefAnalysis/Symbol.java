package udg.php.useDefAnalysis;

public class Symbol {
    // Remember, everytime you add a new property, you need to update the equals
    // and copy function too.

    public static int ARRAY_CONST_INDEX = 0;
    public static int ARRAY_VAR_INDEX = 1;
    public static int ARRAY_UNKNOWN_INDEX = 2;

    public static String indexPrefix = "cid";
    public static String unknownIndex = indexPrefix + "_unknown";

    public String name; // the symbol's name
    public String origName; // The symbol's unprefixed original name
    public boolean isArg = false; // is the symbol an arg?
    public Long argId = null; // The id of the arg

    public boolean isArray = false;
    public int arrayType = -1;
    public boolean isIndexVar = false; // is the index statically unknown?
    public String origIndex = null;
    public String index = null;

    public boolean star = false; // Is an assignment to this symbol and assignment to every array/field
                         // of the symbol? I.e. should we not kill previous definitions of this
                         // symbol?

    public String getSymbolEncoding() {
        String enc = name;
        if (isArray) {
            enc += "[";
            if (arrayType == ARRAY_CONST_INDEX) {
                // Don't add implicit unknown uses yet. When doing the array-type analysis
                // we want to know about explicit uses only
                enc += index ;//+ ";" + name + "[" + unknownIndex;
            } else if (arrayType == ARRAY_VAR_INDEX) {
                enc += "$" + index;
            } else {
                enc += unknownIndex;
            }
        }
        if (star) {
            enc += "*";
        }
        return enc;
    }

    public void setIndex(String i) {
        this.index = i;
        this.origIndex = i;
    }

    public boolean isField;

    public Symbol(String name) {
        this.origName = name;
        this.name = name;
    }

    public Symbol copy() {
        Symbol ret = new Symbol(this.name);
        ret.isArg = this.isArg;
        ret.argId = this.argId;
        ret.isArray = this.isArray;
        ret.isIndexVar = this.isIndexVar;
        ret.origIndex = this.origIndex;
        ret.index = this.index;
        ret.star = this.star;
        ret.arrayType = this.arrayType;
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        Symbol other = (Symbol)o;
        return other.name == this.name &&
                other.origName == this.origName &&
                other.isArg == this.isArg &&
                other.argId == this.argId &&
                other.isArray == this.isArray &&
                other.isIndexVar == this.isIndexVar &&
                other.origIndex == this.origIndex &&
                other.index == this.index &&
                other.arrayType == this.arrayType &&
                other.star == this.star;
    }
}
