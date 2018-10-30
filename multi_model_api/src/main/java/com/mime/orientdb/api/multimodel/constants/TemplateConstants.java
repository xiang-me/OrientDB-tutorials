package com.mime.orientdb.api.multimodel.constants;

public class TemplateConstants {

    public static final String KEYWORD_CLASS = "class";
    public static final String KEYWORD_CONTENT = "content";
    public static final String KEYWORD_WHERE = "where";

    public static final String CREATE_VERTEX_USE_CONTENT = "CREATE VERTEX #class# CONTENT #content#;";

    //注意，当使用唯一的索引并通过WHERE条件对索引执行查找时，UPSERT子句才能保证原子性。
    public static final String UPDATE_VERTEX = "UPDATE #class# SET #content# UPSERT WHERE #where#;";

}
