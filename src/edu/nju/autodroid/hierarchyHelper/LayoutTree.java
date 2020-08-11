package edu.nju.autodroid.hierarchyHelper;

import com.github.davidmoten.rtree.Entries;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import edu.nju.autodroid.utils.Utils;
import edu.nju.autodroid.utils.Logger;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import rx.functions.Action1;

import javax.rmi.CORBA.Util;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.parseBoolean;

/**
 * Created by ysht on 2016/3/7 0007.
 * 用于保存Layout的数据结构（以树的形式）
 */
public class LayoutTree {
    private LayoutNode root;//LayoutTree是一个根节点为空的树，根节点不包含数据
    private String layoutXML;
    //findAll函数的变量，用于保存findAll中的中间结果
    private List<LayoutNode> findList = new ArrayList<LayoutNode>();

    public double[] nodelist = new double[48*2];

    private int totalChildrenCountBeforeCompress = 0;

    //用于rect area相似度判断的R树。会在第一次使用的时候创建。
    private RTree<LayoutNode, Rectangle> layoutRTree = null;

    public LayoutTree(String layoutXML){
        root = new LayoutNode();
        try{
            this.layoutXML = layoutXML;
            createTree(layoutXML);
            createNodelist(layoutXML);
            totalChildrenCountBeforeCompress = root.getTotalChildrenCount();
            compressTree();
        }
        catch (Exception e){
            Logger.logException(e);
        }
    }

    //当一个viewgroup（包含子节点）的child个数只有1个的话，那么将child替代该节点
    private void compressTree(){
        compressTree(root);
    }

    private void compressTree(LayoutNode node){
        for (LayoutNode n : node.getChildren()){
            LayoutNode temp = n;
            while(temp.getChildrenCount() == 1){
                node.replaceChild(temp.getChildren().get(0), temp);
                temp = temp.getChildren().get(0);
            }
        }

        for (LayoutNode n : node.getChildren()){
            compressTree(n);
        }
    }

