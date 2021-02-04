package semantic.analyzers;

import antlr.Java8Parser.*;
import error.checkers.MultipleFuncDecChecker;
import Execution.ExecutionManager;
import semantic.representation.JavaMethod;
import semantic.representation.JavaMethod.FunctionType;
import semantic.symboltable.scope.ClassScope;
import semantic.symboltable.scope.LocalScopeCreator;
import semantic.utils.IdentifiedTokens;
import semantic.utils.RecognizedKeywords;
import semantic.utils.StringUtils;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Analyzes method declarations and properly stores them in the symbol table
 *
 */
public class MethodAnalyzer implements ParseTreeListener {
	
	private ClassScope declaredClassScope;
	private IdentifiedTokens identifiedTokens;
	private JavaMethod declaredJavaMethod;
	private boolean paramsFlag = false;
	
	public MethodAnalyzer(IdentifiedTokens identifiedTokens, ClassScope declaredClassScope) {
		this.identifiedTokens = identifiedTokens;
		this.declaredClassScope = declaredClassScope;
		this.declaredJavaMethod = new JavaMethod();
	}
	
	public void analyze(MethodDeclarationContext ctx) {
		ExecutionManager.getExecutionManager().openFunctionExecution(this.declaredJavaMethod);
		
		ParseTreeWalker treeWalker = new ParseTreeWalker();
		treeWalker.walk(this, ctx);
	}
	
	@Override
	public void visitTerminal(TerminalNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		if(ctx instanceof MethodDeclarationContext) {
			MethodDeclarationContext methodDecCtx = (MethodDeclarationContext) ctx;
			MultipleFuncDecChecker funcDecChecker = new MultipleFuncDecChecker(methodDecCtx);
			funcDecChecker.verify();
			
			this.analyzeIdentifier(methodDecCtx.methodHeader().methodDeclarator().Identifier()); //get the function identifier
		}
		else {
			this.analyzeMethod(ctx);
		}
		
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		if(ctx instanceof MethodDeclarationContext) {
			MethodDeclarationContext mdCtx = (MethodDeclarationContext) ctx;

			if (!this.declaredJavaMethod.hasValidReturns()) {

				int lineNumber = 0;

				if (mdCtx.methodHeader().methodDeclarator().Identifier() != null)
					lineNumber = mdCtx.methodHeader().methodDeclarator().Identifier().getSymbol().getLine();

				ExecutionManager.getExecutionManager().consoleListModel.addElement(StringUtils.formatError("No return statement in function "+this.declaredJavaMethod.getFunctionName()+" at line "+ lineNumber));
			}
			ExecutionManager.getExecutionManager().closeFunctionExecution();
		}
	}
	
	private void analyzeMethod(ParserRuleContext ctx) {

		if(ctx instanceof UnannTypeContext && !this.paramsFlag) {
			UnannTypeContext typeCtx = (UnannTypeContext) ctx;
			
			//return type is a primitive type
			if(typeCtx.unannPrimitiveType() != null) {
				UnannPrimitiveTypeContext primitiveTypeCtx = typeCtx.unannPrimitiveType();
				this.declaredJavaMethod.setReturnType(JavaMethod.identifyFunctionType(primitiveTypeCtx.getText()));
			}
			//return type is a string or a class type
			else {
				this.analyzeClassOrInterfaceType(typeCtx.unannReferenceType().unannClassOrInterfaceType());
			}
		}
		
		else if(ctx instanceof MethodDeclaratorContext) {
			MethodDeclaratorContext exprCtx = (MethodDeclaratorContext) ctx;
			if(exprCtx.formalParameterList() != null) {
				this.paramsFlag = true;
				FormalParameterListContext formalParamsCtx = exprCtx.formalParameterList();
				this.analyzeParameters(formalParamsCtx);
			}
			this.storeJavaMethod();
		}
		
		else if(ctx instanceof MethodBodyContext) {
			BlockContext blockCtx = ((MethodBodyContext) ctx).block();
			
			BlockAnalyzer blockAnalyzer = new BlockAnalyzer();
			this.declaredJavaMethod.setParentLocalScope(LocalScopeCreator.getInstance().getActiveLocalScope());
			blockAnalyzer.analyze(blockCtx.blockStatements());
			
		}
	}
	
	private void analyzeClassOrInterfaceType(UnannClassOrInterfaceTypeContext classOrInterfaceCtx) {
		//a string identified
		if(classOrInterfaceCtx.getText().contains(RecognizedKeywords.PRIMITIVE_TYPE_STRING)) {
			this.declaredJavaMethod.setReturnType(FunctionType.STRING_TYPE);
		}
		//a class identified
		else {
			ExecutionManager.getExecutionManager().consoleListModel.addElement(StringUtils.formatDebug("Class identified: " + classOrInterfaceCtx.getText()));
		}
	}
	
	private void analyzeIdentifier(TerminalNode identifier) {
		this.declaredJavaMethod.setFunctionName(identifier.getText());
	}
	
	private void analyzeParameters(FormalParameterListContext formalParamsCtx) {
		if(formalParamsCtx != null) {
			ParameterAnalyzer parameterAnalyzer = new ParameterAnalyzer(this.declaredJavaMethod);
			parameterAnalyzer.analyze(formalParamsCtx);
		}
	}
	
	/*
	 * Stores the created function in its corresponding class scope
	 */
	private void storeJavaMethod() {
		//System.out.println("Method stored");
		this.declaredClassScope.addPrivateJavaMethod(this.declaredJavaMethod.getFunctionName(), this.declaredJavaMethod);
	}
}
