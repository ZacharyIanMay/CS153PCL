package backend.interpreter;

import org.antlr.v4.runtime.ParserRuleContext;

import antlr4.*;
import intermediate.symtab.*;

public class Executor extends Pcl4BaseVisitor<Object>
{
    private Symtab symtab = new Symtab();
    
    @Override 
    public Object visitProgram(Pcl4Parser.ProgramContext ctx)
    {
        return visit(ctx.block().compoundStatement());
    }
    
    @Override 
    public Object visitAssignmentStatement(Pcl4Parser.AssignmentStatementContext ctx)
    {
        String variableName = ctx.lhs().variable().getText();
        Double value = (Double) visit(ctx.rhs());
        assign(variableName, value);

        return null;
    }
    
    private void assign(String variableName, Double value)
    {
        SymtabEntry variableId = symtab.lookup(variableName);
        if (variableId == null) variableId = symtab.enter(variableName);
        
        variableId.setValue(value);
    }
    
    @Override 
    public Object visitRepeatStatement(Pcl4Parser.RepeatStatementContext ctx)
    {
        Pcl4Parser.StatementListContext listCtx = ctx.statementList();
        boolean value;
        
        do
        {
            visit(listCtx);
            value = (Boolean) visit(ctx.expression());
        } while (!value);
        
        return null;
    }
    
    @Override 
    public Object visitWhileStatement(Pcl4Parser.WhileStatementContext ctx)
    {
        return null;
    }
    
    @Override 
    public Object visitIfStatement(Pcl4Parser.IfStatementContext ctx)
    {
        return null;
    }

    @Override
    public Object visitForto(Pcl4Parser.FortoContext ctx)
    {
        //visit variable: returns id or 0
        //if 0 add new entry to the symtab with value = visit forone
        //begin while loop with condition to check the value of variable against fortwo
        //  visit statement
        //  increment variable
        SymtabEntry var = symtab.lookup(ctx.variable().getText());
        if(var == null)
        {
            var = symtab.enter(ctx.variable().getText());
        }
        double varval = (double)visit(ctx.forOne());
        var.setValue(varval);
        double until = (double)visit(ctx.forTwo());
        while(varval <= until)
        {
            visit(ctx.statement());
            var.setValue(varval+1);
            varval = (double)visit(ctx.variable());
        }
        return null;
    }

    @Override
    public Object visitFordownto(Pcl4Parser.FordowntoContext ctx)
    {
        //visit variable: returns id or 0
        //if 0 add new entry to the symtab with value = visit forone
        //begin while loop with condition to check the value of variable against fortwo
        //  visit statement
        //  decrement variable
        SymtabEntry var = symtab.lookup(ctx.variable().getText());
        if(var == null)
        {
            var = symtab.enter(ctx.variable().getText());
        }
        double varval = (double)visit(ctx.forOne());
        var.setValue(varval);
        double until = (double)visit(ctx.forTwo());
        while(varval >= until)
        {
            visit(ctx.statement());
            var.setValue(varval-1);
            varval = (double)visit(ctx.variable());
        }
        return null;
    }
    
    @Override 
    public Object visitCaseStatement(Pcl4Parser.CaseStatementContext ctx)
    {
    	Double value = (Double) visit(ctx.expression());    //get expression
    	for (int i = 0; i < ctx.constantList.size(); i++)    //test each constant list
        {
    		Pcl4Parser.CaseStatementContext constants = ctx.constantList(i);    //get constants from a constant list
            
            for (int j = 0; j < constants.constant.size(); j++)    //test each constant in the constant list
            {
            	if (((Double) visit(constants.constant(j))).equals(value))    //test whether constant equals original expression
                {
                	return visit(ctx.statement(j));    //evaluate the statement of that constant
                }
            }
        }
        return null;
    }

    @Override 
    public Object visitWritelnStatement(Pcl4Parser.WritelnStatementContext ctx)
    {
        visitChildren(ctx);
        System.out.println();

        return null;
    }

    @Override 
    public Object visitWriteArgumentList(Pcl4Parser.WriteArgumentListContext ctx) 
    {
        // Loop over each argument.
        for (Pcl4Parser.WriteArgumentContext argCtx : ctx.writeArgument())
        {
            // Print the expression value with a format specifier.
            Object value = visit(argCtx.expression());
            StringBuffer format = new StringBuffer("%");
            
            // Create the format string.
            Pcl4Parser.FieldWidthContext fwCtx = argCtx.fieldWidth();              
            if (fwCtx != null)
            {
                String sign = (   (fwCtx.sign() != null) 
                               && (fwCtx.sign().getText().equals("-"))) 
                            ? "-" : "";
                format.append(sign)
                      .append(fwCtx.integerConstant().getText());
                
                Pcl4Parser.DecimalPlacesContext dpCtx = 
                                                    fwCtx.decimalPlaces();
                if (dpCtx != null)
                {
                    format.append(".")
                          .append(dpCtx.integerConstant().getText());
                }
            }
            
            // Use the format string with printf.
            if (value instanceof Double)
            {
                format.append("f");
                System.out.printf(format.toString(), (double) value);
            }
            else  // String
            {
                format.append("s");
                System.out.printf(format.toString(), (String) value);
            }
        }

        return null;
    }

