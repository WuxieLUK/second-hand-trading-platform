package org.example.common;

import lombok.Data;

@Data
public class R {
    private Integer code;
    private String message;
    private Object data;

    public static R ok(){
        R r = new R();
        r.setCode(200);
        r.setMessage("成功");
        return r;
    }

    public static R ok(String message,Object data){
        R r = new R();
        r.setCode(200);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static R error(){
        R r = new R();
        r.setCode(500);
        r.setMessage("失败");
        return r;
    }


    public static R error(Integer code,String message){
        R r = new R();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
