package com.mime.orientdb.api.multimodel.constants;

public class SchemaContants {

    /*class*/
    public static final String CLASS_V = "V";
    public static final String CLASS_E = "E";
    public static final String CLASS_ACCOUNT = "Account";
    public static final String CLASS_PROFILE = "Profile";
    public static final String CLASS_HASFOLLOWED = "HasFollowed";


    /*通用属性*/
    public static final String COMMON_RID = "@rid";
    public static final String COMMON_ID = "id";
    public static final String COMMON_CREATEDTIME = "created_time";

    /*各个class的属性名称*/
    public static final String ACCOUNT_NICKNAME = "nickname";
    public static final String ACCOUNT_PROFILE = "profile";

    public static final String PROFILE_NAME = "name";
    public static final String PROFILE_ADDRESS = "address";
    public static final String PROFILE_PHONENUM = "phoneNum";
    public static final String PROFILE_GENDER = "gender";

    /*index名称*/
    public static final String IDX_ACCOUNT_ID = "idx_id";
    public static final String IDX_PROFILE_PHONENUM = "idx_phonenum";
    public static final String IDX_ACCOUNT_NICKNAME = "idx_nickname";

}
