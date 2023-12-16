package com.github.lorenzopollastrini;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Cell {

    private String content;
    @SerializedName("cited_in")
    private List<String> citedIn;

}
