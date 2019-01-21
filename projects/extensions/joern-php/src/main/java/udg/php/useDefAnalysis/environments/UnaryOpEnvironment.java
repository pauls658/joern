package udg.php.useDefAnalysis.environments;

import udg.ASTProvider;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.php.useDefAnalysis.Symbol;

import java.util.LinkedList;

public class UnaryOpEnvironment extends EmitUseEnvironment {
    private PHPASTDefUseAnalyzer phpAnalyzer;

    public void addChildSymbols(LinkedList<Symbol> childSymbols, ASTProvider child) {
        if (phpAnalyzer.analyzingArgList()) {
            symbols.addAll(childSymbols);
        } else {
            useSymbols.addAll(childSymbols);
        }
    }

    @Override
	public void preTraverse(PHPASTDefUseAnalyzer analyzer) {
		this.phpAnalyzer = analyzer;
	}
}