    @Override 
    public Object visitExpression(Pcl4Parser.ExpressionContext ctx) 
    {
        Pcl4Parser.SimpleExpressionContext simpleCtx1 = ctx.simpleExpression(0);
        Pcl4Parser.RelOpContext relOpCtx = ctx.relOp();
        Object operand1 = visit(simpleCtx1);
        
        // More than one simple expression?
        if (relOpCtx != null)
        {
            String op = relOpCtx.getText();
            Pcl4Parser.SimpleExpressionContext simpleCtx2 = 
                                                        ctx.simpleExpression(1);
            double value1 = (Double) operand1;
            double value2 = (Double) visit(simpleCtx2);
            boolean result = false;
                
            if      (op.equals("=" )) result = value1 == value2;
            else if (op.equals("<>")) result = value1 != value2;
            else if (op.equals("<" )) result = value1 <  value2;
            else if (op.equals("<=")) result = value1 <= value2;
            else if (op.equals(">" )) result = value1 >  value2;
            else if (op.equals(">=")) result = value1 >= value2;
            
            return result;
        }
        
        return operand1;
    }

    @Override 
    public Object visitSimpleExpression(Pcl4Parser.SimpleExpressionContext ctx) 
    {
        int count = ctx.term().size();
        Boolean negate =    (ctx.sign() != null) 
                         && ctx.sign().getText().equals("-");
        
        // First term.
        Pcl4Parser.TermContext termCtx1 = ctx.term(0);
        Object operand1 = visit(termCtx1);       
        if (negate) operand1 = -((Double) operand1);
        
        // Loop over the subsequent terms.
        for (int i = 1; i < count; i++)
        {
            String op = ctx.addOp(i-1).getText().toLowerCase();
            Pcl4Parser.TermContext termCtx2 = ctx.term(i);
            Object operand2 = visit(termCtx2);

            if (operand2 instanceof Double)
            {
                double value1 = (Double) operand1;
                double value2 = (Double) operand2;
                operand1 = (op.equals("+")) ? value1 + value2
                                            : value1 - value2;
            }
            else if (operand2 instanceof Boolean)
            {
                operand1 = ((Boolean) operand1) || ((Boolean) operand2);
            }
            else  // String
            {
                operand1 = ((String) operand1) + ((String) operand2);
            }
        }
        
        return operand1;
    }

    @Override 
    public Object visitTerm(Pcl4Parser.TermContext ctx) 
    {
        int count = ctx.factor().size();
        
        // First factor.
        Pcl4Parser.FactorContext factorCtx1 = ctx.factor(0);
        Object operand1 = visit(factorCtx1);
        
        // Loop over the subsequent factors.
        for (int i = 1; i < count; i++)
        {
            String op = ctx.mulOp(i-1).getText().toLowerCase();
            Pcl4Parser.FactorContext factorCtx2 = ctx.factor(i);
            Object operand2 = visit(factorCtx2);            

            if (operand2 instanceof Double)
            {
                double value1 = (Double) operand1;
                double value2 = (Double) operand2;
                
                if (op.equals("*")) operand1 = value1*value2;
                
                else if (   (op.equals("/")) 
                         || (op.equals("div")) 
                         || (op.equals("mod")))
                {
                    // Check for division by zero.
                    if (value2 == 0) 
                    {
                        runtimeError("Division by zero", factorCtx2);
                        operand1 = 0;
                    }
                    
                    else if (op.equals("/")) operand1 = value1/value2;
                    else
                    {
                        long long1 = (long) value1;
                        long long2 = (long) value2;
                        long result = (op.equals("div")) ? long1/long2 : long1%long2;
                        operand1 = (double) result;
                    }
                }
            }
            else  // Boolean
            {
                operand1 = ((Boolean) operand1) && ((Boolean) operand2);
            }
        }
        
        return operand1;
    }

    @Override 
    public Object visitNotFactor(Pcl4Parser.NotFactorContext ctx) 
    {
        boolean value = (boolean) visit(ctx.factor());
        return !value;
    }

    @Override 
    public Object visitParenthesizedExpression(
                                Pcl4Parser.ParenthesizedExpressionContext ctx) 
    {
        return visit(ctx.expression());
    }

    @Override 
    public Object visitVariable(Pcl4Parser.VariableContext ctx)
    {
        String variableName = ctx.getText();
        SymtabEntry variableId = symtab.lookup(variableName);
        Double value = variableId != null ? variableId.getValue() : 0.0;
        
        return value;
    }
    
    @Override 
    public Object visitNumber(Pcl4Parser.NumberContext ctx)
    {
        boolean negate =    (ctx.sign() != null)
                         && (ctx.sign().getText().equals("-"));
        Double value = (Double) visit(ctx.unsignedNumber());
        if (negate) value = -value;
        
        return value;
    }
    
    @Override 
    public Object visitIntegerConstant(Pcl4Parser.IntegerConstantContext ctx)
    {
        return Double.parseDouble(ctx.getText());
    }
    
    @Override 
    public Object visitRealConstant(Pcl4Parser.RealConstantContext ctx)
    {
        return Double.parseDouble(ctx.getText());
    }
    
    @Override 
    public Object visitCharacterConstant(Pcl4Parser.CharacterConstantContext ctx)
    {
        String pascalString = ctx.CHARACTER().getText();        
        return convertString(pascalString);
    }

    @Override 
    public Object visitStringConstant(Pcl4Parser.StringConstantContext ctx)
    {
        String pascalString = ctx.STRING().getText();        
        return convertString(pascalString);
    }
    
    /**
     * Convert a Pascal string to a Java string.
     * @param pascalString the Pascal string.
     * @return the Java string.
     */
    private String convertString(String pascalString)
    {
        String unquoted = pascalString.substring(1, pascalString.length()-1);
        return unquoted.replace("''", "'");
    }

    /**
     * Flag a runtime error.
     * @param node the root node of the offending statement or expression.
     * @param message the runtime error message.
     * @param ctx the context.
     */
    public void runtimeError(String message, ParserRuleContext ctx)
    {
        System.out.printf("\n*** RUNTIME ERROR at line %03d: %s\n",
                          ctx.getStart().getLine(), message);
    }
}
