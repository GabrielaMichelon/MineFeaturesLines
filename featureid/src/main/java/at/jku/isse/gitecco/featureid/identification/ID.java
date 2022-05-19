package at.jku.isse.gitecco.featureid.identification;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.featuretree.visitor.GetAllFeaturesDefinesIncludesVisitor;
import at.jku.isse.gitecco.featureid.type.FeatureType;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;

import java.util.*;
import java.util.stream.Collectors;

public class ID {

    public static Map<Feature, Integer> mapOfFeatures =  new HashMap<>();
    public static List<DefineNode> allDefines = new ArrayList<>();
    /**
     * Identifies all features and their types that occur in a tree of a commit.
     *
     * @param tree
     * @return
     */
    public static Map<Feature, FeatureType> id(RootNode tree) {
        /*
         * idea:
         *        - traverse the tree for each file
         *        - for each file iterate over all found features
         *        - for each feature iterate over all defines and compare their line value
         *        - if a define occurs before the use of the feature --> internal
         *        - if a feature is used without a single define of it in the file --> external
         *        - if a feature is used before a define of it in the same file --> transient
         *        - every time a value would be stored in the map check for the current value
         *        - only override if the types is weaker than the one in the map
         *        - after this procedure the map contains all features and its types
         *          for one commit.
         *
         *       Further steps: continue with evaluating the map --> translating this result into a set of
         *                      traceable features.
         *
         */
        Map<Feature, FeatureType> featureMap = new HashMap<>();
        mapOfFeatures =  new HashMap<>();
        GetAllFeaturesDefinesIncludesVisitor allincVisitor = new GetAllFeaturesDefinesIncludesVisitor();
        FeatureType type;
        boolean sameName;
        int lineResult;
        //FileNode tmpF = null;

        for (FileNode child : tree.getChildren()) {
            if (child instanceof SourceFileNode) {
                allincVisitor.reset();
                child.accept(allincVisitor, null);
                allDefines.addAll(allincVisitor.getDefines());
                mapOfFeatures.putAll(allincVisitor.getFeatureMap());
            }
        }

        for (Map.Entry<Feature, Integer> entry : mapOfFeatures.entrySet()) {
            List<DefineNode> filteredDefines = allDefines.stream()
                    .filter(s -> entry.getKey().getName().equals(s.getMacroName()))
                    .collect(Collectors.toList());
            for (DefineNode define : filteredDefines) {
                type = featureMap.get(entry.getKey());
                sameName = entry.getKey().getName().equals(define.getMacroName());
                lineResult = entry.getValue().compareTo(define.getLineInfo());
                if (sameName && lineResult < 0) {
                    //feature is defined but also appears before its define:
                    featureMap.put(entry.getKey(), FeatureType.TRANSIENT);
                    //System.err.println("Feature: " + entry.getKey().getName() + " is transient at some point!");
                    break;
                } else if (sameName && lineResult >= 0) {
                    //feature is defined, and never (at least until this define) appears before its define:
                    if (type == null || !type.equals(FeatureType.TRANSIENT))
                        featureMap.put(entry.getKey(), FeatureType.INTERNAL);
                }
            }

            type = featureMap.get(entry.getKey());
            //if the feature has never been defined in this file it is external:
            if (type == null || type.equals(FeatureType.EXTERNAL))
                featureMap.put(entry.getKey(), FeatureType.EXTERNAL);
        }


        /*for (FileNode child : tree.getChildren()) {
            allincVisitor.reset();
            allDefines.clear();

            if (child instanceof SourceFileNode) {

                child.accept(allincVisitor, null);

                // use includes to get all other defines.
                //idea:  - for each include retrieve the subtree
                //       - collect all defines form that subtree
                //       - create virtual define nodes, with all the collected defines.
                //       - give every virtual define the line number of the include statement
                //       - proceed as usual.

                //retrieve virtual defines from all includes
               /* for (IncludeNode include : allincVisitor.getIncludes()) {
                    definesVisitor.reset();
                    tmpF = tree.getChild(include.getFileName());
                    System.out.println("for includes ID1!");
                    if (tmpF != null) tmpF.accept(definesVisitor);
                    for (DefineNode define : definesVisitor.getDefines()) {
                        //just acting like undefs are just defines because in this scenario it does not matter
                        allDefines.add(new Define(define.getMacroName(), null, include.getLineInfo(), (ConditionalNode) include.getParent()));
                    }
                }

                //local defines of the current file
                allDefines.addAll(allincVisitor.getDefines());

                for (Map.Entry<Feature, Integer> entry : allincVisitor.getFeatureMap().entrySet()) {
                    allDefines.clear();
                    includeVisitor.reset();
                    allDefines.addAll(allincVisitor.getDefines());
                    //retrieve virtual defines from all includes
                    for (IncludeNode include : allincVisitor.getIncludes()) {
                        definesVisitor.reset();
                        tmpF = tree.getChild(include.getFileName());
                        if (tmpF != null) {
                            tmpF.accept(definesVisitor, entry.getKey().getName());
                            tmpF.accept(includeVisitor, entry.getKey().getName());
                        }

                        for (DefineNode define : definesVisitor.getDefines()) {
                            //just acting like undefs are just defines because in this scenario it does not matter
                            allDefines.add(new Define(define.getMacroName(), null, include.getLineInfo(), (ConditionalNode) include.getParent()));
                        }
                    }

                    for (DefineNode define : allDefines) {
                        type = featureMap.get(entry.getKey());
                        sameName = entry.getKey().getName().equals(define.getMacroName());
                        lineResult = entry.getValue().compareTo(define.getLineInfo());
                        if (sameName && lineResult < 0) {
                            //feature is defined but also appears before its define:
                            featureMap.put(entry.getKey(), FeatureType.TRANSIENT);
                            System.err.println("Feature: " + entry.getKey().getName() + " is transient at some point!");
                            break;
                        } else if (sameName && lineResult >= 0) {
                            //feature is defined, and never (at least until this define) appears before its define:
                            if (type == null || !type.equals(FeatureType.TRANSIENT))
                                featureMap.put(entry.getKey(), FeatureType.INTERNAL);
                        }
                    }

                    type = featureMap.get(entry.getKey());
                    //if the feature has never been defined in this file it is external:
                    if (type == null || type.equals(FeatureType.EXTERNAL))
                        featureMap.put(entry.getKey(), FeatureType.EXTERNAL);
                }
            }
        } */
        return featureMap;
    }

    /**
     * Evaluates the map that results from analyzing one commit.
     * For every occurrence of a feature the counter is increased corresponding to its types.
     *
     * @param evalList A list of traceable features.
     * @param map      The map that results from the id method
     * @return the list that was passed.
     */
    public static List<TraceableFeature> evaluateFeatureMap(List<TraceableFeature> evalList, Map<Feature, FeatureType> map, Long commitNumber) {
        for (Map.Entry<Feature, FeatureType> entry : map.entrySet()) {
            if (evalList.contains(entry.getKey())) {
                evalList.get(evalList.indexOf(entry.getKey())).inc(entry.getValue());
                evalList.get(evalList.indexOf(entry.getKey())).setListcommit(commitNumber, true);
            } else {
                evalList.add(new TraceableFeature(entry.getKey()).inc(entry.getValue()));
                evalList.get(evalList.indexOf(entry.getKey())).setListcommit(commitNumber, true);
            }
        }
        return evalList;
    }

}
