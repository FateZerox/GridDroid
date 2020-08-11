package edu.nju.autodroid.windowtransaction;

import edu.nju.autodroid.hierarchyHelper.LayoutTree;
import edu.nju.autodroid.utils.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ysht on 2017/1/6.
 */
public class Group implements IWindow {
    private int depth = -1;
    private List<LayoutTree> layoutList = new ArrayList<LayoutTree>();
    private int numLayoutTree = 0;
    private LayoutTree currentLayout = null;//windows总当前的layout
    private String id;
    private String activityName;

    public Group(String id){
        if(id == null){
            Logger.logException("PagedWindow id cannot be null!");
        }
        this.id = id;
        this.activityName = "activity";
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    @Deprecated
    public LayoutTree getLayout() {
        return null;
    }

    @Override
    @Deprecated
    public void setLayout(LayoutTree layout) {

    }

    public void setDepth(int depth){
        this.depth = depth;
    }

    public int getDepth(){
        return this.depth;
    }

    public void addLayout(LayoutTree layout){
        if(layoutList.size()==0) {
            layoutList.add(layout);
            numLayoutTree++;
        }else if (layoutList.size()==1){
            for(int i = 0;i<layoutList.get(0).nodelist.length;i++){
                layoutList.get(0).nodelist[i] = (layoutList.get(0).nodelist[i]*numLayoutTree+layout.nodelist[i])/(numLayoutTree+1);
                numLayoutTree++;
            }
        }
    }

    public List<LayoutTree>  getLayouts(){
        return layoutList;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    public static Group OutWindow = new Group("-1");

}
