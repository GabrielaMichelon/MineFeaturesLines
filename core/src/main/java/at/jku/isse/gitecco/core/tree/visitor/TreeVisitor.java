package at.jku.isse.gitecco.core.tree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;

public interface TreeVisitor {
    void visit(RootNode n,String feature);

    void visit(BinaryFileNode n,String feature);

    void visit(SourceFileNode n,String feature);

    void visit(ConditionBlockNode n,String feature);

    void visit(IFCondition c,String feature);

    void visit(IFDEFCondition c,String feature);

    void visit(IFNDEFCondition c,String feature);

    void visit(ELSECondition c,String feature);

    void visit(ELIFCondition c,String feature);

    void visit(Define d,String feature);

    void visit(Undef d,String feature);

    void visit(IncludeNode n,String feature);

    void visit(BaseNode n,String feature);
}
