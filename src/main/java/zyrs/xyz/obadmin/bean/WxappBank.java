package zyrs.xyz.obadmin.bean;

/**
 * Created by Administrator on 2019/3/5.
 * 小程序 银行 绑定
 */
public class WxappBank {
   private Integer id;
   private Integer oid;
    private String card;
    private String account;
    private String openid;

    @Override
    public String toString() {
        return "WxappBank{" +
                "id=" + id +
                ", oid=" + oid +
                ", card='" + card + '\'' +
                ", account='" + account + '\'' +
                ", openid='" + openid + '\'' +
                '}';
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {

        this.id = id;
    }

    public Integer getOid() {
        return oid;
    }

    public void setOid(Integer oid) {
        this.oid = oid;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
