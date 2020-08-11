package edu.nju.autodroid.main;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.sun.deploy.util.ReflectionUtil;
import com.sun.xml.internal.stream.buffer.stax.StreamWriterBufferCreator;
import edu.nju.autodroid.hierarchyHelper.LayoutSimilarityAlgorithm;
import edu.nju.autodroid.hierarchyHelper.LayoutTree;
import edu.nju.autodroid.windowtransaction.GroupTransaction;
import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.*;
import rx.Observable;
import rx.functions.Action1;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.util.*;

/**
 * Created by ysht on 2018/1/18.
 */
public class Main_Similarity {
    private static HashMap<Long, Double> simDic = new HashMap<Long, Double>();

    static int groupcount(String filename, File strategyFolder){
        //System.out.println(filename);
        File bm = new File(strategyFolder+"\\"+filename);
        int count = bm.listFiles().length-2;
        return count;
    }

    static double[] readOnlyGroup(String filename, File strategyFolder) throws Exception{
        File bm = new File(strategyFolder+"\\"+filename);
        String line = "";
        for(File f : bm.listFiles()){
            if(!f.getName().equals("graph_output.txt")&&!f.getName().equals("-1.txt")){
                BufferedReader bw = new BufferedReader(new FileReader(f));
                bw.readLine();
                bw.readLine();
                line = bw.readLine();
                bw.close();
                break;
            }
        }
        double[] nodelist = readBirthmarks(line);
        return nodelist;
    }

    static void tryGrapht(){
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
    }

    static double groupnodecountSimilarity(int[] groupnodelist1,int[] groupnodelist2){
        int d = 0;
        int sum = 0;
        for (int i = 0; i < groupnodelist1.length; i++) {
            if(groupnodelist1[i]>=groupnodelist2[i]){
                d += (groupnodelist2[i]);
                sum += (groupnodelist1[i]+groupnodelist2[i]);
            }else {
                d += (groupnodelist1[i]);
                sum += (groupnodelist1[i]+groupnodelist2[i]);
            }
        }

        double simi = (2.0*d)/sum;
        return simi;
    }

