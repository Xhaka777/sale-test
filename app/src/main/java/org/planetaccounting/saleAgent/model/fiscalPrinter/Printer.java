package org.planetaccounting.saleAgent.model.fiscalPrinter;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Printer extends RealmObject {

    @PrimaryKey
    public String id;
    public String name;
    public String code;
    public int selected;//mujna me bo edhe true/false

    //Required
    public Printer(){

    }

    public Printer(String id, String name){
        this.id = id;
        this.name = name;
    }

    public Printer(String id, String name,String code, int selected){
        this.id = id;
        this.name = name;
        this.code = code;
        this.selected = selected;
    }

    public String getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setCode(String code){
        this.code = code;
    }

    public String getCode(){
        return code;
    }

    public void setSelected(int selected){
        this.selected = selected;
    }

    public int getSelected(){
        return selected;
    }
}
