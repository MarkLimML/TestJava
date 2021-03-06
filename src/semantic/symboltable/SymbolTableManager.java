package semantic.symboltable;

import semantic.symboltable.scope.ClassScope;

import java.util.HashMap;

/**
 * Holds all of the found variables in code and stores them here in the symbol table manager.
 *
 */
public class SymbolTableManager {
	
	private static SymbolTableManager symbolTableManager = null;
	
	public static SymbolTableManager getInstance() {
		return symbolTableManager;
	}
	
	private HashMap<String, ClassScope> classTable;
	
	private SymbolTableManager() {
		this.classTable = new HashMap<>();
	}
	
	public static void initialize() {
		symbolTableManager = new SymbolTableManager();
	}
	
	public static void reset() {
		symbolTableManager.classTable.clear();
	}
	
	public void addClassScope(String className, ClassScope classScope) {
		this.classTable.put(className, classScope);
	}
	
	public ClassScope getClassScope(String className) {
		if(containsClassScope(className)) {
			return this.classTable.get(className);
		}
		else {
			System.err.println("SymbolTableManager: " + className + " is not found!");
			return null;
		}
	}
	
	public boolean containsClassScope(String className) {
		return this.classTable.containsKey(className);
	}
	
	public void resetClassTables() {
		ClassScope[] classScopes = this.classTable.values().toArray(new ClassScope[this.classTable.size()]);

		for (ClassScope classScope : classScopes) {
			classScope.resetValues();
		}
	}
}