    public static void main(String[] args) throws IOException {
        tryGrapht();
        File strategyFolder1 = new File("D:\\download\\AutoDroid-master\\GridWeighted\\strategy_output_Wandoujia");
        File strategyFolder2 = new File("D:\\download\\AutoDroid-master\\GridWeighted\\strategy_output_Wandoujia");

        File strategyFolder_origin_aijiami = new File("D:\\download\\AutoDroid-master\\strategy_output_Original_IJiami");
        File strategyFolder_aijiami = new File("D:\\download\\AutoDroid-master\\strategy_output_IJiami");

        File strategyFolder_Fdroid = new File("D:\\download\\AutoDroid-master\\strategy_output_FDroid");
        File strategyFolder_Fdroid_AndroCrypt = new File("D:\\download\\AutoDroid-master\\strategy_output_FDroid-AndroCrypt");
        File strategyFolder_Fdroid_FakeActivity = new File("D:\\download\\AutoDroid-master\\GridWeighted\\strategy_output_FDroid-FakeActivity");
        File strategyFolder_Fdroid_NestedLayout = new File("D:\\download\\AutoDroid-master\\GridWeighted\\strategy_output_FDroid-NestedLayout");

        File strategyFolder_hybrid_all = new File("D:\\download\\AutoDroid-master\\GridWeighted\\strategy_output_hybrid_all");

        File strategy_output_APKvisible2 = new File("D:\\download\\AutoDroid-master\\strategy_output_APKvisible2");

        BufferedWriter bw = new BufferedWriter(new FileWriter("output_sim_s1.txt"));
        //BufferedWriter bw_filter1 = new BufferedWriter(new FileWriter("output_sim_filter1.txt"));
        BufferedWriter bw_filter2 = new BufferedWriter(new FileWriter("output_sim_filter2.txt"));
        BufferedWriter bw_filter2_nodecount = new BufferedWriter(new FileWriter("output_sim_filter2_nodecount.txt"));

        List<WindowGraph> graphList1 = getGraphsFromDir(strategyFolder_origin_aijiami);
//        graphList1.addAll(getGraphsFromDir(strategyFolder_origin_aijiami));
//        graphList1.addAll(getGraphsFromDir(strategyFolder_aijiami));
//        graphList1.addAll(getGraphsFromDir(strategyFolder_Fdroid));
//        graphList1.addAll(getGraphsFromDir(strategyFolder_Fdroid_AndroCrypt));
//        graphList1.addAll(getGraphsFromDir(strategyFolder_Fdroid_FakeActivity));
//        graphList1.addAll(getGraphsFromDir(strategyFolder_Fdroid_NestedLayout));
//        graphList1.addAll(getGraphsFromDir(strategyFolder_hybrid_all));

        //List<WindowGraph> graphList2 = new ArrayList<WindowGraph>(graphList1);
        List<WindowGraph> graphList2 = getGraphsFromDir(strategyFolder_aijiami);
        System.out.println(graphList1.size());
        System.out.println(graphList2.size());

//        for(int i=0; i<graphList1.size(); i++){
//            WindowGraph graph1 = graphList1.get(i);
//            System.out.println(graph1.getEdgeCount());
//        }
        //统计每个graph所有节点的grid和
        for (WindowGraph graph: graphList1
             ) {
            for (WindowVertex vertex:graph.vertexMap.values()
                 ) {
                for (int i = 0;i<vertex.nodelist.length;i++){
                    graph.GridSum[i] += vertex.nodelist[i];
                }
            }

        }
        for (WindowGraph graph: graphList2
                ) {
            for (WindowVertex vertex:graph.vertexMap.values()
                    ) {
                for (int i = 0;i<vertex.nodelist.length;i++){
                    graph.GridSum[i] += vertex.nodelist[i];
                }
            }

        }
        //用来翻倍数据集测试性能
//        List<WindowGraph> graphList0 = getGraphsFromDir(strategyFolder1);
//        for (int i = 0; i <99 ; i++) {
//            for (int j = 0; j < graphList0.size(); j++) {
//                graphList1.add(graphList0.get(j));
//                graphList2.add(graphList0.get(j));
//            }
//        }
//        System.out.println(graphList1.size()+" "+graphList2.size());

//        for(int i=0; i<graphList1.size(); i++){
//            System.out.println(graphList1.get(i).fileName);
//            for(String s:graphList1.get(i).groupNodeCount.keySet()){
//                System.out.println(s+" "+graphList1.get(i).groupNodeCount.get(s));
//            }
//            System.out.println(graphList1.get(i).groupnodelist);
//        }


        int count = 0;
        int count_gridsum = 0;
        int count_onegroup =0;
        int count_two_times_group_filter = 0;
        int count_group_fileter = 0;
        int count_graph_compare = 0;//最后进行graph二分图比较的
        int errorCount0 = 0;
        int errorCount = 0;
        double graphTime = 0;
        double matchTime = 0;
        double yuzhi_layoutsimi = 0.5;
        double yuzhi_simi = 0.75;
        Long time = System.nanoTime();

        //统计二部图的信息
        int vertexCount=0;
        int edgeCount = 0;

        //统计filter2的node数量分布
        for(int i =0 ;i<graphList1.size();i++){
            WindowGraph graph = graphList1.get(i);
            for(Double s:graph.groupNodeCount.values()){
                if(s>100.0){
                    continue;
                }
                bw_filter2_nodecount.write(s+"");
                bw_filter2_nodecount.newLine();
            }
        }
        bw_filter2_nodecount.close();

        //time = System.nanoTime();
        for(int i=0; i<graphList1.size(); i++){
            for(int j= 0; j<graphList2.size(); j++){

                WindowGraph graph1 = graphList1.get(i);
//                System.out.println(graph1.fileName);
//                System.out.println(groupcount(graph1.fileName,strategyFolder1));
                WindowGraph graph2 = graphList2.get(j);
//                System.out.println(graph2.fileName);
//                System.out.println(groupcount(graph2.fileName,strategyFolder1));
//                if (graph1.fileName.equals(graph2.fileName))
//                    continue;

                count++;//比较对数加一
//                int minGroup = Math.min(groupcount(graph1.fileName,strategyFolder1),groupcount(graph2.fileName,strategyFolder2));
//                int maxGroup = Math.max(groupcount(graph1.fileName,strategyFolder1),groupcount(graph2.fileName,strategyFolder2));
//                System.out.println(graph1.groupcount+" "+graph2.groupcount);

                int minGroup = Math.min(graph1.groupcount,graph2.groupcount);
                int maxGroup = Math.max(graph1.groupcount,graph2.groupcount);

//                bw_filter1.write(getGridSumSim(graph1.GridSum,graph2.GridSum)+"");//记录第一个filter的情况
//                bw_filter1.newLine();
                if (getGridSumSim(graph1.GridSum,graph2.GridSum)<0.65){
                    count_gridsum += 1;
                    //continue;
                }
//
                if(minGroup == 1 && maxGroup == 1){
                    count_onegroup += 1;
                    try {
//                        double[] bm1 = readOnlyGroup(graph1.fileName,strategyFolder1);
//                        double[] bm2 = readOnlyGroup(graph2.fileName,strategyFolder2);
                        double[] bm1 = graph1.OnlyGroup;
                        double[] bm2 = graph2.OnlyGroup;
                        double simi = getOneGroupSim(bm1,bm2);
                        if(simi >= yuzhi_simi && graph2.fileName.equals(graph1.fileName)){
                        //if(simi >= yuzhi_simi && graph2.fileName.equals(graph1.fileName+"_ui_obfuscated")){
                        //if(simi >= yuzhi_simi){
                            errorCount0 ++;
                            System.err.println("error pair0 " + graph1.fileName + " " + graph2.fileName+" "+simi+" "+graph1.groupcount+" "+graph2.groupcount);
                        }
                        bw.write(graph1.fileName + " " + graph2.fileName + " " + simi + " " +simi+" "+ graph1.getEdgeCount() + " " + graph2.getEdgeCount()+" "+graph1.groupcount+" "+graph2.groupcount+" 0 0");
                        bw.newLine();
                    }catch (Exception e){
                    }
                    //continue;
                }
//
//                if ((maxGroup*1.0)/minGroup>2.0) {
//                    count_two_times_group_filter += 1;
//                    bw.write(graph1.fileName + " " + graph2.fileName + " " + 0.0 + " " +0.0+" "+ graph1.getEdgeCount() + " " + graph2.getEdgeCount()+" "+graph1.groupcount+" "+graph2.groupcount+" 0 0");
//                    bw.newLine();
//                    continue;
//                }

                bw_filter2.write(groupnodecountSimilarity(graph1.groupnodelist,graph2.groupnodelist)+"");//记录第2个filter的情况
                bw_filter2.newLine();
                if (groupnodecountSimilarity(graph1.groupnodelist,graph2.groupnodelist)<0.6){
                    count_group_fileter += 1;
                    //bw.write("桶过滤 "+graph1.fileName + " " + graph2.fileName + " " + 0.0 + " " +0.0+" "+ graph1.getEdgeCount() + " " + graph2.getEdgeCount()+" "+graph1.groupcount+" "+graph2.groupcount+" 0 0");
                    //bw.newLine();
                    //continue;
                }

                count_graph_compare += 1;
                SimpleWeightedGraph<WindowEdge, DefaultWeightedEdge> biPartitieGraph = new SimpleWeightedGraph<WindowEdge, DefaultWeightedEdge>(new ClassBasedEdgeFactory<WindowEdge, DefaultWeightedEdge>(DefaultWeightedEdge.class));
                Long graph_construct_time = 0L;
                Long time1 = System.nanoTime();//记录构图的时间

                Set<WindowEdge> part1 = new HashSet<WindowEdge>();
                Set<WindowEdge> part2 = new HashSet<WindowEdge>();
//                Map<WindowEdge,Double> map1 = new HashMap<>();
//                Map<WindowEdge,Double> map2 = new HashMap<>();
                for (WindowEdge e1: graph1.getEdges()) {
                    biPartitieGraph.addVertex(e1);
                    part1.add(e1);
                    for(WindowEdge e2 : graph2.getEdges()){
                        biPartitieGraph.addVertex(e2);
                        part2.add(e2);
                        double simi = getMaxSim(graph1,e1,graph2,e2);
//                        if(map1.get(e1) == null)
//                        {
//                            map1.put(e1,simi);
//                        }else {
//                            if(simi > map1.get(e1)){
//                                map1.put(e1,simi);
//                            }
//                        }
//                        if(map2.get(e2) == null)
//                        {
//                            map2.put(e2,simi);
//                        }else {
//                            if(simi > map2.get(e2)){
//                                map2.put(e2,simi);
//                            }
//                        }
                        if(simi >=yuzhi_layoutsimi)
                        //{
                            biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge(e1, e2), simi);
                            //System.out.println(e1.hashCode()+" "+e2.hashCode()+" "+simi);
                        //}
                    }
                }
                graph_construct_time = (System.nanoTime()-time1);
                graphTime += graph_construct_time;

                vertexCount+=biPartitieGraph.vertexSet().size();
                edgeCount+=biPartitieGraph.edgeSet().size();

                simDic.clear();
                long singlematchtime = 0L;
                Long time2 = System.nanoTime();//记录匹配算法的时间
                MaximumWeightBipartiteMatching<WindowEdge, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(biPartitieGraph, part1,part2);
                MatchingAlgorithm.Matching<WindowEdge, DefaultWeightedEdge> matchResult = matching.getMatching();
                double graph_weight = 0;
                Set<DefaultWeightedEdge> matchedEdges =  matchResult.getEdges();
                for(DefaultWeightedEdge edge:matchedEdges){
                    try {
                        Method method = DefaultWeightedEdge.class.getDeclaredMethod("getWeight");
                        method.setAccessible(true);
                        String s = method.invoke(edge).toString();
                        graph_weight += Double.parseDouble(s);
                    }catch (Exception e){
                        System.out.println(e);
                    }
                }
                //double graph_weight = matchResult.getWeight();
                singlematchtime = (System.nanoTime()-time2);
                matchTime += singlematchtime;

                int minVertex = Math.min(graph1.getEdgeCount(), graph2.getEdgeCount());
                double sim = graph_weight*1.0/minVertex;
                //if(minVertex > 1 && maxGroup*1.0/minGroup<=2.0)
                {
                    if(sim >= yuzhi_simi &&graph2.fileName.equals(graph1.fileName)){
                    //if(sim >= yuzhi_simi && graph2.fileName.equals(graph1.fileName+"_ui_obfuscated")){
                    //if(sim >= yuzhi_simi){
                        //System.err.println(groupnodecountSimilarity(graph1.groupnodelist,graph2.groupnodelist));
                        errorCount ++;
                        System.err.println("error pair " + graph1.fileName + " " + graph2.fileName+" "+graph_weight+" "+sim+" "+graph1.getEdgeCount() + " " + graph2.getEdgeCount()+" "+graph1.groupcount+" "+graph2.groupcount+" "+graph_construct_time+" "+singlematchtime);
                    }
                }
                /*if(minVertex <= 1||maxGroup*1.0/minGroup>2.0){
                    continue;
                }*/
                //System.out.println("matchResult " + matchResult.getWeight()/10+ " " + graph1.fileName + " " + graph2.fileName + " " + graph1.getEdgeCount() + " " + graph2.getEdgeCount());
                bw.write(graph1.fileName + " " + graph2.fileName + " "+graph_weight+" "+sim+" "+ graph1.getEdgeCount() + " " + graph2.getEdgeCount()+" "+graph1.groupcount+" "+graph2.groupcount+" "+ graph_construct_time+" "+singlematchtime);
                bw.newLine();
            }
        }


