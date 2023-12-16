package com.github.lorenzopollastrini;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Table {

    @SerializedName("table_id")
    private String tableId;
    private String body;
    private String caption;
    @SerializedName("caption_citations")
    private List<String> captionCitations;
    private List<String> foots;
    private List<Paragraph> paragraphs;
    private List<Cell> cells;

}
