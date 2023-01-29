package org.planetaccounting.saleAgent.aksionet;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by SHB on 2/16/2018.
 */

public class ActionCategoryItem extends RealmObject {

        @SerializedName("id")
        @Expose
        public String id;
        @SerializedName("from")
        @Expose
        public String from;
        @SerializedName("to")
        @Expose
        public String to;
        @SerializedName("category_id")
        @Expose
        public String categoryId;
        @SerializedName("category_name")
        @Expose
        public String categoryName;
        @SerializedName("quantity")
        @Expose
        public String quantity;
        @SerializedName("discount")
        @Expose
        public String discount;
        @SerializedName("unit")
        @Expose
        public String unit;
        @SerializedName("client_category")
        @Expose
        public String clientCategory;
        @SerializedName("client_category_name")
        @Expose
        public String clientCategoryName;

    @SerializedName("type")
    @Expose
    public String type;

    @SerializedName("steps")
    @Expose
    public RealmList<ActionSteps> steps;

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getDiscount() {
        return discount;
    }

    public String getUnit() {
        return unit;
    }

    public String getClientCategory() {
        return clientCategory;
    }

    public String getClientCategoryName() {
        return clientCategoryName;
    }

    public String getType() {
        return type;
    }

    public RealmList<ActionSteps> getSteps() {
        return steps;
    }
}