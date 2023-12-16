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
    @SerializedName("caption_citations")
    private List<String> captionCitations;
    private List<Paragraph> paragraphs;

}
