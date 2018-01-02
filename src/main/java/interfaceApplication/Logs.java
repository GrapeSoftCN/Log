package interfaceApplication;

import java.net.InetAddress;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.java.JGrapeSystem.rMsg;
import common.java.apps.appsProxy;
import common.java.authority.plvDef.UserMode;
import common.java.check.checkHelper;
import common.java.database.dbFilter;
import common.java.interfaceModel.GrapeDBSpecField;
import common.java.interfaceModel.GrapeTreeDBModel;
import common.java.nlogger.nlogger;
import common.java.session.session;
import common.java.string.StringHelper;
import common.java.time.TimeHelper;

public class Logs {
    private GrapeTreeDBModel log;
    private GrapeDBSpecField gDbSpecField;
    private session se;
    private JSONObject usersInfo = null;
    private String currentWeb = null;
    private String userId = null;
    private String userName = null;
    private Integer userType = 0;

    public Logs() {
        log = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Logs"));
        log.descriptionModel(gDbSpecField);
        log.bindApp();
        // log.enableCheck();//开启权限检查

        se = new session();
        usersInfo = se.getDatas();
        if (usersInfo != null && usersInfo.size() >= 0) {
            currentWeb = usersInfo.getString("currentWeb"); // 当前用户所属网站id
            userName = usersInfo.getString("name"); // 当前用户姓名
            userId = usersInfo.getString("id"); // 当前用户用户名
            userType = usersInfo.getInt("userType");// 当前用户身份
        }
    }

    /**
     * 增加一条操作日志
     * 
     * @param uid
     * @param username
     */
    @SuppressWarnings("unchecked")
    public String AddLogs(String uid, String uname, String action, String wbid, String FunctionName) {
        Object info = null;
        String ip = "", address = "";
        try {
            JSONObject object = new JSONObject();
            object.put("userId", uid); // 用户名
            object.put("userName", uname); // 姓名
            ip = InetAddress.getLocalHost().getHostAddress();
            object.put("userIp", ip); // ip
            object.put("userAddress", address); // 根据ip地址定位地理位置
            object.put("action", action); // 操作
            object.put("time", TimeHelper.nowMillis()); // 操作时间
            object.put("wbid", wbid); // 操作网站id
            object.put("functionName", FunctionName); // 调用接口名称
            info = log.data(object).autoComplete().insertEx();
        } catch (Exception e) {
            nlogger.logout(e);
        }
        return info != null ? rMsg.netMSG(0, "添加成功") : rMsg.netMSG(0, "添加失败");
    }

    /**
     * 获取分页数据
     * 
     * @param idx
     *            当前页
     * @param pageSize
     *            每页最大数据量
     * @return  返回网站名称
     * 
     *         备注：系统管理员可以查看当前系统下所有用户操作日志 备注：网站管理员可以查看本人操作日志及普通用户操作日志
     */
    public String Page(int idx, int pageSize) {
        long total = 0;
        if (idx <= 0) {
            return rMsg.netMSG(false, "页码错误");
        }
        if (pageSize <= 0) {
            return rMsg.netMSG(false, "页长度错误");
        }

        if (userType == 0) {
            return rMsg.netMSG(false, "登录信息失效，请重新登录");
        }
        if (userType >= UserMode.admin && userType < UserMode.root) {
            JSONArray cond = buildArray();
            if (cond != null && cond.size() > 0) {
                log.or().where(cond);
            } else {
                return rMsg.netMSG(false, "获取站点信息失败");
            }
        }
        JSONArray array = log.dirty().page(idx, pageSize);
        total = log.count();
        return rMsg.netPAGE(idx, pageSize, total, array);
    }

    /**
     * 根据条件获取分页数据
     * 
     * @param idx
     *            当前页
     * @param pageSize
     *            每页最大数据量
     * @param condString
     *            分页条件
     * @return
     */
    public String PageBy(int idx, int pageSize, String condString) {
        long total = 0;
        if (idx <= 0) {
            return rMsg.netMSG(false, "页码错误");
        }
        if (pageSize <= 0) {
            return rMsg.netMSG(false, "页长度错误");
        }

        if (userType == 0) {
            return rMsg.netMSG(false, "登录信息失效，请重新登录");
        }
        if (userType >= UserMode.admin && userType < UserMode.root) {
            JSONArray cond = buildArray();
            if (cond != null && cond.size() > 0) {
                log.or().where(cond);
            } else {
                return rMsg.netMSG(false, "获取站点信息失败");
            }
        }
        JSONArray condArray = JSONArray.toJSONArray(condString);
        if (condArray != null && condArray.size() > 0) {
            log.and().where(condArray);
        } else {
            return rMsg.netMSG(false, "无效参数");
        }
        JSONArray array = log.dirty().page(idx, pageSize);
        total = log.count();
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 生成查询条件
     * 
     * @return
     */
    private JSONArray buildArray() {
        dbFilter filter = new dbFilter();
        // 获取当前站点的下级站点
        String[] wbids = getAllWeb();
        if (wbids != null && wbids.length > 0) {
            for (String wbid : wbids) {
                if (StringHelper.InvaildString(wbid) && !wbid.equals("0")) {
                    if (ObjectId.isValid(wbid) || checkHelper.isInt(wbid)) {
                        filter.eq("wbid", wbid);
                    }
                }
            }
        }
        return filter.build();
    }

    /**
     * 获取所有下级站点id
     * 
     * @return
     */
    private String[] getAllWeb() {
        String[] wbids = null;
        String wbid = null;
        if (StringHelper.InvaildString(currentWeb) && !currentWeb.equals("0")) {
            if (ObjectId.isValid(currentWeb) || checkHelper.isInt(currentWeb)) {
                wbid = (String) appsProxy.proxyCall("");
            }
            if (StringHelper.InvaildString(wbid) && !currentWeb.equals("0")) {
                wbids = wbid.split(",");
            }
        }
        return wbids;
    }
}
