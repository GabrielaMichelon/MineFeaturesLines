package at.jku.isse.gitecco.translation.constraintcomputation.util;


import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.RootNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.visitor.BuildImplicationsVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class ConstraintComputer {

    private final List<String> featureList;

    public ConstraintComputer(List<String> featureList) {
        this.featureList = featureList;
    }

    /**
     * Computes and returns the configuration for a given changed Node.
     * Also filters the solution for only global features.
     *
     * @param changedNode
     * @param tree
     * @return
     */
    public Map<Feature, Integer> computeConfig(ConditionalNode changedNode, RootNode tree) {
        //if the changed node is a base node just return BASE as solution
        if (changedNode instanceof BaseNode) {
            Map<Feature, Integer> ret = new HashMap<>();
            ret.put(new Feature("BASE"), 1);
            return ret;
        }

        //get the changedNode condition give to us the global condition, which give as if exist a parent node
        //and if yes it gives to us the features of this parent node until reach BASE
        //and if we just have external features, we do not need to look for defines, because any block above
        //and any block that covers (in cases that the changedNode has parents besides Base) will be selected
        //or not if user selects. So, we need to give 1/true for all these literals.
        Feature feature = new Feature("");
        ExpressionSolver solver = new ExpressionSolver();
        Map<Feature, Queue<FeatureImplication>> implMap = new HashMap<>();
        solver.setExpr(changedNode.getCondition());

        ArrayList<Feature> literalsExpression = feature.parseConditionArray(changedNode.getCondition());
        Boolean onlyExternal = true;
        for (Feature feat : literalsExpression) {
            if (!featureList.contains(feat.getName())) {
                onlyExternal = false;
            }
        }
        if (!onlyExternal) {


            //setup and build constraint queues
            BuildImplicationsVisitor visitor = new BuildImplicationsVisitor(implMap, tree, changedNode.getLineFrom());
            try {
                changedNode.getContainingFile().accept(visitor, null);
            } catch (StackOverflowError stackOverflowError) {
                //.out.println("DO SOMETHING");
            }
        /*BuildImplicationsVisitor visitorInclude, visitorIncludeAux;
        List<IncludeNode> includeNodes = visitor.getIncludeNodes();
        ArrayList<IncludeNode> includeNodesAux3 =  new ArrayList<>();
        List<IncludeNode> includeNodesAux =  new ArrayList<>();
        List<IncludeNode> includeNodesAux4 =  includeNodes;
        for (IncludeNode includeNode : includeNodesAux4) {
            FileNode sfn = tree.getChild(includeNode.getFileName());
            if(sfn != null){
                visitorInclude = new BuildImplicationsVisitor(implMap, tree, includeNode);
                sfn.accept(visitorInclude,null);
                FileNode sfnAAux;
                if(visitorInclude.getIncludeNodes().size()>0){
                    includeNodesAux = visitorInclude.getIncludeNodes();
                    List<IncludeNode> includeNodesAux2 =  new ArrayList<>();
                    for (IncludeNode inclInsideIncl:includeNodesAux) {
                        if(!includeNodes.contains(inclInsideIncl) && tree.getChild(inclInsideIncl.getFileName()) != null) {
                            includeNodesAux2.add(inclInsideIncl);
                            includeNodes.add(inclInsideIncl);
                        }
                    }
                    includeNodesAux3.addAll(includeNodesAux2);
                    while (includeNodesAux3.size()>0){
                        includeNodesAux2 =  new ArrayList<>();
                        for (IncludeNode includeNode1 : includeNodesAux3){
                            if(!includeNodes.contains(includeNode1) && tree.getChild(includeNode1.getFileName()) != null) {
                                visitorIncludeAux = new BuildImplicationsVisitor(implMap, tree, includeNode1);
                                sfnAAux = tree.getChild(includeNode1.getFileName());
                                sfnAAux.accept(visitorIncludeAux,null);
                                includeNodes.add(includeNode1);
                                if(visitorIncludeAux.getIncludeNodes().size()>0)
                                    includeNodesAux2.addAll(visitorIncludeAux.getIncludeNodes());
                            }
                        }
                        if(includeNodesAux2.size()>0)
                            includeNodesAux3.addAll(includeNodesAux2);
                    }
                }
            }
        }*/

            //hand the expression of the condition for the changed node to the solver.

            feature = new Feature("");
            //ArrayList<Feature> literalsExpression = feature.parseConditionArray(changedNode.getCondition());
            ArrayList<Feature> newliteralsToConsider = new ArrayList<>();
            Map<Feature, Queue<FeatureImplication>> implMapFeaturesExpression = new HashMap<>();

            for (Feature literalToConsider : literalsExpression) {
                for (Map.Entry<Feature, Queue<FeatureImplication>> implMapAux : implMap.entrySet()) {
                    Queue<FeatureImplication> queueAux = new LinkedList<>();
                    for (FeatureImplication featureimplication : implMapAux.getValue()) {
                        if (featureimplication.getCondition().contains(literalToConsider.getName())) {
                            queueAux.add(featureimplication);
                            if (!(literalsExpression.contains(implMapAux.getKey())) && !(newliteralsToConsider.contains(implMapAux.getKey()))) {
                                newliteralsToConsider.add(implMapAux.getKey());
                            }
                        }
                    }
                    implMapFeaturesExpression.put(implMapAux.getKey(), queueAux);
                }
            }
            while (newliteralsToConsider.size() > 0) {
                ArrayList<Feature> literalsAux = newliteralsToConsider;
                literalsExpression.addAll(newliteralsToConsider);
                newliteralsToConsider = new ArrayList<>();
                for (Feature literalToConsider : literalsAux) {
                    for (Map.Entry<Feature, Queue<FeatureImplication>> implMapAux : implMap.entrySet()) {
                        Queue<FeatureImplication> queueAux = new LinkedList<>();
                        for (FeatureImplication featureimplication : implMapAux.getValue()) {
                            if (featureimplication.getCondition().contains(literalToConsider.getName())) {
                                queueAux.add(featureimplication);
                                literalsExpression.add(literalToConsider);
                                if (!(literalsExpression.contains(implMapAux.getKey())) && !(newliteralsToConsider.contains(implMapAux.getKey()))) {
                                    newliteralsToConsider.add(implMapAux.getKey());
                                }
                            }
                        }
                        implMapFeaturesExpression.put(implMapAux.getKey(), queueAux);
                    }
                }

            }

            //add the built constraint queues to the solver which further constructs all the internal constraints
            for (Map.Entry<Feature, Queue<FeatureImplication>> featureQueueEntry : implMapFeaturesExpression.entrySet()) {
                solver.addClause(featureQueueEntry.getKey(), featureQueueEntry.getValue());
            }
        }

        //solve, filter for global features only and return the solution/configuration.
        //filtering should be optional because if a correct constraint is built we already get only global features.
        Map<Feature, Integer> ret = solver.solve();
        if (ret != null) {
            ret = ret
                    .entrySet()
                    .stream()
                    .filter(entry -> featureList.contains(entry.getKey().getName()))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
        } else {
            System.err.println("DEAD CODE: No solution found for "
                    + changedNode.getLocalCondition()
                    + " @ " + changedNode.getLineFrom()
                    + " in " + changedNode.getContainingFile().getFilePath());
        }

        return ret;
    }

    /**
     * Computes which features should be marked as changed.
     * <p>
     * This can and will be replaced with a set of different
     * heuristics to achieve the most convenient and fitting result.
     *
     * @param changedNode
     * @return
     */
    public Set<Feature> computeChangedFeatures(ConditionalNode changedNode, Map<Feature, Integer> config) {
        //if the changed node is a base node just return BASE as solution
        if (changedNode instanceof BaseNode) {
            Set<Feature> ret = new HashSet<>();
            ret.add(new Feature("BASE"));
            return ret;
        }

        ExpressionSolver solver = new ExpressionSolver();
        boolean repeat = true;
        Set<Feature> ret = null;

        StringBuilder configString = new StringBuilder();
        config.entrySet().forEach(x -> configString.append(" || ( " + x.getKey().getName() + "==" + x.getValue() + " )"));
        String configClause = configString.toString().replaceFirst("\\|\\|", "");
        configClause = " && ( " + configClause + ")";

        while ((changedNode != null || !(changedNode instanceof BaseNode)) && repeat) {
            solver.setExpr(changedNode.getLocalCondition() + configClause);
            Map<Feature, Integer> result = solver.solve();
            if (result != null) {
                ret = result
                        .entrySet()
                        .stream()
                        .filter(entry -> featureList.contains(entry.getKey().getName()) && entry.getValue() != 0)
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toSet());

                repeat = ret.size() < 1 ? true : false;
            } else {
                repeat = true;
            }
            if (repeat) changedNode = changedNode.getParent().getParent();
        }

        //if (ret == null || repeat) {
        if (ret == null) {
            ret = new HashSet<>();
            ret.add(new Feature("BASE"));
        }
        Feature feature = new Feature("BASE");
        //if ret > 1 means it contains a changed node of a feature, so we do not consider BASE, only consider BASE when the changed node does not belong to any feature
        if (ret.size() > 1) {
            for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                if (ret.contains(feat.getKey()) && feat.getValue() == 0) {
                    ret.remove(feat.getKey());
                }
            }
        }

        if (ret.size() < 1) {
            ret.add(feature);
        }

        return ret;
    }
}