        bw.close();
        //bw_filter1.close();
        bw_filter2.close();
        System.out.println("graph grid sum小于阈值："+count_gridsum);
        System.out.println("graph只有一个group的比较："+count_onegroup);
        System.out.println("group数量差别超过两倍的filter:"+count_two_times_group_filter);
        System.out.println("group的特征向量数值和桶分类的filter："+count_group_fileter);
        System.out.println("最后进行graph二分图比较的："+count_graph_compare);
        System.out.println("所有比较finish in " + (System.nanoTime()-time) + " ns");
        System.out.println("每对比较average time " + (System.nanoTime()-time)/(graphList1.size()*graphList2.size()/2) + " ns/对");
        System.out.println("error count " + errorCount);
        System.out.println("error count0 " + errorCount0);
        System.out.println("graph construction time " + graphTime+ " ns");
        System.out.println("graph construction time " + graphTime/(graphList1.size()*graphList2.size()/2)+ " ns/对");
        System.out.println("MatchingAlgorithm time " + matchTime+ " ns");
        System.out.println("MatchingAlgorithm time " + matchTime/(graphList1.size()*graphList2.size()/2)+ " ns/对");
        //System.out.println("二部图信息，总共pair:"+count_graph_compare+" 平均vertex:"+vertexCount/count_graph_compare+" 平均edge:"+edgeCount/count_graph_compare);
    }

    //边之间的相似度
    static double getMaxSim(WindowGraph g1, WindowEdge e1, WindowGraph g2, WindowEdge e2)
    {
        double sim1,sim2;
        sim1 = getMaxSim2(g1.vertexMap.get(e1.v1),g2.vertexMap.get(e2.v1)) +  getMaxSim2(g1.vertexMap.get(e1.v2),g2.vertexMap.get(e2.v2));
        // sim2 = getMaxSim(g1.vertexMap.get(e1.v2),g2.vertexMap.get(e2.v1)) +  getMaxSim(g1.vertexMap.get(e1.v1),g2.vertexMap.get(e2.v2));
        // return (Math.max(sim1,sim2))/2.0;
        return sim1/2.0;
    }

    //d点之间的相似度
    static double getMaxSim(WindowVertex v1, WindowVertex v2){
        double maxSim = 0;
        if(v1 == null || v2== null)
            System.out.println("null!");
        Long vv = Long.parseLong(v1.windowID)*3+Long.parseLong(v2.windowID);
        if(simDic.containsKey(vv))
            return simDic.get(vv);
        for(LayoutTree lt1 : v1.layoutTreeList){
            for(LayoutTree lt2 : v2.layoutTreeList){
                double sim = lt1.similarityWith(lt2, LayoutSimilarityAlgorithm.RectArea);
                maxSim = Math.max(maxSim, sim);
            }
        }
        simDic.put(vv, maxSim);
        return maxSim;
    }

    static double getGridSumSim(double[] v1, double[] v2) {
        double d = 0;
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            if(v1[i]>=v2[i]){
                d += (v2[i]);
                sum += (v1[i]+v2[i]);
            }else {
                d += (v1[i]);
                sum += (v1[i]+v2[i]);
            }
        }

        double simi = (2.0*d)/sum;
        return simi;
    }

    static double getOneGroupSim(double[] v1, double[] v2){
//        double d = 0;
//        double sum = 0;
//        for (int i = 0; i < v1.length; i++) {
//            double max = 1.0;
//            if(v1[i]>=v2[i]){
//                d = v1[i]-v2[i];
//                max += v1[i];
//            }else {
//                d = v2[i]-v1[i];
//                max += v2[i];
//            }
//            double s = d/max;
//            sum += s;
//        }
//        double dict = sum/v1.length;
//        return 1-dict;
        double d = 0;
        double sum = 0;
        double alpha = 0.5;
        for (int i = 0; i < v1.length; i++) {
            if(v1[i]>=v2[i]){
                d += v2[i]+alpha;
                sum += v1[i]+alpha;
            }else {
                d += v1[i]+alpha;
                sum += v2[i]+alpha;
            }
        }

        double simi = d/sum;
        return simi;


    }

    //d点之间的相似度
