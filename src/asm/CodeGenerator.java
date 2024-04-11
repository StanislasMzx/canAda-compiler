package asm;

import ast.GraphViz;
import ast.Node;
import tds.*;
import tds.Record;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;


public class CodeGenerator {
    private final FileWriter fileWriter;
    public Stack<StackFrame> stackFrames; // TO CHANGE
    private Stack<String> asmStack;
    private Boolean codeGenOn = true;
    private TDS tds;
    private int region;
    private List<String> callableElements = new ArrayList<>();

    public CodeGenerator(String fileName) {
        try {
            this.fileWriter = new FileWriter(fileName+"-output.s");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.stackFrames = new Stack<>();
        this.asmStack = new Stack<>();
    }

    public void setTDS(TDS tds){
        this.tds = tds;
    }

    public void setRegion(int region){
        this.region = region;
    }

    public void setCodeGenOn(Boolean codeGenOn) {
        this.codeGenOn = codeGenOn;
    }

    public void write(String s) {
        try {
            fileWriter.write(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void procedureGen(String name, String last, String fatherName) {
        if (codeGenOn) {
            stackFrames.push(new StackFrame(name + last));
            callableElements.add(name);
            String label = name + callableElements.lastIndexOf(name) + "global";
            if(fatherName == null){
                appendToBuffer("\tldr\tr10, =" + label + "\n\tstr\tr13, [r10]\n\t;PARAMETERS\n");
                startBufferAppend("\t" + label + "\tDCD\t0xFFFFFFFF\n");
                return;
            }
            String labelParent = fatherName + callableElements.lastIndexOf(fatherName) + "global";
            appendToBuffer("\tldr\tr10, =" + label + "\n\tldr\tr10, [r10]\n\tstmfd\tr13!, {r10}\n\tldr\tr10, ="+ labelParent +"\n\tldr\tr12, [r10]\n\tstmfd\tr13!, {r12}\n\tldr\tr10, =" + label + "\n\tstr\tr13, [r10]\n\tstmfd\tr13!, {r11}\n\tmov\tr11, r13\n");
            appendToBuffer("\t;PARAMETERS\n");
            startBufferAppend("\t" + label + "\tDCD\t0xFFFFFFFF\n");
            endBufferAppend("\tmov\tr13, r11\n\tldmfd\tr13!, {r11}\n\tadd\tr13, r13, #4\n\tldr\tr10, ="+label+"\n\tldmfd\tr13!, {r12}\n\tstr\tr12, [r10]\n");
        }
    }

    public void functionGen(String name, String last, String fatherName) {
        if (codeGenOn) {
            stackFrames.push(new StackFrame(name + last));
            callableElements.add(name);
            String label = name + callableElements.lastIndexOf(name) + "global";
            String labelParent = fatherName + callableElements.lastIndexOf(fatherName) + "global";
            appendToBuffer("\tldr\tr10, =" + label + "\n\tldr\tr10, [r10]\n\tstmfd\tr13!, {r10}\n\tldr\tr10, ="+ labelParent +"\n\tldr\tr12, [r10]\n\tstmfd\tr13!, {r12}\n\tldr\tr10, =" + label + "\n\tstr\tr13, [r10]\n\tstmfd\tr13!, {r11}\n\tmov\tr11, r13\n");
            appendToBuffer("\t;PARAMETERS\n");
            startBufferAppend("\t" + label + "\tDCD\t0xFFFFFFFF\n");
            endBufferAppend("\tmov\tr13, r11\n\tldmfd\tr13!, {r11}\n\tadd\tr13, r13, #4\n\tldr\tr10, ="+label+"\n\tldmfd\tr13!, {r12}\n\tstr\tr12, [r10]\n");
        }
    }

    public void varGen(List<Symbol> symbolsOfRegion) {
        if (codeGenOn) {
            this.appendToBuffer("\t;VARIABLES\n");
            int lastOffset = -1;
            for (Symbol symbol : symbolsOfRegion) {
                if (symbol instanceof Var) {
                    lastOffset = ((Var) symbol).getOffset();
                    this.appendToBuffer("\t\t;" + ((Var) symbol).getType() + "\t" + symbol.getName() + "\n");
                }
            }
            if (lastOffset != -1) {
                this.appendToBuffer("\tsub\tr13, r13, #" + lastOffset + "\n");
            }
        }
    }

    public void appendToBuffer(String s) {
        if (codeGenOn) {
            if (!stackFrames.isEmpty()) {
                stackFrames.peek().getBuffer().append(s);
            }
        }
    }

    public void endBufferAppend(String s){
        if(codeGenOn) {
            if (!stackFrames.isEmpty()) {
                stackFrames.peek().getEndBuffer().append(s);
            }
        }
    }

    public void startBufferAppend(String s){
        if(codeGenOn) {
            if (!stackFrames.isEmpty()) {
                stackFrames.firstElement().getStartBuffer().append(s);
            }
        }
    }

    public void endBlock() {
        if (codeGenOn) {
            if (!stackFrames.isEmpty()) {
                StackFrame stackFrame = stackFrames.pop();
                asmStack.push(stackFrame.toString(stackFrames.isEmpty()));
            }
        }
    }

    public void writeDownBlocks() {
        if (codeGenOn) {
            // copy the content of the print.s file at the beginning of the output file
            try {
                List<String> lines = Files.readAllLines(Paths.get("src/asm/visual/print.s"));
                for (String line : lines) {
                    this.write(line + "\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (!asmStack.isEmpty()) {
                this.write(asmStack.pop());
            }

            this.write("mul\tstmfd\tr13!, {r0-r2, r11, lr} ; This is PreWritten Code for multiplication\n\tmov\tr11, r13\n\tldr\tr1, [r11, #4*6]\n\tldr\tr2, [r11, #4*7]\n\tmov\tr0, #0\nmul_loop\tlsrs\tr2,r2,#1\n\taddcs\tr0,r0,r1\n\tlsl\tr1,r1,#1\n\ttst\tr2,r2\n\tbne\tmul_loop\n\tstr\tr0, [r11, #4*5]\n\tmov\tr13, r11\n\tldmfd\tr13!,{r0-r2, r11, pc}\n\n");
            this.write("div\tstmfd\tsp!, {r0-r5, r11, lr} ; This is PreWritten Code for division\n\tmov\tr11, r13\n\tldr\tr1, [r11, #4*9]\n\tldr\tr2, [r11, #4*10]\n\tmov\tr0,#0\n\tmov\tr3,#0\n\tcmp\tr1, #0\n\trsblt\tr1, r1,#0\n\teorlt\tr3, r3, #1\n\tcmp\tr2, #0\n\trsblt\tr2, r2,#0\n\teorlt\tr3, r3, #1\n\tmov\tr4, r2\n\tmov\tr5, #1\ndiv_max\tlsl\tr4, r4, #1\n\tlsl\tr5,r5,#1\n\tcmp\tr4, r1\n\tble\tdiv_max\ndiv_loop\tlsr\tr4, r4, #1\n\tlsr\tr5, r5, #1\n\tcmp\tr4, r1\n\tbgt\tdiv_loop\n\tadd\tr0, r0, r5\n\tsub\tr1, r1, r4\n\tcmp\tr1, r2\n\tbge\tdiv_loop\n\tcmp\tr3, #1\n\tbne\tdiv_exit\n\tcmp\tr1,#0\n\taddne\tr0, r0,#1\n\trsb\tr0, r0, #0\n\trsb\tr1, r1, #0\n\taddne\tr1,r1,r2\ndiv_exit\tstr\tr0, [r11, #4*8]\n\tldmfd\tr13!, {r0-r5, r11, pc}\n\n");

            try {
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void assignationGen(GraphViz ast, Node node) {
//        System.out.println(ast.getTree().nodes.get(node.getChildren().get(0)).getLabel());
//        System.out.println(tds.getTds());
//        System.out.println("--------------------");
        // TODO assign the value
        int exprRegister = 0;
        boolean isRegisterBorrowed = false;
        try {
            exprRegister = stackFrames.peek().getRegisterManager().borrowRegister();
        } catch (RuntimeException e) {
            exprRegister = 0;
            isRegisterBorrowed = true;
            appendToBuffer("\tstmfd\tr13!, {r" + exprRegister + "} ; No more register available, making space with memory stack\n");
        }
        expressionGen(ast, node.getChildren().get(1), exprRegister);
        // Get the var to assign
        // Set the value
        if(isRegisterBorrowed) {
            appendToBuffer("\tldmfd\tr13!, {r" + exprRegister + "} ; Freeing memory stack\n"); // Free the register
        } else {
            stackFrames.peek().getRegisterManager().freeRegister(exprRegister);
        }
    }

    public void stackArg(GraphViz ast, Integer nodeInt) {
        if (codeGenOn) {
            int register;
            boolean isRegisterBorrowed = false;
            try {
                register = stackFrames.peek().getRegisterManager().borrowRegister();
            } catch (RuntimeException e) {
                register = 0;
                isRegisterBorrowed = true;
                appendToBuffer("\tsub\tr13, r13, #4 ; keeping some space to stack the arg\n\tstmfd\tr13!, {r" + register + "} ; No more register available, making space with memory stack\n");
            }
            expressionGen(ast, nodeInt, register);
            if (isRegisterBorrowed) {
                appendToBuffer("\tstr\tr" + register + ", [r13, #4] ; Stacking the arg\n");
                appendToBuffer("\tldmfd\tr13!, {r" + register + "} ; Freeing memory stack\n");
            } else {
                appendToBuffer("\tstmfd\tr13!, {r" + register + "} ; Stacking the arg\n");
                stackFrames.peek().getRegisterManager().freeRegister(register);
            }
        }
    }

    public void expressionGen(GraphViz ast, Integer nodeInt, int returnRegister) {
        if(codeGenOn){
            Node node = ast.getTree().nodes.get(nodeInt);
            String type = node.getLabel();
            try {
                int number = Integer.parseInt(type);
                if(number > 256){
                    appendToBuffer("\tldr\tr" + returnRegister + ", =" + number + " ; Generating number for expression\n");
                } else {
                    appendToBuffer("\tmov\tr" + returnRegister + ", #" + number + " ; Generating number for expression\n");
                }
                return;
            } catch (NumberFormatException e) {
                // Not a number so we continue
            }

            int register1;
            boolean isR1Borrowed = false;

            switch (type) {
                case "*" :
                    expressionGen(ast, node.getChildren().get(1), returnRegister);
                    try { // If no register available, we use memory stack
                        register1 = stackFrames.peek().getRegisterManager().borrowRegister();
                    } catch (RuntimeException e) {
                        if (returnRegister != 0) {
                            register1 = 0;
                        }
                        else {
                            register1 = 1;
                        }
                        appendToBuffer("\tstmfd\tr13!, {r" + register1 + "} ; No more register available, making space with memory stack\n");
                        isR1Borrowed = true;
                    }
                    expressionGen(ast, node.getChildren().get(0), register1);
                    appendToBuffer("\n\tstmfd\tr13!, {r" + returnRegister + ",r" + register1 + "} ; Block for multiplication : " + ast.getTree().nodes.get(node.getChildren().get(1)).getLabel() + " * " + ast.getTree().nodes.get(node.getChildren().get(0)).getLabel() + "\n\tsub\tr13, r13, #4\n\tbl\tmul\n\tldr r" + returnRegister + ", [r13]\n\tadd\tr13, r13, #4*3 ; 2 paramètres et 1 valeur de retour\n\n");
                    break;
                case "/" :
                    expressionGen(ast, node.getChildren().get(1), returnRegister);
                    try { // If no register available, we use memory stack
                        register1 = stackFrames.peek().getRegisterManager().borrowRegister();
                    } catch (RuntimeException e) {
                        if (returnRegister != 0) {
                            register1 = 0;
                        }
                        else {
                            register1 = 1;
                        }
                        appendToBuffer("\tstmfd\tr13!, {r" + register1 + "} ; No more register available, making space with memory stack\n");
                        isR1Borrowed = true;
                    }
                    expressionGen(ast, node.getChildren().get(0), register1);
                    appendToBuffer("\tstmfd\tr13!, {r" + returnRegister + "} ; Block for division : " + ast.getTree().nodes.get(node.getChildren().get(1)).getLabel() + " / " + ast.getTree().nodes.get(node.getChildren().get(0)).getLabel() + "\n\tstmfd\tr13!, {r" + register1 + "}\n\tsub\tr13, r13, #4\n\tbl\tdiv\n\tldr r" + returnRegister + ", [r13]\n\tadd\tr13, r13, #4*3 ; 2 paramètres et 1 valeur de retour\n\n");
                    break;
                case "+" :
                    expressionGen(ast, node.getChildren().get(0), returnRegister);
                    try { // If no register available, we use memory stack
                        register1 = stackFrames.peek().getRegisterManager().borrowRegister();
                    } catch (RuntimeException e) {
                        if (returnRegister != 0) {
                            register1 = 0;
                        }
                        else {
                            register1 = 1;
                        }
                        appendToBuffer("\tstmfd\tr13!, {r" + register1 + "} ; No more register available, making space with memory stack\n");
                        isR1Borrowed = true;
                    }
                    expressionGen(ast, node.getChildren().get(1), register1);
                    appendToBuffer("\tadd\tr" + returnRegister + ", r" + returnRegister + ", r" + register1 + " ; Block for addition : " + ast.getTree().nodes.get(node.getChildren().get(0)).getLabel() + " + " + ast.getTree().nodes.get(node.getChildren().get(1)).getLabel() + "\n\n");
                    break;
                case "-" :
                    expressionGen(ast, node.getChildren().get(1), returnRegister);
                    try { // If no register available, we use memory stack
                        register1 = stackFrames.peek().getRegisterManager().borrowRegister();
                    } catch (RuntimeException e) {
                        if (returnRegister != 0) {
                            register1 = 0;
                        }
                        else {
                            register1 = 1;
                        }
                        appendToBuffer("\tstmfd\tr13!, {r" + register1 + "} ; No more register available, making space with memory stack\n");
                        isR1Borrowed = true;
                    }
                    expressionGen(ast, node.getChildren().get(0), register1);
                    appendToBuffer("\tsub\tr" + returnRegister + ", r" + returnRegister + ", r" + register1 + " ; Block for substraction : " + ast.getTree().nodes.get(node.getChildren().get(1)).getLabel() + " - " + ast.getTree().nodes.get(node.getChildren().get(0)).getLabel() + "\n\n");
                    break;
                default: // Variable à aller chercher
                    getVar(type, returnRegister);
//                    appendToBuffer("\t; Unhandeled expression (for the moment) : " + type + "\n");
//                    throw new RuntimeException("Unhandeled expression : " + type);
                    return;
            }
            if (isR1Borrowed){
                appendToBuffer("\tldmfd\tr13!, {r" + register1 + "} ; Freeing memory stack\n");
            } else {
                stackFrames.peek().getRegisterManager().freeRegister(register1);
            }
        }
    }

    public void callGen(Symbol symbol) {
        if (codeGenOn) {
            String name = symbol.getName() + region;
            if (Objects.equals(name, "put0")) {
               appendToBuffer("\t; CALL put (not yet implemented)\n");
            } else {
                appendToBuffer("\tbl\t" + name + " ; CALL\n");
            }
            appendToBuffer("\t; End of call\n\n");
        }
    }

    public void getVar(String name, int returnRegister) {
        if (codeGenOn) {
            // get the region in the TDS
//            System.out.println("Nesting level : "+tds.getTds().get(region).get(0).getNestingLevel());
//            System.out.println("Search for : " + name);
            for (Symbol i : tds.getTds().get(region)) {
                if (i.getName().equals(name)) {
                    if(i instanceof Var){
                        int offset = ((Var) i).getOffset();
                        appendToBuffer("\tldr\tr" + returnRegister + ", [r11, #-" + offset + "-4] ; Getting the value of " + name + "\n"); // -4 because r11 pointing on base and not on 1st element
                    } else if (i instanceof Param){
                        appendToBuffer(";Coming soon Param\n");
                        return; // TODO
//                        int offset = ((Param) i).getOffset();
//                        System.out.println("Here");
//                        System.out.println(offset);
//                        appendToBuffer("\tldr\tr" + returnRegister + ", [r11, #" + offset);
//                        appendToBuffer("] ; Getting the value of " + name + "\n");
                    } else if (i instanceof Record) {
                        appendToBuffer(";Coming soon Record\n");
                        return; // TODO
//                        int offset = ((Record) i).getOffset();
//                        System.out.println("Found : "+i.getName()+" at offset : "+offset+" as a Record");
                    } else {
                        throw new RuntimeException("Unhandeled type of Symbol : "+i+" named "+i.getName() + " of type "+i.getClass());
                    }
                }
            }
//            System.out.println("--------------------");
        }
    }

    public void stackReturn(Symbol symbol, int region) {
        if (codeGenOn) {
            String name = symbol.getName() + region;
            if (symbol instanceof Func) {
                appendToBuffer("\tsub\tr13, r13, #" + TDS.offsets.get(((Func )symbol).getReturnType()) + " ; " + name + " return val init\n");
            }
        }
    }
}