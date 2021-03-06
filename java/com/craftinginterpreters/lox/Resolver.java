//> Resolving and Binding resolver
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
//> scopes-field
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
//< scopes-field
//> function-type-field
private FunctionType currentFunction = FunctionType.NONE;
//< function-type-field

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
//> function-type
  private enum FunctionType {
    NONE,
/* Resolving and Binding function-type < Classes not-yet
    FUNCTION
*/
//> Classes not-yet
    FUNCTION,
    METHOD,
    INITIALIZER
//< Classes not-yet
  }
//< function-type
//> Classes not-yet

  private enum ClassType {
    NONE,
/* Classes not-yet < Inheritance not-yet
    CLASS
 */
//> Inheritance not-yet
    CLASS,
    SUBCLASS
//< Inheritance not-yet
  }

  private ClassType currentClass = ClassType.NONE;

//< Classes not-yet
//> resolve-statements
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }
//< resolve-statements
//> visit-block-stmt
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }
//< visit-block-stmt
//> Classes not-yet
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    declare(stmt.name);
    define(stmt.name);

    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;
//> Inheritance not-yet

    if (stmt.superclass != null) {
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass);
      beginScope();
      scopes.peek().put("super", true);
    }
//< Inheritance not-yet

    for (Stmt.Function method : stmt.methods) {
      // Push the implicit scope that binds "this" and "class".
      beginScope();
      scopes.peek().put("this", true);

      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }

      resolveFunction(method, declaration);
      endScope();
    }

//> Inheritance not-yet

    if (currentClass == ClassType.SUBCLASS) endScope();

//< Inheritance not-yet
    currentClass = enclosingClass;
    return null;
  }

//< Classes not-yet
//> visit-expression-stmt
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-expression-stmt
//> visit-function-stmt
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

/* Resolving and Binding visit-function-stmt < Resolving and Binding pass-function-type
    resolveFunction(stmt);
*/
//> pass-function-type
    resolveFunction(stmt, FunctionType.FUNCTION);
//< pass-function-type
    return null;
  }
//< visit-function-stmt
//> visit-if-stmt
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }
//< visit-if-stmt
//> visit-print-stmt
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-print-stmt
//> visit-return-stmt
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
//> return-from-top
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Cannot return from top-level code.");
    }

//< return-from-top
    if (stmt.value != null) {
//> Classes not-yet
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword,
            "Cannot return a value from an initializer.");
      }

//< Classes not-yet
      resolve(stmt.value);
    }

    return null;
  }
//< visit-return-stmt
//> visit-var-stmt
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }
//< visit-var-stmt
//> visit-while-stmt
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }
//< visit-while-stmt
//> visit-assign-expr
  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }
//< visit-assign-expr
//> visit-binary-expr
  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//< visit-binary-expr
//> visit-call-expr
  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }
//< visit-call-expr
//> Classes not-yet
  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

//< Classes not-yet
//> visit-grouping-expr
  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }
//< visit-grouping-expr
//> visit-literal-expr
  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }
//< visit-literal-expr
//> visit-logical-expr
  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//< visit-logical-expr
//> Classes not-yet
  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

//< Classes not-yet
//> Inheritance not-yet
  @Override
  public Void visitSuperExpr(Expr.Super expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Cannot use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword,
          "Cannot use 'super' in a class with no superclass.");
    } else {
      resolveLocal(expr, expr.keyword);
    }
    return null;
  }

//< Inheritance not-yet
//> Classes not-yet
  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Cannot use 'this' outside of a class.");
    } else {
      resolveLocal(expr, expr.keyword);
    }
    return null;
  }

//< Classes not-yet
//> visit-unary-expr
  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }
//< visit-unary-expr
//> visit-variable-expr
  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name,
          "Cannot read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }
//< visit-variable-expr
//> resolve-stmt
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }
//< resolve-stmt
//> resolve-expr
  private void resolve(Expr expr) {
    expr.accept(this);
  }
//< resolve-expr
//> resolve-function
/* Resolving and Binding resolve-function < Resolving and Binding set-current-function
  private void resolveFunction(Stmt.Function function) {
*/
//> set-current-function
  private void resolveFunction(
      Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

//< set-current-function
    beginScope();
    for (Token param : function.parameters) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
//> restore-current-function
    currentFunction = enclosingFunction;
//< restore-current-function
  }
//< resolve-function
//> begin-scope
  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }
//< begin-scope
//> end-scope
  private void endScope() {
    scopes.pop();
  }
//< end-scope
//> declare
  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
//> duplicate-variable
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
          "Variable with this name already declared in this scope.");
    }

//< duplicate-variable
    scope.put(name.lexeme, false);
  }
//< declare
//> define
  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }
//< define
//> resolve-local
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }

    // Not found. Assume it is global.
  }
//< resolve-local
}
