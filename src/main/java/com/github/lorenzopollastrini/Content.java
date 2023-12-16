package com.github.lorenzopollastrini;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Content {

    private String title;
    @SerializedName("abstract")
    private String abstractString;
    private List<String> keywords;
    private List<Table> tables;
    private List<Figure> figures;

}