    private void createNodelist(String layoutXML)throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(layoutXML.getBytes("utf-8")));
        Element rootEle = doc.getDocumentElement();
        if(rootEle == null)
            return;
        NamedNodeMap rootnm = rootEle.getAttributes();
        String rotation = rootnm.getNamedItem("rotation").getNodeValue();//获取界面rotation属性
        LayoutNodeList nlist = root.getChildren();
        if(nlist == null)
            return;
        for(int i = 0;i < nlist.size();i++){
            if (nlist.get(i).getChildrenCount() == 0){
                //System.out.println(rotation+nlist.get(i).bound[0]+" "+nlist.get(i).bound[1]+" "+nlist.get(i).bound[2]+" "+nlist.get(i).bound[3]);
                BoundNode node1 = new BoundNode(rotation,nlist.get(i).bound);
                nodelist[node1.x0+node1.y0*6] += 1;
                nodelist[node1.x3+node1.y3*6+48] += 1;
            }else {
                for(int j = 0;j< nlist.get(i).getChildren().size();j++){
                    recursionNodelist(rotation,nlist.get(i).getChildren().get(j));
                }
            }
        }
        conv(nodelist,nodelist.length);
    }

    private void recursionNodelist(String rotation,LayoutNode node){
        if (node.getChildrenCount() == 0){
            //System.out.println(rotation+node.bound[0]+" "+node.bound[1]+" "+node.bound[2]+" "+node.bound[3]);
            BoundNode node1 = new BoundNode(rotation,node.bound);
            nodelist[node1.x0+node1.y0*6] += 1;
            nodelist[node1.x3+node1.y3*6+48] += 1;
        }else {
            for(int i = 0;i< node.getChildren().size();i++){
                recursionNodelist(rotation,node.getChildren().get(i));
            }
        }
    }

    //对节点进行卷积
    private void conv(double[] nodelist, int length){
        double[] copy = Arrays.copyOf(nodelist, length);
        double a = 0.75;
        double b = 0.5;
        int size = length/2;
        for(int k = 0;k<2;k++) {
            for (int i = size * k; i < size * (k + 1); i++) {
                if (i+1>=size*k && i+1 <size*(k+1))
                    nodelist[i] += copy[i+1]*a;
                if (i-1>=size*k && i-1 <size*(k+1))
                    nodelist[i] += copy[i-1]*a;
                if (i+6>=size*k && i+6 <size*(k+1))
                    nodelist[i] += copy[i+6]*a;
                if (i-6>=size*k && i-6 <size*(k+1))
                    nodelist[i] += copy[i-6]*a;
                if (i-7>=size*k && i-7 <size*(k+1))
                    nodelist[i] += copy[i-7]*b;
                if (i-5>=size*k && i-5 <size*(k+1))
                    nodelist[i] += copy[i-5]*b;
                if (i+5>=size*k && i+5 <size*(k+1))
                    nodelist[i] += copy[i+5]*b;
                if (i+7>=size*k && i+7 <size*(k+1))
                    nodelist[i] += copy[i+7]*b;
            }
        }

    }

    public String getLayoutXML(){
        return layoutXML;
    }

    public String getNodelist(){
        String result = ""+nodelist[0];
        for(int i = 1;i<nodelist.length;i++){
            result = result+" "+nodelist[i];
        }
        return result;
    }

    public int getTreeSize(){return root.getTotalChildrenCount();}

    public int getTreeSizeBeforeCompress(){return totalChildrenCountBeforeCompress;}

    public int[] getScreenSize(){
        for(LayoutNode node : root.getChildren()){
            return new int[]{node.bound[2], node.bound[3]};
        }
        return new int[]{480, 800};
    }

    /**
     * 计算当前layoutTree和另一个layoutTree的相似度,采用树的BFS序列的编辑距离最为参照
     * @param layoutTree 待比较的另一个layoutTree
     * @return 0.0-1.0之间的相似度值
     */
    public double similarityWith(LayoutTree layoutTree){
       /* if(layoutTree.layoutXML.equals(this.layoutXML))
            return 1.0;*///事实上这个更浪费时间

        int editDis = Utils.EditDistance(getTreeBFSHashes(), layoutTree.getTreeBFSHashes());
        return 1.0-editDis*1.0/Math.max(root.getTotalChildrenCount(),layoutTree.getTreeSize());
    }

    public double similarityWith(LayoutTree layoutTree, LayoutSimilarityAlgorithm algorithm){
        switch (algorithm){
            case BFSThenEditdistane:
                return similarityWith(layoutTree);
            case RectArea:
                return similarityWithByRectArea(layoutTree);
            case RegionRatio:
                return similarityWithByRegionRatio(layoutTree);
            case GridNormal:
                return similarityWithGridNormal(layoutTree);
            default:
                throw new UnsupportedOperationException("不支持的相似度计算算法");
        }
    }

    protected double similarityWithGridNormal(LayoutTree layoutTree){
        double d = 0;
        double sum = 0;
        for (int i = 0; i < layoutTree.nodelist.length; i++) {
            if(nodelist[i]>=layoutTree.nodelist[i]){
                d += layoutTree.nodelist[i];
                sum += (nodelist[i]+layoutTree.nodelist[i]);
            }else {
                d += nodelist[i];
                sum += (nodelist[i]+layoutTree.nodelist[i]);
            }
        }
        double simi = (2.0*d)/sum;
        return simi;
//        double d = 0;
//        double sum = 0;
//        for (int i = 0; i < layoutTree.nodelist.length; i++) {
//            if(nodelist[i]>=layoutTree.nodelist[i]){
//                d += layoutTree.nodelist[i];
//                sum += nodelist[i];
//            }else {
//                d += nodelist[i];
//                sum += layoutTree.nodelist[i];
//            }
//        }
//        double simi = d/sum;
//        return simi;
//        for (int i = 0; i < layoutTree.nodelist.length; i++) {
//            double max = 1;
//            if(nodelist[i]>=layoutTree.nodelist[i]){
//                d = nodelist[i]-layoutTree.nodelist[i];
//                max += nodelist[i];
//            }else {
//                d = layoutTree.nodelist[i]-nodelist[i];
//                max += layoutTree.nodelist[i];
//            }
//            double s = d/max;
//            sum += s;
//        }
//        double dict = sum/nodelist.length;
//
//        return 1-dict;
    }

    protected double similarityWithByRectArea(LayoutTree layoutTree){
//        if(layoutRTree == null){
//            createRTree();
//        }
//
//        if(layoutTree.layoutRTree == null){
//            layoutTree.createRTree();
//        }
//        double[] tsim = new double[1];
//        int[] count = new int[1];
//        layoutTree.layoutRTree.entries().subscribe(new Action1<Entry<LayoutNode, Rectangle>>() {
//            @Override
//            public void call(Entry<LayoutNode, Rectangle> otherEntry) {
//                //对每个，查找覆盖的元素
//                double[] maxSim = new double[]{0};
//                layoutRTree.search(otherEntry.geometry()).subscribe(new Action1<Entry<LayoutNode, Rectangle>>() {
//                    @Override
//                    public void call(Entry<LayoutNode, Rectangle> myEntry) {
//                        //找到有重叠的区域，那么则计算覆盖率
//                        double sim = Utils.getNormalizedOverlapArea(myEntry.value().bound, otherEntry.value().bound);
//                        if(sim > maxSim[0])
//                            maxSim[0] = sim;
//                    }
//                });
//                tsim[0] += maxSim[0];
//                count[0] += 1;
//            }
//        });
//
//        if(count[0]== 0)
//            return 0;
//
//        return tsim[0]/count[0];
        if(layoutRTree == null){
            createRTree();
        }

        if(layoutTree.layoutRTree == null){
            layoutTree.createRTree();
        }
        double[] tsim = new double[1];
        int[] count = new int[1];
        RTree minRtree, maxRTree;
        if(layoutTree.layoutRTree.size()<layoutRTree.size())
        {
            minRtree = layoutTree.layoutRTree;
            maxRTree = layoutRTree;
        }
        else{
            maxRTree = layoutTree.layoutRTree;
            minRtree = layoutRTree;
        }
        minRtree.entries().subscribe(new Action1<Entry<LayoutNode, Rectangle>>() {
            @Override
            public void call(Entry<LayoutNode, Rectangle> otherEntry) {
                //对每个，查找覆盖的元素
                double[] maxSim = new double[]{0};
                maxRTree.search(otherEntry.geometry()).subscribe(new Action1<Entry<LayoutNode, Rectangle>>() {
                    @Override
                    public void call(Entry<LayoutNode, Rectangle> myEntry) {
                        //找到有重叠的区域，那么则计算覆盖率
                        double sim = Utils.getNormalizedOverlapArea(myEntry.value().bound, otherEntry.value().bound);
                        if(sim > maxSim[0])
                            maxSim[0] = sim;
                    }
                });
                tsim[0] += maxSim[0];
                count[0] += 1;
            }
        });

        if(count[0]== 0)
            return 0;

        return tsim[0]/minRtree.size();
    }

    //不对layoutRTree是否为空进行判断。直接生成新的RTree赋给layoutRTree
    protected RTree createRTree(){
        layoutRTree = RTree.create();
        List<LayoutNode> leafLayoutNode = findAll(new Predicate<LayoutNode>() {
            @Override
            public boolean test(LayoutNode node) {
                return node.getChildrenCount() == 0;
            }
        }, TreeSearchOrder.DepthFirst);

        List<Entry<LayoutNode, Rectangle>> entryList = new ArrayList<Entry<LayoutNode, Rectangle>>();
        for(LayoutNode n : leafLayoutNode){
            int x1, x2, y1, y2;
            x1 = Math.min(n.bound[0], n.bound[2]);
            x2 = Math.max(n.bound[0], n.bound[2]);
            y1 = Math.min(n.bound[1], n.bound[3]);
            y2 = Math.max(n.bound[1], n.bound[3]);
            entryList.add(Entries.entry(n, Geometries.rectangle(x1,y1,x2,y2)));
        }

        layoutRTree = layoutRTree.add(entryList);
        return layoutRTree;
    }

    //通过二分图匹配算的图的相似度。速度太慢了。。。
    protected double similarityWithByRectArea2(LayoutTree layoutTree){
        List<LayoutNode> nodes1 = findAll(new Predicate<LayoutNode>() {
            @Override
            public boolean test(LayoutNode node) {
                return true;
            }
        }, TreeSearchOrder.BoardFirst);
        List<LayoutNode> nodes2 = layoutTree.findAll(new Predicate<LayoutNode>() {
            @Override
            public boolean test(LayoutNode node) {
                return true;
            }
        }, TreeSearchOrder.BoardFirst);
        int maxSize = Math.max(nodes1.size(), nodes2.size());
        double[][] weight = new double[maxSize][maxSize];
        int[] match = new int[maxSize];
        for(int i=0; i<nodes1.size(); i++){
            for(int j=0; j<nodes2.size(); j++){
                weight[i][j] = Utils.getNormalizedOverlapArea(nodes1.get(i).bound, nodes2.get(j).bound);
            }
        }
        double sim = Utils.biGraph(true, weight, match)*1.0/Math.max(nodes1.size(), nodes2.size());
        return sim;
    }

    //先将每个控件按照面积排序，然后用二分图匹配，权值就是面积比（小/大）
    protected double similarityWithByRegionRatio(LayoutTree layoutTree) {
        SimpleWeightedGraph<LayoutNode, DefaultWeightedEdge> biPartitieGraph = new SimpleWeightedGraph<LayoutNode, DefaultWeightedEdge>(new ClassBasedEdgeFactory<LayoutNode, DefaultWeightedEdge>(DefaultWeightedEdge.class));
        List<LayoutNode> nodes1 = findAll(new Predicate<LayoutNode>() {
            @Override
            public boolean test(LayoutNode node) {
                if(node.getChildrenCount() == 0) {
                    biPartitieGraph.addVertex(node);
                    return true;
                }
                return false;
            }
        }, TreeSearchOrder.BoardFirst);
        List<LayoutNode> nodes2 = layoutTree.findAll(new Predicate<LayoutNode>() {
            @Override
            public boolean test(LayoutNode node) {
                if(node.getChildrenCount() == 0) {
                    biPartitieGraph.addVertex(node);
                    return true;
                }
                return false;
            }
        }, TreeSearchOrder.BoardFirst);
        for (LayoutNode n1 : nodes1) {
            for (LayoutNode n2 : nodes2) {
                double ratio = regionRatio(n1, n2);
                if(ratio >= 0.7)
                    biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge(n1, n2), regionRatio(n1, n2));
            }
        }
        MaximumWeightBipartiteMatching<LayoutNode, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(biPartitieGraph, new HashSet<>(nodes1), new HashSet<>(nodes2));
        MatchingAlgorithm.Matching<LayoutNode, DefaultWeightedEdge> matchResult = matching.getMatching();
        return matchResult.getWeight()/Math.min(nodes1.size(), nodes2.size());
    }

    private double regionRatio(LayoutNode n1, LayoutNode n2){
        int r1 = Math.abs(n1.bound[0]-n1.bound[2])*Math.abs(n1.bound[1]-n1.bound[3]);
        int r2 = Math.abs(n2.bound[0]-n2.bound[2])*Math.abs(n2.bound[1]-n2.bound[3]);
        if(r1>r2)
            return r2*1.0/r1;
        return r1*1.0/r2;
    }

    protected Integer[] getTreeBFSHashes(){
        Integer[] hashes = new Integer[root.getTotalChildrenCount()];
        int i=0;
        Queue<LayoutNode> nodeQueue = new LinkedList<LayoutNode>();
        for(LayoutNode n : root.getChildren()){
            nodeQueue.offer(n);
        }

        while(!nodeQueue.isEmpty()){
            LayoutNode cn = nodeQueue.poll();
            //当出现TreeView时，我们不管内部的结构
            if(!cn.className.contains("TreeView")){
                for(LayoutNode n : cn.getChildren()){
                    nodeQueue.offer(n);
                }
            }
            hashes[i++] = cn.className.hashCode();
        }

        return hashes;
    }

    /**
     * 查找所有满足条件的节点
     * @param predicate 条件
     * @param searchOrder 遍历顺序
     * @return 满足条件的以searhOrder为顺序的节点列表，若未查找到，返回一个空列表
     */
    public List<LayoutNode> findAll(Predicate<LayoutNode> predicate, TreeSearchOrder searchOrder){
        findList.clear();

        if(searchOrder == TreeSearchOrder.DepthFirst){
            for(LayoutNode n : root.getChildren()){
                findAll(n, predicate);
            }
        }
        else if(searchOrder == TreeSearchOrder.BoardFirst){
            Queue<LayoutNode> q = new LinkedList<LayoutNode>();
            for(LayoutNode n : root.getChildren()){
                q.offer(n);
            }
            while(!q.isEmpty()){
                LayoutNode ln = q.poll();
                if(predicate.test(ln)){
                    findList.add(ln);
                }
                for(LayoutNode n : ln.getChildren()){
                    q.offer(n);
                }
            }
        }

        List<LayoutNode> result = new ArrayList<LayoutNode>();
        result.addAll(findList);
        return result;
    }

    //用于DFS模式的findAll
    private void findAll(LayoutNode node, Predicate<LayoutNode> predicate){
        if(node == null)
            return;
        if(predicate.test(node))
            findList.add(node);
        for(LayoutNode n : node.getChildren()){
            findAll(n, predicate);
        }
    }

    private void createTree(String layoutXML) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(layoutXML.getBytes("utf-8")));
        Element rootEle = doc.getDocumentElement();
        if(rootEle == null)
            return;
        NodeList nodes = rootEle.getChildNodes();
        if(nodes == null)
            return;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node != null && node.getNodeType() == Node.ELEMENT_NODE){
                LayoutNode ln = parseActivityNode(node);
                ln.indexXpath = ln.index + "";
                root.addChild(ln);
                recursionCreateTree(node, ln);
            }
        }
    }

    private void recursionCreateTree(Node curNode, LayoutNode parent){
        if(curNode == null)
            return;
        NodeList nodes = curNode.getChildNodes();
        if(nodes == null)
            return;
        for(int i=0; i<nodes.getLength(); i++){
            Node node = nodes.item(i);
            if(node != null && node.getNodeType() == Node.ELEMENT_NODE){
                LayoutNode ln = parseActivityNode(node);
                ln.indexXpath = parent.indexXpath + " " + ln.index;
                parent.addChild(ln);
                recursionCreateTree(node, ln);
            }
        }
    }

    /**
     * 通过XML中的Node节点创建LayoutNode
     * @param node 用于转换的节点
     * @return 根据node创建的LayoutNode
     */
    private LayoutNode parseActivityNode(Node node){
        LayoutNode layoutNode = new LayoutNode();
        NamedNodeMap nnm = node.getAttributes();
        layoutNode.index = Integer.parseInt(nnm.getNamedItem("index").getNodeValue());
        layoutNode.text = nnm.getNamedItem("text").getNodeValue();
        layoutNode.className = nnm.getNamedItem("class").getNodeValue();
        layoutNode.packageName = nnm.getNamedItem("package").getNodeValue();
        layoutNode.contentDesc = nnm.getNamedItem("content-desc").getNodeValue();
        layoutNode.checkable = parseBoolean(nnm.getNamedItem("checkable").getNodeValue());
        layoutNode.checked = parseBoolean(nnm.getNamedItem("checked").getNodeValue());
        layoutNode.clickable = parseBoolean(nnm.getNamedItem("clickable").getNodeValue());
        layoutNode.enabled = parseBoolean(nnm.getNamedItem("enabled").getNodeValue());
        layoutNode.focusable = parseBoolean(nnm.getNamedItem("focusable").getNodeValue());
        layoutNode.focuesd = parseBoolean(nnm.getNamedItem("focused").getNodeValue());
        layoutNode.scrollable = parseBoolean(nnm.getNamedItem("scrollable").getNodeValue());
        layoutNode.longClickable = parseBoolean(nnm.getNamedItem("long-clickable").getNodeValue());
        layoutNode.password = parseBoolean(nnm.getNamedItem("password").getNodeValue());
        layoutNode.selected = parseBoolean(nnm.getNamedItem("selected").getNodeValue());
        String boundStr = nnm.getNamedItem("bounds").getNodeValue();
        Matcher matcher = Pattern.compile("[0-9]+").matcher(boundStr);
        if(matcher.find())
            layoutNode.bound[0] = Integer.parseInt(matcher.group());
        if(matcher.find())
            layoutNode.bound[1] = Integer.parseInt(matcher.group());
        if(matcher.find())
            layoutNode.bound[2] = Integer.parseInt(matcher.group());
        if(matcher.find())
            layoutNode.bound[3]= Integer.parseInt(matcher.group());
        return layoutNode;
    }

    public LayoutNode getNodeByXPath(String indexXPath){
        String[] indexStrs = indexXPath.split(" ");
        int[] indexes = new int[indexStrs.length];
        for(int i=0; i<indexes.length; i++){
            indexes[i] = Integer.parseInt(indexStrs[i]);
        }
        LayoutNode node = root;
        for(int i=0; i<indexes.length; i++){
           if(node.getChildrenCount() <= indexes[i])
               return null;
            node = node.getChildren().get(indexes[i]);
        }
       return node;
    }

    class BoundNode {
        private int x0 = 0;
        private int y0 = 0;
        private int x3 = 0;
        private int y3 = 0;
        BoundNode(String flag,int[] bounds){
            //System.out.println("bounds:"+bounds);
//            String regEx = "[^0-9]";//匹配指定范围内的数字
//            //Pattern是一个正则表达式经编译后的表现模式
//            Pattern p = Pattern.compile(regEx);
//            // 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
//            Matcher m = p.matcher(bounds);
//            //将输入的字符串中非数字部分用空格取代并存入一个字符串
//            String string = m.replaceAll(" ").trim();
//            //以空格为分割符在讲数字存入一个字符串数组中
//            String[] s = string.split(" ");
            //遍历数组转换数据类型输出
            if (flag.equals("0")|| flag.equals("3")){
                int lt_x = bounds[0];
                int lt_y = bounds[1];
                int rb_x = bounds[2];
                int rb_y = bounds[3];
                x0 = (lt_x)/60;
                y0 = (lt_y)/60;
                x3 = (rb_x)/60;
                y3 = (rb_y)/60;

                if(x0>=6){x0 = 5;}
                if(x3>=6){x3 = 5;}
                if(y0>=8){y0 = 7;}
                if(y3>=8){y3 = 7;}
            }
            else {
                int lt_x = bounds[0];
                int lt_y = bounds[1];
                int rb_x = bounds[2];
                int rb_y = bounds[3];

                y0 = (lt_x)/60;
                x0 = (lt_y)/60;
                y3 = (rb_x)/60;
                x3 = (rb_y)/60;

                if(x0>=6){x0 = 5;}
                if(x3>=6){x3 = 5;}
                if(y0>=8){y0 = 7;}
                if(y3>=8){y3 = 7;}
            }
        }
    }
    public static void main(String[] args) {
        String s = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><hierarchy rotation=\"1\"><node index=\"0\" text=\"\" resource-id=\"\" class=\"android.widget.FrameLayout\" package=\"com.coboqo.fm\" content-desc=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false\" scrollable=\"false\" long-clickable=\"false\" password=\"false\" selected=\"false\" bounds=\"[0,0][438,320]\"><node index=\"0\" text=\"\" resource-id=\"\" class=\"android.widget.LinearLayout\" package=\"com.coboqo.fm\" content-desc=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false\" scrollable=\"false\" long-clickable=\"false\" password=\"false\" selected=\"false\" bounds=\"[0,0][438,320]\"><node index=\"0\" text=\"\" resource-id=\"android:id/content\" class=\"android.widget.FrameLayout\" package=\"com.coboqo.fm\" content-desc=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false\" scrollable=\"false\" long-clickable=\"false\" password=\"false\" selected=\"false\" bounds=\"[0,0][438,320]\"><node index=\"0\" text=\"\" resource-id=\"\" class=\"android.widget.FrameLayout\" package=\"com.coboqo.fm\" content-desc=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false\" scrollable=\"false\" long-clickable=\"false\" password=\"false\" selected=\"false\" bounds=\"[0,0][438,320]\"><node index=\"0\" text=\"\" resource-id=\"\" class=\"android.view.View\" package=\"com.coboqo.fm\" content-desc=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"true\" focused=\"true\" scrollable=\"false\" long-clickable=\"false\" password=\"false\" selected=\"false\" bounds=\"[0,0][438,320]\" /><node NAF=\"true\" index=\"1\" text=\"\" resource-id=\"\" class=\"android.widget.EditText\" package=\"com.coboqo.fm\" content-desc=\"\" checkable=\"false\" checked=\"false\" clickable=\"true\" enabled=\"true\" focusable=\"true\" focused=\"false\" scrollable=\"false\" long-clickable=\"true\" password=\"false\" selected=\"false\" bounds=\"[0,0][438,48]\" /></node></node></node></node></hierarchy>";
        LayoutTree l = new LayoutTree(s);

    }

}
