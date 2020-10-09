package com.sohu.smc.core;

/**
 * 只要ValueWrapper不为null则表示缓存命中
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
class ValueWrapper {

    private Object o;

    public ValueWrapper(Object o) {
        this.o = o;
    }

    /**
     * 获取缓存的值
     * @return 获取缓存的值,支持null
     */
    public Object get(){
        if (NullValue.NULL.equals(o)){
            return null;
        }
        return o;
    }

}
