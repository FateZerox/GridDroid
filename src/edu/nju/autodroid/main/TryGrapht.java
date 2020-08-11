package edu.nju.autodroid.main;

import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TryGrapht {
    public static void main(String[] args) throws IOException {
        SimpleWeightedGraph<String, DefaultWeightedEdge> biPartitieGraph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(new ClassBasedEdgeFactory<String, DefaultWeightedEdge>(DefaultWeightedEdge.class));


        Set<String> part1 = new HashSet<String>();
        Set<String> part2 = new HashSet<String>();
        Map<String, Double> map1 = new HashMap<>();
        Map<String, Double> map2 = new HashMap<>();


        biPartitieGraph.addVertex("A");
        part1.add("A");
        biPartitieGraph.addVertex("B");
        part1.add("B");
        biPartitieGraph.addVertex("C");
        part1.add("C");
        biPartitieGraph.addVertex("D");
        part1.add("D");
        biPartitieGraph.addVertex("1");
        part2.add("1");
        biPartitieGraph.addVertex("2");
        part2.add("2");
        biPartitieGraph.addVertex("3");
        part2.add("3");

        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("A", "1"), 0.5);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("A", "2"), 0.6);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("A", "3"), 0.7);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("B", "1"), 0.6);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("B", "2"), 0.7);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("B", "3"), 0.8);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("C", "1"), 0.8);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("C", "2"), 0.8);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("C", "3"), 0.9);
        biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge("D", "3"), 0.9);

        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(biPartitieGraph, part1,part2);
        MatchingAlgorithm.Matching<String, DefaultWeightedEdge> matchResult = matching.getMatching();
        System.out.println(matchResult.getWeight());


    }
}
