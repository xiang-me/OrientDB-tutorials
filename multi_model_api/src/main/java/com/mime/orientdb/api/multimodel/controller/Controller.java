package com.mime.orientdb.api.multimodel.controller;

import com.mime.orientdb.api.multimodel.service.OrientDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/orientdb", produces="application/json;charset=UTF-8")
public class Controller {

    @Autowired
    OrientDbService service;

    @PostMapping("/createAccount")
    @ResponseBody
    public String createAccountClass() {
        return service.createAccountClass();
    }

    @PostMapping("/createHasFollowed")
    @ResponseBody
    public String createHasFollowClass() {
        return service.createHasFollowedClass();
    }

    /**
     * 入参说明：
     * <pre>
     *    nickName: String | Mandatory
     *    profile: | Mandatory
     *      name: String | Mandatory
     *      address: String | Mandatory
     *      gender: int | Mandatory | 0=female, 1=male
     *      phoneNum: String | Mandatory
     * </pre>
     *
     * 入参样例：
     * <pre>
     *     {
     *         "nickName":"hello_orientdb",
     *         "profile":{
     *             "name":"张三",
     *             "address":"上海",
     *             "gender":1,
     *             "phoneNum":"18888888888"
     *         }
     *     }
     * </pre>
     * @param params
     * @return id 账号ID
     */
    @PostMapping("/newAccount")
    @ResponseBody
    public String newAccount(@RequestBody String params) {
        return service.newAccount(params, false);
    }

    /**
     * 同newAccount
     * @param params
     * @return
     */
    @PostMapping("/newAccountSql")
    @ResponseBody
    public String newAccountSql(@RequestBody String params) {
        return service.newAccount(params, true);
    }

    /**
     * 入参说明：
     * <pre>
     *     id: String | Mandatory
     *     @rid: String | Optional
     *     nickName: String | Mandatory
     * </pre>
     *
     * 入参样例：
     * <pre>
     *     {
     *         "id": "6d5f1625-e171-4ab7-be22-8fd1036e41fd",
     *         "@rid": "#100:0",
     *         "nickName": "hi_orientdb"
     *     }
     * </pre>
     * @param params
     * @return Response
     */
    @PostMapping("/updateNickName")
    @ResponseBody
    public String updateNickName(@RequestBody String params) {
        return service.updateNickName(params, false);
    }

    /**
     * 同updateNickName
     * @param params
     * @return
     */
    @PostMapping("/updateNickNameSql")
    @ResponseBody
    public String updateNickNameSql(@RequestBody String params) {
        return service.updateNickName(params, true);
    }

    /**
     * 入参说明：
     * <pre>
     *     id: String | Mandatory
     *     @rid: String | Optional
     *     hasFollowed: String | Mandatory
     *         id: String | Mandatory
     *         @rid: String | Optional
     * </pre>
     *
     * 入参样例：
     * <pre>
     *     {
     *         "id": "6d5f1625-e171-4ab7-be22-8fd1036e41fd",
     *         "@rid": "#100:1"
     *         "hasFollowed": {
     *             "id": "6d5f1625-e171-4ab7-be22-111111111111",
     *             "@rid": "#200:1"
     *         }
     *     }
     * </pre>
     * @param params
     * @return
     */
    @PostMapping("/follow")
    @ResponseBody
    public String follow(@RequestBody String params) {
        return service.follow(params, false);
    }

    /**
     * 同follow
     * @param params
     * @return
     */
    @PostMapping("/followSql")
    @ResponseBody
    public String followSql(@RequestBody String params) {
        return service.follow(params, true);
    }

    /**
     * 入参说明：
     * <pre>
     *     id: String | Mandatory
     *     @rid: String | Optional
     *     hasFollowed: String | Mandatory
     * </pre>
     *
     * 入参样例：
     * <pre>
     *     {
     *         "id": "6d5f1625-e171-4ab7-be22-8fd1036e41fd",
     *         "@rid": "#100:0"
     *         "hasFollowed": "6d5f1625-e171-4ab7-be22-111111111111"
     *     }
     * </pre>
     * @param params
     * @return
     */
    @PostMapping("/unfollow")
    @ResponseBody
    public String unfollow(@RequestBody String params) {
        return service.unFollowed(params, false);
    }

    /**
     * 同unfollow
     * @param params
     * @return
     */
    @PostMapping("/unfollowSql")
    @ResponseBody
    public String unfollowSql(@RequestBody String params) {
        return service.unFollowed(params, true);
    }


    /**
     * 入参说明：
     *      id: String | Mandatory
     *      @rid: String | Optional
     *
     * 入参样例：
     * <pre>
     *     {
     *         "id": "6d5f1625-e171-4ab7-be22-8fd1036e41fd",
     *         "@rid": "#100:0"
     *     }
     * </pre>
     * @param params
     * @return
     */
    @PostMapping("/listFollowed")
    public String listFollowed(@RequestBody String params) {
        return service.listFollowed(params, false);
    }

    /**
     * 同listFollowed
     * @param params
     * @return
     */
    @PostMapping("/listFollowedSql")
    public String listFollowedSql(@RequestBody String params) {
        return service.listFollowed(params, true);
    }

}
