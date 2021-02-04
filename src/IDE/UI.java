package IDE;

import antlr.Java8Lexer;
import antlr.Java8Parser;
import antlr.CustomListener;
import error.ErrorListener;
import error.CustomErrorStrategy;
import Execution.ExecutionManager;
import Execution.FunctionTracker;
import semantic.statements.StatementControlOverseer;
import semantic.symboltable.SymbolTableManager;
import semantic.symboltable.scope.LocalScopeCreator;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import uicomp.SquigglePainter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by jasonsapdos on 06/10/2017.
 */
public class UI {

    // UI ELEMENTS
    private JPanel mainPane;
    private JScrollPane consolePane;
    private JList consoleList;
    private JButton btnRun;
    private JButton btnClear;
    private JPanel actionPane;
    private JPanel codeCasePane;
    private JTabbedPane consoleTabPane;
    private JPanel allTab;
    private JPanel verboseTab;
    private JPanel errorTab;
    private JPanel debugTab;
    private DefaultListModel consoleListModel;
    private RSyntaxTextArea txtArCode;
    private JTextField searchField;
    private JCheckBox regexCB;
    private JCheckBox matchCaseCB;

    // INPUT ELEMENTS
    private String code;
    private int lineNum;
    private ArrayList<Integer> errorPositionList;
    private int selectedTabIndex = 0;