//    static double getMaxSim(WindowVertex v1, WindowVertex v2){
//        double maxSim = 0;
//        if(v1 == null || v2== null)
//            System.out.println("null!");
//        String vv = v1.windowID+" "+v2.windowID;
//        if(simDic.containsKey(vv))
//            return simDic.get(vv);
////        for(LayoutTree lt1 : v1.layoutTreeList){
//////            for(LayoutTree lt2 : v2.layoutTreeList){
//////                double sim = lt1.similarityWith(lt2, LayoutSimilarityAlgorithm.GridCenterNormal);
//////                maxSim = Math.max(maxSim, sim);
//////            }
//////        }
//        //if(!filter1(v1.nodelist,v2.nodelist))
//            //return 0;
////        if(!filter2(v1.nodelist,v2.nodelist)) {
////            simDic.put(vv, 0.0);
////            return 0;
////        }
//
//        if(!filter3(v1.nodelist_2,v2.nodelist_2)) {
//            simDic.put(vv, 0.0);
//            return 0;
//        }
//
//        double d = 0;
//        double sum = 0;
//        for (int i = 0; i < 48; i++) {
//            double max = 1.0;
//            if(v1.nodelist[i]>=v2.nodelist[i]){
//                d = v1.nodelist[i]-v2.nodelist[i];
//                max += v1.nodelist[i];
//            }else {
//                d = v2.nodelist[i]-v1.nodelist[i];
//                max += v2.nodelist[i];
//            }
//            double s = d/max;
//            sum += s;
//        }
//        for (int i = 48*3; i < 48*4; i++) {
//            double max = 1.0;
//            if(v1.nodelist[i]>=v2.nodelist[i]){
//                d = v1.nodelist[i]-v2.nodelist[i];
//                max += v1.nodelist[i];
//            }else {
//                d = v2.nodelist[i]-v1.nodelist[i];
//                max += v2.nodelist[i];
//            }
//            double s = d/max;
//            sum += s;
//        }
//        double dict = sum*2.0/v1.nodelist.length;
//        simDic.put(vv, 1-dict);
//        return 1-dict;
//    }

    static boolean filter1(double[] v1,double[] v2){
        double countv1up = 0;
        double countv1down = 0;
        double countv2up = 0;
        double countv2down = 0;
        for (int i = 0;i<24;i++){
            countv1up += v1[i];
            countv1up += v1[i+48];
            countv1up += v1[i+48*2];
            countv1up += v1[i+48*3];
            countv2up += v2[i];
            countv2up += v2[i+48];
            countv2up += v2[i+48*2];
            countv2up += v2[i+48*3];
        }
        for (int i = 24;i<48;i++){
            countv1down += v1[i];
            countv1down += v1[i+48];
            countv1down += v1[i+48*2];
            countv1down += v1[i+48*3];
            countv2down += v2[i];
            countv2down += v2[i+48];
            countv2down += v2[i+48*2];
            countv2down += v2[i+48*3];
        }
        if(countv1up/countv2up>=2.0||countv2up/countv1up>=2.0||countv1down/countv2down>=2.0||countv2down/countv1down>=2.0)
            return false;
        return true;
    }

    //对于特征向量值超过2倍的直接计算相似度为0
    static boolean filter2(double[] a1,double[] a2){
        double sum1 = 0;
        double sum2 = 0;
        for (int i = 0;i<a1.length;i++){
            sum1 += a1[i];
            sum2 += a2[i];
        }
        if (sum1/sum2 >2.0 || sum2/sum1>2.0)
            return false;
        return true;
    }

    //层级筛选
    static boolean filter3(double[] a1,double[] a2){
        double d = 0;
        double sum = 0;
        for (int i = 0; i < a1.length; i++) {
            if(a1[i]>=a2[i]){
                d += a2[i];
                sum += a1[i]+a2[i];
            }else {
                d += a1[i];
                sum += a2[i]+a1[i];
            }
        }
        double simi = (2.0*d)/sum;
        if (simi > 0.1)
            return true;
        return false;
    }

    //d点之间的相似度
    static double getMaxSim2(WindowVertex v1, WindowVertex v2){
        double maxSim = 0;
        if(v1 == null || v2== null)
            System.out.println("null!");
        Long vv = Long.parseLong(v1.windowID)*3+Long.parseLong(v2.windowID);
        //simiDic保存初始graph的点与点之间相似度，即layout之间相似度
        if(simDic.containsKey(vv))
            return simDic.get(vv);
//        for(LayoutTree lt1 : v1.layoutTreeList){
////            for(LayoutTree lt2 : v2.layoutTreeList){
////                double sim = lt1.similarityWith(lt2, LayoutSimilarityAlgorithm.GridCenterNormal);
////                maxSim = Math.max(maxSim, sim);
////            }
////        }

//        if(!filter3(v1.nodelist_2,v2.nodelist_2)) {
//            simDic.put(vv, 0.0);
//            return 0;
//        }
//        double d = 0;
//        double sum = 0;
//        double alpha = 0.308;
//        for (int i = 0; i < v1.nodelist.length; i++) {
//            if(v1.nodelist[i]>=v2.nodelist[i]){
//                d += (v2.nodelist[i]+alpha);
//                sum += (v1.nodelist[i]+alpha);
//            }else {
//                d += (v1.nodelist[i]+alpha);
//                sum += (v2.nodelist[i]+alpha);
//            }
//        }
//
//        double simi = d/sum;
//        simDic.put(vv, simi);
//        return simi;
        double d = 0;
        double sum = 0;
        for (int i = 0; i < v1.nodelist.length; i++) {
            if(v1.nodelist[i]>=v2.nodelist[i]){
                d += (v2.nodelist[i]);
                sum += (v1.nodelist[i]+v2.nodelist[i]);
            }else {
                d += (v1.nodelist[i]);
                sum += (v2.nodelist[i]+v2.nodelist[i]);//改成v2
            }
        }

        double simi = (2.0*d)/sum;
        simDic.put(vv, simi);
        return simi;
    }

    static List<WindowGraph> getGraphsFromDir(File dir) throws IOException {
        List<WindowGraph> graphList = new ArrayList<WindowGraph>();
        for (File d : dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        })){
            WindowGraph g = null;
            for(File f : d.listFiles()){
                if(f.getName().equals("graph_output.txt")){
                    g = getGraphFromFile(f.getPath());
                }
            }
            g.fileName = d.getName();
            System.out.println("get graph " + g.fileName);
            for(File f : d.listFiles()){
                if(!f.getName().equals("graph_output.txt")&&!f.getName().equals("-1.txt")){
                    String windowId = f.getName().substring(0, f.getName().lastIndexOf("."));
                    if(g.vertexMap.containsKey(windowId)) {
                        BufferedReader bw = new BufferedReader(new FileReader(f));
                        String line;
                        bw.readLine();
                        bw.readLine();
                        while ((line = bw.readLine()) != null) {
//                            String[] ws = line.split(" ");
//                            if (ws.length >= 3) {
//                                g.vertexMap.get(windowId).layoutTreeList.add(new LayoutTree(line));
//                            }
                            g.vertexMap.get(windowId).nodelist = readBirthmarks(line);
                            g.vertexMap.get(windowId).nodelist_2 = readBirthmarks_2(line);
                        }
                        bw.close();
                    }
                }
            }
            g.groupcount = groupcount(g.fileName, dir);
            if(g.groupcount == 1){
                try{
                    g.OnlyGroup = readOnlyGroup(g.fileName, dir);
                }catch (Exception e){

                }
            }
            for(String s:g.vertexMap.keySet()){
                WindowVertex  w= g.vertexMap.get(s);
                Double nodecount = 0.0;
                for (int i = 0;i< w.nodelist.length/2;i++){
                    nodecount += w.nodelist[i];
                }
                g.groupNodeCount.put(s,nodecount);
                if(nodecount<=8){
                    g.groupnodelist[0] += 1;
                }else if (nodecount>8 && nodecount <= 16){
                    g.groupnodelist[1] += 1;
                }else if (nodecount>16 && nodecount <= 24){
                    g.groupnodelist[2] += 1;
                }else if (nodecount>24 && nodecount <= 32){
                    g.groupnodelist[3] += 1;
                }else if (nodecount>32 && nodecount <= 40){
                    g.groupnodelist[4] += 1;
                }else if (nodecount>48 && nodecount <= 56){
                    g.groupnodelist[5] += 1;
                } else if (nodecount>56 && nodecount <= 64){
                    g.groupnodelist[6] += 1;
                }else if (nodecount>64 && nodecount <= 72){
                    g.groupnodelist[7] += 1;
                }else if (nodecount>72 && nodecount <= 80){
                    g.groupnodelist[8] += 1;
                } else {
                   g.groupnodelist[9] += 1;
                }
            }
            graphList.add(g);
        }
        return graphList;
    }

    static double[] readBirthmarks(String line){
        double[] nodelist = new double[48*2];
        String[] ws = line.split(" ");
        for(int i = 0;i<48*2;i++){
            nodelist[i] = Double.parseDouble(ws[i].split("\n")[0]);
        }

        return nodelist;
    }

    static double[] readBirthmarks_2(String line){
        double[] nodelist = new double[48];
        String[] ws = line.split(" ");
        for(int i = 0;i<48*2;i++){
            nodelist[i/2] += Double.parseDouble(ws[i].split("\n")[0]);
        }

        return nodelist;
    }

    //从graph_output.txt读取
    static WindowGraph getGraphFromFile(String strategyFile) throws IOException {
        WindowGraph graph = new WindowGraph();
        BufferedReader br = new BufferedReader(new FileReader(strategyFile));
        String line;
        while((line=br.readLine()) != null){
            String[] ws = line.split(" ");
            if(ws.length >= 3){
                if(!(ws[0].equals("-1") || ws[1].equals("-1")))
                    graph.AddEdge(ws[0], ws[1]);
            }
        }
        return graph;
    }
}

