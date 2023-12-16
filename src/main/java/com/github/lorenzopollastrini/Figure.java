package com.github.lorenzopollastrini;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Figure {

    @SerializedName("fig_id")
    private String figId;
    private String source;
    private String caption;
    private List<Paragraph> paragraphs;

}