    public UI() {

        txtArCode = new RSyntaxTextArea(10, 60);
        txtArCode.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        txtArCode.setCodeFoldingEnabled(true);
        RTextScrollPane textScrollPane = new RTextScrollPane(txtArCode);
        codeCasePane.add(textScrollPane);

        //CompletionProvider provider = createCompletionProvider();
        //AutoCompletion ac = new AutoCompletion(provider);
        //ac.install(txtArCode);

        changeStyle();
        addListeners();
        initializeInterpreter();

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MyJAVA Interpreter");
        frame.setContentPane(new UI().mainPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void addListeners(){
        btnRun.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetComponents();
                System.out.println("Run button clicked!");
                code = "public class Main {" + txtArCode.getText() + "}";
                removeHighlights(txtArCode);

                Java8Lexer lex = new Java8Lexer(new ANTLRInputStream(code));
                CommonTokenStream tokens = new CommonTokenStream(lex);
                Java8Parser parser = new Java8Parser(tokens);
                ErrorListener errorListener = new ErrorListener();
                parser.setErrorHandler(new CustomErrorStrategy());
                parser.removeErrorListeners();
                parser.addErrorListener(errorListener);
                parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

                ParserRuleContext parserRuleContext = parser.compilationUnit();
                ParseTreeWalker treeWalker = new ParseTreeWalker();
                // CustomListener is like the Implementor
                // When it walks to a class declaration context, trigger si class analyzer
                // When it walks to a main declaration context, triggered si main analyzer
                treeWalker.walk(new CustomListener(), parserRuleContext);

                System.out.println(parserRuleContext.toStringTree(parser));

                // After semantic checking
                if(!ExecutionManager.hasErrors)
                    ExecutionManager.getExecutionManager().executeAllActions();

                consoleListModel = new DefaultListModel();
                consoleList.setSelectedIndex(0);
                consoleListModel = ExecutionManager.getExecutionManager().consoleListModel;
                // consoleListModel = errorListener.getConsoleListModel();
                errorPositionList = errorListener.getErrorPositionList();
                underlineErrors(errorPositionList);

                if(consoleListModel.getSize() == 0){
                    consoleListModel.addElement("[DEBUG] No syntax errors were detected.");
                }

                ExecutionManager.getExecutionManager().consoleListModel = consoleListModel;
                switch(consoleTabPane.getSelectedIndex()){
                    case 0: consoleList.setModel(generateDListModel(consoleListModel, 'A')); break;
                    case 1: consoleList.setModel(generateDListModel(consoleListModel, 'P')); break;
                    case 2: consoleList.setModel(generateDListModel(consoleListModel, 'E')); break;
                    case 3: consoleList.setModel(generateDListModel(consoleListModel, 'D')); break;
                }

                consolePane.setViewportView(consoleList);
                consoleList.setLayoutOrientation(JList.VERTICAL);
            }
        });
        consoleList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {

                    if(consoleList.getSelectedIndex() != -1) {
                        System.out.println(consoleList.getSelectedIndex());
                        String selectedMessage = consoleList.getSelectedValue().toString();

                        // Checks if message has an error keyword and contains a line number
                        if (selectedMessage.charAt(1) == 'E' && selectedMessage.contains("Line")) {
                            // Get the line number of the selected message!
                            lineNum = extractLineNumber(selectedMessage);
                            // Moves the highlight text to the line + 1
                            txtArCode.setCaretPosition(txtArCode.getDocument()
                                    .getDefaultRootElement().getElement(lineNum - 1)
                                    .getStartOffset());
                        }
                    }
                }
            }
        });
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                code = "";
                txtArCode.setText("");
                consolePane.getViewport().removeAll();
                consolePane.revalidate();
                consolePane.repaint();
            }
        });
        consoleTabPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(consoleListModel != null) {
                    System.out.println("Current tab: " + consoleTabPane.getSelectedIndex());
                    JScrollPane newPane = consolePane;
                    JList newList = (JList) newPane.getViewport().getView();

                    // Clear the consolePane of the last selected tab
                    ((JPanel) consoleTabPane.getComponentAt(selectedTabIndex)).removeAll();

                    switch (consoleTabPane.getSelectedIndex()) {
                        case 0: { // ALL
                            consoleList.setModel(consoleListModel);
                            allTab.add(consolePane);
                        }
                        break;
                        case 1: { // Program output
                            consoleList.setModel(generateDListModel(consoleListModel, 'P'));
                            verboseTab.add(newPane);
                        }
                        break;
                        case 2: { // Error
                            consoleList.setModel(generateDListModel(consoleListModel, 'E'));
                            errorTab.add(newPane);
                        }
                        break;
                        case 3: { // Debug
                            consoleList.setModel(generateDListModel(consoleListModel, 'D'));
                            debugTab.add(newPane);
                        }
                        break;
                    }
                    selectedTabIndex = consoleTabPane.getSelectedIndex();
                }
            }
        });
    }

    private DefaultListModel generateDListModel(DefaultListModel defaultListModel, char messageType){

        // No need to be filtered, all messages needed.
        if(messageType == 'A')
            return defaultListModel;

        DefaultListModel newModel = new DefaultListModel();

        for(int i = 0; i < defaultListModel.getSize(); i++){
            // if message does not contain the required type
            if(defaultListModel.get(i).toString().charAt(1) == messageType)
                newModel.addElement(defaultListModel.get(i));
        }

        System.out.println("newModel size: " + newModel.getSize());

        if(newModel.getSize() == 0)
            newModel.addElement("No messages of this type.");

        return newModel;
    }

    // Gets the line number from a message!
    private int extractLineNumber(String s){
        // [ERROR] Line 1:2
        String colonSplit = s.split("\\:")[0];
        // [ERROR] Line 1
        String smallESplit = colonSplit.substring(13, colonSplit.length());
        return Integer.parseInt(smallESplit);
    }

    private void underlineErrors(ArrayList<Integer> errorPositionList){
        SquigglePainter red = new SquigglePainter( Color.RED );

        for(int i = 0; i < errorPositionList.size(); i+=3){

            try {
                txtArCode.getHighlighter().addHighlight(errorPositionList.get(i+1),
                                                    errorPositionList.get(i+2)+1,
                                                        red);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static void removeHighlights(JTextComponent textComp) {
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();

        for (Highlighter.Highlight hilite1 : hilites) {
            if (hilite1.getPainter() instanceof SquigglePainter) {
                hilite.removeHighlight(hilite1);
            }
        }
    }


    private void initializeInterpreter() {
        // Initialize the interpreter stuff
        SymbolTableManager.initialize();
        ExecutionManager.initialize();
        LocalScopeCreator.initialize();
        StatementControlOverseer.initialize();
        FunctionTracker.initialize();

        // Execution manager takes charge of thread
    }

    public static void resetComponents() {
        SymbolTableManager.reset();
        ExecutionManager.reset();
        LocalScopeCreator.reset();
        StatementControlOverseer.reset();
        FunctionTracker.reset();
    }

    private CompletionProvider createCompletionProvider() {

        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        // lower case
        provider.addCompletion(new BasicCompletion(provider, "break"));
        provider.addCompletion(new BasicCompletion(provider, "case"));
        // ... etc ...
        provider.addCompletion(new BasicCompletion(provider, "try"));
        provider.addCompletion(new BasicCompletion(provider, "void"));
        provider.addCompletion(new BasicCompletion(provider, "while"));

        // UPPER CASE
        provider.addCompletion(new BasicCompletion(provider, "BREAK"));
        provider.addCompletion(new BasicCompletion(provider, "CASE"));
        // ... etc ...
        provider.addCompletion(new BasicCompletion(provider, "TRY"));
        provider.addCompletion(new BasicCompletion(provider, "VOID"));
        provider.addCompletion(new BasicCompletion(provider, "WHILE"));

        provider.addCompletion(new ShorthandCompletion(provider, "print",
                    "print(", "print("));
        provider.addCompletion(new ShorthandCompletion(provider, "scan",
                    "scan(", "scan("));

        return provider;

    }

    private void changeStyle() {
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "uicomp/eclipse.xml"));
            theme.apply(txtArCode);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }
    }

}

