package interfaceApplication;

import java.net.InetAddress;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import apps.appsProxy;
import authority.plvDef.UserMode;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import nlogger.nlogger;
import session.session;
import string.StringHelper;
import time.TimeHelper;

public class Logs {
    private GrapeTreeDBModel log;
    private GrapeDBSpecField gDbSpecField;
    private session se;
    private JSONObject usersInfo = null;
    private String currentWeb = null;
    private String userId = null;
    private String userName = null;
    private Integer userType = null;

    public Logs() {
        log = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Logs"));
        log.descriptionModel(gDbSpecField);
        log.bindApp();
        log.enableCheck();//开启权限检查
        
        
        se = new session();
        usersInfo = se.getDatas();
        if (usersInfo != null && usersInfo.size() != 0) {
            currentWeb = usersInfo.getString("currentWeb"); // 当前用户所属网站id
            userName = usersInfo.getString("name");    //当前用户姓名
            userId = usersInfo.getString("id");  //当前用户用户名
            userType =usersInfo.getInt("userType");//当前用户身份
        }
    }

    /**
     * 增加一条操作日志
     * 
     * @param uid
     * @param username
     */
    @SuppressWarnings("unchecked")
    public void AddLogs(String uid,String uname,String action,String wbid,String FunctionName) {
        String ip = "", address = "";
        try {
            JSONObject object = new JSONObject();
            object.put("userId", uid);  //用户名
            object.put("userName", uname); //姓名
            ip = InetAddress.getLocalHost().getHostAddress();
            object.put("userIp", ip);  //ip
            object.put("userAddress", address); // 根据ip地址定位地理位置
            object.put("action", action); //操作
            object.put("time", TimeHelper.nowMillis()); //操作时间
            object.put("wbid", wbid);  //操作网站id
            object.put("functionName", FunctionName);  // 调用接口名称
            log.data(object).autoComplete().insertEx();
        } catch (Exception e) {
            nlogger.logout(e);
        }
    }

    
    /**
     * 获取分页数据
     * @param idx  当前页
     * @param pageSize  每页最大数据量
     * @return
     * 
     * 备注：系统管理员可以查看当前系统下所有用户操作日志
     * 备注：网站管理员可以查看本人操作日志及普通用户操作日志
     */
    public String Page(int idx, int pageSize) {
        return PageBy(idx, pageSize, null);
    }

    /**
     * 根据条件获取分页数据
     * @param idx  当前页
     * @param pageSize   每页最大数据量
     * @param condString  分页条件
     * @return
     */
    public String PageBy(int idx, int pageSize, String condString) {
        long total = 0;
        if (idx > 0 && pageSize > 0) {
            if (StringHelper.InvaildString(condString)) {
                JSONArray condArray = JSONArray.toJSONArray(condString);
                if (condArray !=null && condArray.size() > 0) {
                    log.where(condArray);
                }else{
                    return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
                }
            }
        }
        //系统管理员，可以查看当前系统下所有用户的操作日志
        //网站管理员，可以查看当前网站下个人的操作日志
        log.eq("wbid", currentWeb).eq("userId", userId).eq("username", userName);
        JSONArray array = log.dirty().page(idx, pageSize);
        total = log.count();
        return rMsg.netPAGE(idx, pageSize, total, (array!=null && array.size() > 0)?array:new JSONArray());
    }
}
