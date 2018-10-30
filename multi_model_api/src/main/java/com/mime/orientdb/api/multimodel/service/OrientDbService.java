package com.mime.orientdb.api.multimodel.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import static com.mime.orientdb.api.multimodel.constants.GlobalConstants.*;
import static com.mime.orientdb.api.multimodel.constants.SchemaContants.*;
import static com.mime.orientdb.api.multimodel.constants.TemplateConstants.*;

import com.mime.orientdb.api.multimodel.constants.GlobalConstants;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class OrientDbService {

    private static final Logger logger = LoggerFactory.getLogger(OrientDbService.class);

    @Autowired
    ODatabasePool pool;

    public String createAccountClass() {
        try {

            try (ODatabaseSession session = pool.acquire()) {
                OSchema oSchema = session.getMetadata().getSchema();

                if (!oSchema.existsClass(CLASS_PROFILE) && !oSchema.existsClass(CLASS_ACCOUNT)) {
                    OClass oProfile = session.createClass(CLASS_PROFILE, CLASS_V);
                    oProfile.createProperty(PROFILE_NAME, OType.STRING);
                    oProfile.createProperty(PROFILE_ADDRESS, OType.STRING);
                    oProfile.createProperty(PROFILE_GENDER, OType.SHORT);
                    oProfile.createProperty(PROFILE_PHONENUM, OType.STRING);
                    //为phoneNum创建唯一索引
                    oProfile.createIndex(IDX_PROFILE_PHONENUM, OClass.INDEX_TYPE.UNIQUE, PROFILE_PHONENUM);
                    oProfile.setStrictMode(true);

                    OClass oAccount = session.createClass(CLASS_ACCOUNT, CLASS_V);
                    oAccount.createProperty(COMMON_ID, OType.STRING);
                    oAccount.createProperty(ACCOUNT_NICKNAME, OType.STRING);
                    oAccount.createProperty(COMMON_CREATEDTIME, OType.DATETIME);
                    oAccount.createProperty(ACCOUNT_PROFILE, OType.LINK, oProfile);
                    //分别为id和nickName创建唯一索引
                    oAccount.createIndex(IDX_ACCOUNT_ID, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, COMMON_ID);
                    oAccount.createIndex(IDX_ACCOUNT_NICKNAME, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, ACCOUNT_NICKNAME);

                    session.commit();
                }
            }

            return GlobalConstants.RESP_SUCCESS;
        } catch (Exception e) {
            logger.error("createAccountClass error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    public String createHasFollowedClass() {
        try {
            try (ODatabaseSession session = pool.acquire()) {
                OSchema oSchema = session.getMetadata().getSchema();
                if (!oSchema.existsClass(CLASS_HASFOLLOWED)) {
                    OClass oHasFollowed = session.createClass(CLASS_HASFOLLOWED, CLASS_E);
                    oHasFollowed.createProperty(COMMON_CREATEDTIME, OType.DATETIME);
                    session.commit();
                }
                return GlobalConstants.RESP_SUCCESS;
            }
        } catch (Exception e) {
            logger.error("createHasFollowClass error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    public String newAccount(String params, boolean executeBySql) {
        if (executeBySql) {
            return this.newVertexSql(CLASS_ACCOUNT, parseJson(params)
                    .fluentPut(COMMON_ID, UUID.randomUUID().toString())
                    .fluentPut(COMMON_CREATEDTIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))));
        } else {
            return this.newVertex(CLASS_ACCOUNT, parseJson(params)
                    .fluentPut(COMMON_ID, UUID.randomUUID().toString())
                    .fluentPut(COMMON_CREATEDTIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))));
        }
    }

    public String newVertex(String clazz, JSONObject params) {
        try {

            OVertex vertex;
            try (ODatabaseSession session = pool.acquire()) {
                session.begin();
                vertex = newVertexRecursively(session, clazz, params);
                session.commit();
            }
            return vertex.toJSON();

        } catch (Exception e) {
            logger.error("newVertex error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    /*
    * 通过递归调用，处理像Account中关联Profile的情况
    */
    private OVertex newVertexRecursively(ODatabaseSession session, String clazz, JSONObject params) {
        OVertex vertex = session.newVertex(clazz);
        for (Map.Entry<String, Object> e: params.entrySet()) {
            if (e.getValue() instanceof JSONObject) {
                OVertex inner = newVertexRecursively(session, e.getKey(), (JSONObject) e.getValue());
                vertex.setProperty(e.getKey(), inner);
            } else {
                vertex.setProperty(e.getKey(), e.getValue());
            } // todo 只处理了1:1的关系，1:n或n:n的情况请自行完善
        }
        session.save(vertex);
        return vertex;
    }

    public String newVertexSql(String clazz, JSONObject params) {
        try {

            OVertex account;
            StringBuilder sql = new StringBuilder("BEGIN;\n");
            try (ODatabaseSession session = pool.acquire()) {

                newVertexSqlRecursively(sql, clazz, params);
                sql.append("COMMIT;\n").append("return $").append(clazz);

                try (OResultSet rs = session.execute("sql", sql.toString())) {
                    account = rs.vertexStream().findFirst().get();
                    return account.toJSON();
                }
            }

        } catch (Exception e) {
            logger.error("newVertexSql error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    /**
     * 通过递归生成batch脚本，处理像Account中关联Profile的情况
     *
     * 语句样例：
     * <pre>
     * BEGIN;
     *   LET profile = CREATE VERTEX profile CONTENT {"address":"上海","gender":1,"name":"张三","phoneNum":"***"};
     *   LET Account = CREATE VERTEX Account CONTENT {"created_time":"***","nickName":"***","id":"**","profile":$profile.@rid};
     * COMMIT;
     * return $Account
     * </pre>
     *
     */
    private void newVertexSqlRecursively(StringBuilder sql, String clazz, JSONObject params) {
        Map<String, Object> temp = new HashMap<>();
        StringBuilder inner = new StringBuilder();
        for (Map.Entry<String, Object> e: params.entrySet()) {
            if (e.getValue() instanceof JSONObject) {
                newVertexSqlRecursively(sql, e.getKey(), (JSONObject) e.getValue());
                inner.append("\"").append(e.getKey()).append("\":")
                        .append("$").append(e.getKey()).append(".@rid").append("}");
            } else {
                temp.put(e.getKey(), e.getValue());
            }
            // todo 目前只处理了1:1的关系，1:n或n:n的情况请自行完善
        }

        Map<String, String> args = new HashMap<>();
        args.put(KEYWORD_CLASS, clazz);
        String content = JSON.toJSONString(temp);
        args.put(KEYWORD_CONTENT, inner.length() == 0 ? content :
                content.substring(0, content.length() - 1) + "," + inner.toString());
        sql.append("LET ").append(clazz).append(" = ").append(parseSql(CREATE_VERTEX_USE_CONTENT, args)).append("\n");
    }

    public String updateNickName(String params, Boolean executeBySql) {
        try {
            JSONObject jo = parseJson(params);

            if (executeBySql) {

                Map<String, Object> properties = new HashMap<>();
                properties.put(COMMON_ID, jo.getString(COMMON_ID));
                properties.put(ACCOUNT_NICKNAME, jo.getString(ACCOUNT_NICKNAME));
                String sql = "UPDATE Account SET nickname = :nickname WHERE id = :id;";
                try (ODatabaseSession session = pool.acquire()) {
                    try (OResultSet result = session.command(sql, properties)) {
                        return result.stream().findFirst().map(OResult::toJSON).orElse(null);
                    }
                }

            } else {

                Map<String, Object> properties = new HashMap<>();
                properties.put(ACCOUNT_NICKNAME, jo.getString(ACCOUNT_NICKNAME));
                OVertex vertex;
                try (ODatabaseSession session = pool.acquire()) {
                    session.begin();
                    vertex = session.load(new ORecordId(jo.getString(COMMON_RID)));
                    for (Map.Entry<String, Object> e: properties.entrySet()) {
                        vertex.setProperty(e.getKey(), e.getValue());
                    }
                    vertex.save();
                    session.commit();
                }

                return GlobalConstants.RESP_SUCCESS;
            }
        } catch (Exception e) {
            logger.error("updateVertex error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    public String follow(String params, Boolean executeBySql) {
        JSONObject jo = parseJson(params);
        String fromRid = jo.getString(COMMON_RID);
        String fromId = jo.getString(COMMON_ID);
        String toRid = jo.getJSONObject(CLASS_HASFOLLOWED).getString(COMMON_RID);
        String toId = jo.getJSONObject(CLASS_HASFOLLOWED).getString(COMMON_ID);
        if (executeBySql) {

            Map<String, Object> properties = new HashMap<>();
            properties.put("fromId", fromId);
            properties.put("toId", toId);
            String sql = "CREATE EDGE HasFollowed FROM " +
                    "(SELECT FROM Account WHERE id = :fromId) TO " +
                    "(SELECT FROM Account WHERE id = :toId);";
            try (ODatabaseSession session = pool.acquire()) {
                try (OResultSet result = session.command(sql, properties)) {
                    return result.stream().findFirst().map(OResult::toJSON).orElse(null);
                }
            }

        } else {

            try (ODatabaseSession session = pool.acquire()) {
                session.begin();
                OEdge edge = session.newEdge(session.load(new ORecordId(fromRid)),
                        session.load(new ORecordId(toRid)),
                        CLASS_HASFOLLOWED);
                session.save(edge);
                session.commit();
                return edge.toJSON();
            }
        }
    }

    public String unFollowed(String params, Boolean executeBySql) {
        try {
            JSONObject jo = parseJson(params);
            String rid = jo.getString(COMMON_RID);
            String fromId = jo.getString(COMMON_ID);
            String toId = jo.getJSONObject(CLASS_HASFOLLOWED).getString(COMMON_ID);

            if (executeBySql) {

                try (ODatabaseSession session = pool.acquire()) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("fromId", fromId);
                    properties.put("toId", toId);
                    String sql = "DELETE EDGE HasFollowed FROM " +
                            "(SELECT FROM Account WHERE id = :fromId) TO " +
                            "(SELECT FROM Account WHERE id = :toId);";
                    try (OResultSet resultSet = session.command(sql, properties)) {
                        return resultSet.stream().findFirst().map(OResult::toJSON).orElse(null);
                    }
                }

            } else {

                try (ODatabaseSession session = pool.acquire()) {
                    session.delete(new ORecordId(rid));
                }

            }
            return GlobalConstants.RESP_SUCCESS;
        } catch (Exception e) {
            logger.error("unFollowed error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    public String listFollowed(String params, Boolean executeBySql) {
        try {
            JSONObject jo = parseJson(params);
            if (executeBySql) {

                try (ODatabaseSession session = pool.acquire()) {
                    String sql = "match \n" +
                            "  {class:Account, as:self, where:(id = :id)}\n" +
                            "  .outE('HasFollowed'){as:hasFollowed}\n" +
                            "  .inV(){as:follow}\n" +
                            "return \n" +
                            "  follow.@rid as rid, follow.nickname as nickname, " +
                            "  follow.id as id, follow.profile.name as name, " +
                            "  follow.profile.phoneNum as phoneNum, " +
                            "  follow.profile.gender as gender, " +
                            "  follow.profile.address as address, " +
                            "  hasFollowed.@rid as hasFollowRid;";
                    Map<String, Object> properties = new HashMap<>();
                    properties.put(COMMON_ID, jo.getString(COMMON_ID));
                    try (OResultSet resultSet = session.query(sql, properties)) {
                        return JSON.toJSONString(resultSet.stream()
                                .map(OResult::toJSON)
                                .collect(Collectors.toList()));
                    }
                }

            } else {

                try (ODatabaseSession session = pool.acquire()) {
                    OVertex resultSet = session.load(new ORecordId(jo.getString(COMMON_RID)));
                    return resultSet.toJSON("fetchPlan:profile:-1 out_HasFollowed.in:1");
                }

            }
        } catch (Exception e) {
            logger.error("unFollowed error", e);
            return GlobalConstants.RESP_ERROR;
        }
    }

    private String parseSql(String template, Map<String, String> args) {
        for (Map.Entry<String, String> e: args.entrySet()) {
            template = template.replace("#" + e.getKey() + "#", e.getValue());
        }
        return template;
    }

    private JSONObject parseJson(String s) {
        return JSON.parseObject(s);
    }

}
